package com.kiwi.manager.ui.screen.home

import com.kiwi.manager.domain.model.AppDisplayInfo

data class HomeUiState(
    val apps: List<AppDisplayInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
    val isWatchConnected: Boolean = false
)
