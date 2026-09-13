package com.kiwi.manager.ui.screen.watchapps

import com.kiwi.manager.domain.model.WatchAppInfo
import com.kiwi.manager.domain.model.WatchDeviceInfo

enum class WatchAppFilter(val label: String) {
    USER("Người dùng"),
    SYSTEM("Hệ thống"),
    RUNNING("Đang chạy"),
    ALL("Tất cả")
}

data class WatchAppUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val currentActionPackage: String? = null,
    val actionFeedbackMessage: String? = null,
    val errorMessage: String? = null,
    val deviceInfo: WatchDeviceInfo? = null,
    val apps: List<WatchAppInfo> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: WatchAppFilter = WatchAppFilter.USER,
    val selectedAppForDetails: WatchAppInfo? = null,
    val selectedAppDumpsys: String? = null,
    val isLoadingDetails: Boolean = false,
    val appToUninstall: WatchAppInfo? = null
) {
    val filteredApps: List<WatchAppInfo>
        get() {
            val query = searchQuery.trim().lowercase()
            return apps.filter { app ->
                val matchesFilter = when (activeFilter) {
                    WatchAppFilter.USER -> !app.isSystemApp
                    WatchAppFilter.SYSTEM -> app.isSystemApp
                    WatchAppFilter.RUNNING -> app.isRunning
                    WatchAppFilter.ALL -> true
                }
                val matchesSearch = query.isEmpty() ||
                        app.appName.lowercase().contains(query) ||
                        app.packageName.lowercase().contains(query)
                matchesFilter && matchesSearch
            }
        }

    val userAppCount: Int
        get() = apps.count { !it.isSystemApp }

    val systemAppCount: Int
        get() = apps.count { it.isSystemApp }

    val runningAppCount: Int
        get() = apps.count { it.isRunning }
}
