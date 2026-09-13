package com.kiwi.manager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AppInfo(
    val id: String,
    val name: String,
    val description: String,
    val icon: String = "",
    val githubRepo: String = "",
    val source: String = "static",  // "static" or "github_releases"
    val isManager: Boolean = false,
    val phone: PlatformInfo? = null,
    val watch: PlatformInfo? = null
)

@Serializable
data class PlatformInfo(
    val packageName: String,
    val assetPattern: String = "",
    val versionName: String = "",
    val versionCode: Int = 0,
    val downloadUrl: String? = null,
    val changelog: String = "",
    val minSdk: Int = 26
)
