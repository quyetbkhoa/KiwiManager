package com.kiwi.manager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InstalledVersionInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isInstalled: Boolean
)
