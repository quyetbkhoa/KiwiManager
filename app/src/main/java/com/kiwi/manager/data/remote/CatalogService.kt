package com.kiwi.manager.data.remote

import com.kiwi.manager.domain.model.CatalogManifest
import com.kiwi.manager.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class CatalogService {
    companion object {
        const val CATALOG_URL = Constants.CATALOG_URL
    }

    private val okHttpClient = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchCatalog(): Result<CatalogManifest> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(CATALOG_URL)
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val manifest = json.decodeFromString<CatalogManifest>(bodyString)
                Result.success(manifest)
            } else {
                Result.failure(Exception("Failed to fetch catalog: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
