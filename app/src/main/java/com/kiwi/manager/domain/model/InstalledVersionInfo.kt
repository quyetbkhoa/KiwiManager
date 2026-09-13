package com.kiwi.manager.domain.model

data class InstalledVersionInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isInstalled: Boolean
)
