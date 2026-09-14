package com.kiwi.manager.domain.model

data class InstallTask(
    val appId: String,
    val isWatch: Boolean,
    val appName: String,
    val packageName: String,
    val stage: InstallStage = InstallStage.IDLE,
    val isInstalling: Boolean = false,
    val downloadProgress: DownloadProgress? = null,
    val pushProgress: DownloadProgress? = null,
    val statusMessage: String = "",
    val logs: List<String> = emptyList(),
    val error: String? = null,
    val isSuccess: Boolean = false
)
