package com.kiwi.manager.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CatalogManifest(
    val schemaVersion: Int = 1,
    val repositoryName: String = "",
    val updatedAt: String = "",
    val apps: List<AppInfo> = emptyList()
)
