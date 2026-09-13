package com.kiwi.manager.ui.screen.appdetail

import com.kiwi.manager.domain.model.AppDisplayInfo
import com.kiwi.manager.domain.model.DownloadProgress

data class AppDetailUiState(
    val app: AppDisplayInfo? = null,
    val isLoading: Boolean = true,
    val phoneDownloadProgress: DownloadProgress? = null,
    val watchDownloadProgress: DownloadProgress? = null,
    val phoneInstalling: Boolean = false,
    val watchInstalling: Boolean = false,
    val installLog: List<String> = emptyList(),
    val error: String? = null
)
