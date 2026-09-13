package com.kiwi.manager.domain.model

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long = 0
) {
    val percent: Int get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
    val downloadedMb: String get() = "%.1f".format(bytesDownloaded / 1_048_576.0)
    val totalMb: String get() = "%.1f".format(totalBytes / 1_048_576.0)
}
