package com.kiwi.manager.domain.model

data class AppDisplayInfo(
    val app: AppInfo,
    val phoneInstalled: InstalledVersionInfo?,
    val watchInstalled: InstalledVersionInfo?,
    val phoneStatus: InstallStatus = InstallStatus.UNKNOWN,
    val watchStatus: InstallStatus = InstallStatus.UNKNOWN
)

enum class InstallStatus {
    NOT_INSTALLED,
    UPDATE_AVAILABLE,
    UP_TO_DATE,
    UNKNOWN
}
