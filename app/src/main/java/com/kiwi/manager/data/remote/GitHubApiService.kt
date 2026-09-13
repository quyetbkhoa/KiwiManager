package com.kiwi.manager.data.remote

import com.kiwi.manager.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String = "",
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0
)

class GitHubApiService {
    private val okHttpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getLatestRelease(repoFullName: String): Result<GitHubRelease> = withContext(Dispatchers.IO) {
        try {
            val url = "${Constants.GITHUB_API_BASE}/$repoFullName/releases/latest?nocache=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(url)
                .build()
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val release = json.decodeFromString<GitHubRelease>(bodyString)
                Result.success(release)
            } else {
                Result.failure(Exception("Failed to fetch release for $repoFullName: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
