package com.kiwi.manager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WatchAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val isSystemApp: Boolean = false,
    val isRunning: Boolean = false,
    val isEnabled: Boolean = true,
    val isUninstalledUser0: Boolean = false,
    val apkPath: String? = null
)
