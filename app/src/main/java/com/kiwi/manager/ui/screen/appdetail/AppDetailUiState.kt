package com.kiwi.manager.ui.screen.appdetail

import com.kiwi.manager.domain.model.AppDisplayInfo
import com.kiwi.manager.domain.model.DownloadProgress
import com.kiwi.manager.domain.model.InstallStage

data class AppDetailUiState(
    val app: AppDisplayInfo? = null,
    val isLoading: Boolean = true,
    val phoneDownloadProgress: DownloadProgress? = null,
    val phonePushProgress: DownloadProgress? = null,
    val phoneStage: InstallStage = InstallStage.IDLE,
    val phoneStatusMessage: String = "",
    val phoneInstalling: Boolean = false,
    val watchDownloadProgress: DownloadProgress? = null,
    val watchPushProgress: DownloadProgress? = null,
    val watchStage: InstallStage = InstallStage.IDLE,
    val watchStatusMessage: String = "",
    val watchInstalling: Boolean = false,
    val installLog: List<String> = emptyList(),
    val error: String? = null
)
