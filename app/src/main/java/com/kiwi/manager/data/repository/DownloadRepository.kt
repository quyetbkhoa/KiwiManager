package com.kiwi.manager.data.repository

import android.content.Context
import com.kiwi.manager.domain.model.DownloadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadRepository(private val context: Context) {
    private val okHttpClient = OkHttpClient()

    suspend fun downloadApk(
        url: String,
        fileName: String,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            var currentUrl = url
            var redirectCount = 0
            var response = okHttpClient.newCall(Request.Builder().url(currentUrl).build()).execute()

            while (response.isRedirect && redirectCount < 8) {
                currentUrl = response.header("Location") ?: break
                response.close()
                response = okHttpClient.newCall(Request.Builder().url(currentUrl).build()).execute()
                redirectCount++
            }

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty body"))
            val totalBytes = body.contentLength()
            val file = File(context.cacheDir, fileName)

            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesDownloaded = 0L
                    var bytesRead: Int
                    var lastProgressTime = System.currentTimeMillis()
                    var lastBytesDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastProgressTime >= 500) {
                            val timeDiff = now - lastProgressTime
                            val bytesDiff = bytesDownloaded - lastBytesDownloaded
                            val speed = (bytesDiff * 1000) / timeDiff

                            onProgress(DownloadProgress(bytesDownloaded, totalBytes, speed))

                            lastProgressTime = now
                            lastBytesDownloaded = bytesDownloaded
                        }
                    }
                    onProgress(DownloadProgress(bytesDownloaded, totalBytes, 0))
                }
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
