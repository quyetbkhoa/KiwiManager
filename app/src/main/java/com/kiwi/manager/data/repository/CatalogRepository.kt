package com.kiwi.manager.data.repository

import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.data.local.PackageManagerWrapper
import com.kiwi.manager.data.remote.CatalogService
import com.kiwi.manager.data.remote.GitHubApiService
import com.kiwi.manager.domain.model.AppDisplayInfo
import com.kiwi.manager.domain.model.AppInfo
import com.kiwi.manager.domain.model.CatalogManifest
import com.kiwi.manager.domain.model.InstallStatus
import com.kiwi.manager.domain.model.InstalledVersionInfo
import com.kiwi.manager.util.VersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CatalogRepository(
    private val catalogService: CatalogService,
    private val gitHubApiService: GitHubApiService,
    private val dataStoreManager: DataStoreManager,
    private val packageManagerWrapper: PackageManagerWrapper
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun refreshCatalog(): Result<List<AppDisplayInfo>> = withContext(Dispatchers.IO) {
        val result = catalogService.fetchCatalog()
        if (result.isSuccess) {
            val manifest = result.getOrThrow()
            
            val processedApps = manifest.apps.map { app ->
                if (app.source == "github_releases") {
                    resolveGitHubVersions(app)
                } else {
                    app
                }
            }
            
            val updatedManifest = manifest.copy(apps = processedApps)
            dataStoreManager.setCachedCatalog(json.encodeToString(updatedManifest))
            
            val displayInfos = processedApps.map { resolveAppDisplayInfo(it) }
            Result.success(displayInfos)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error fetching catalog"))
        }
    }

    suspend fun getCachedApps(): List<AppDisplayInfo> = withContext(Dispatchers.IO) {
        val cachedJson = dataStoreManager.getCachedCatalog()
        if (cachedJson.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val manifest = json.decodeFromString<CatalogManifest>(cachedJson)
                manifest.apps.map { resolveAppDisplayInfo(it) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun resolveAppDisplayInfo(app: AppInfo): AppDisplayInfo {
        val phoneInstalled = app.phone?.let { packageManagerWrapper.getInstalledVersion(it.packageName) }
        val watchInstalled = null // Check over ADB is handled externally

        return AppDisplayInfo(
            app = app,
            phoneInstalled = phoneInstalled,
            watchInstalled = watchInstalled,
            phoneStatus = checkPhoneStatus(app, phoneInstalled),
            watchStatus = InstallStatus.UNKNOWN
        )
    }

    private suspend fun resolveGitHubVersions(app: AppInfo): AppInfo {
        if (app.githubRepo.isEmpty()) return app

        val releaseResult = gitHubApiService.getLatestRelease(app.githubRepo)
        if (releaseResult.isFailure) return app

        val release = releaseResult.getOrNull() ?: return app
        val cleanVersion = VersionComparator.cleanVersion(release.tagName)
        val computedCode = VersionComparator.computeVersionCode(cleanVersion)

        val updatedPhone = app.phone?.let { phoneInfo ->
            val asset = release.assets.find { it.name.contains(phoneInfo.assetPattern) }
            if (asset != null) {
                phoneInfo.copy(
                    versionName = cleanVersion,
                    versionCode = computedCode,
                    downloadUrl = asset.downloadUrl,
                    changelog = release.body
                )
            } else phoneInfo
        }

        val updatedWatch = app.watch?.let { watchInfo ->
            val asset = release.assets.find { it.name.contains(watchInfo.assetPattern) }
            if (asset != null) {
                watchInfo.copy(
                    versionName = cleanVersion,
                    versionCode = computedCode,
                    downloadUrl = asset.downloadUrl,
                    changelog = release.body
                )
            } else watchInfo
        }

        return app.copy(phone = updatedPhone, watch = updatedWatch)
    }

    private fun checkPhoneStatus(app: AppInfo, installed: InstalledVersionInfo?): InstallStatus {
        if (installed == null || !installed.isInstalled) return InstallStatus.NOT_INSTALLED
        val catalogVersionCode = app.phone?.versionCode ?: 0
        return if (catalogVersionCode > installed.versionCode) {
            InstallStatus.UPDATE_AVAILABLE
        } else {
            InstallStatus.UP_TO_DATE
        }
    }
}
