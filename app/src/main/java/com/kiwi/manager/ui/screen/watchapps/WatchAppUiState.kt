package com.kiwi.manager.ui.screen.watchapps

import com.kiwi.manager.domain.model.WatchAppInfo
import com.kiwi.manager.domain.model.WatchDeviceInfo

enum class WatchAppFilter(val label: String) {
    USER("Người dùng"),
    SYSTEM("Hệ thống"),
    RUNNING("Đang chạy"),
    DISABLED("Vô hiệu hóa"),
    UNINSTALLED("Đã gỡ (U0)"),
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
    val activeFilter: WatchAppFilter = WatchAppFilter.ALL,
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
                    WatchAppFilter.USER -> !app.isSystemApp && !app.isUninstalledUser0
                    WatchAppFilter.SYSTEM -> app.isSystemApp && !app.isUninstalledUser0
                    WatchAppFilter.RUNNING -> app.isRunning && !app.isUninstalledUser0
                    WatchAppFilter.DISABLED -> !app.isEnabled && !app.isUninstalledUser0
                    WatchAppFilter.UNINSTALLED -> app.isUninstalledUser0
                    WatchAppFilter.ALL -> true
                }
                val matchesSearch = query.isEmpty() ||
                        app.appName.lowercase().contains(query) ||
                        app.packageName.lowercase().contains(query)
                matchesFilter && matchesSearch
            }
        }

    val userAppCount: Int
        get() = apps.count { !it.isSystemApp && !it.isUninstalledUser0 }

    val systemAppCount: Int
        get() = apps.count { it.isSystemApp && !it.isUninstalledUser0 }

    val runningAppCount: Int
        get() = apps.count { it.isRunning && !it.isUninstalledUser0 }

    val disabledAppCount: Int
        get() = apps.count { !it.isEnabled && !it.isUninstalledUser0 }

    val uninstalledAppCount: Int
        get() = apps.count { it.isUninstalledUser0 }
}
