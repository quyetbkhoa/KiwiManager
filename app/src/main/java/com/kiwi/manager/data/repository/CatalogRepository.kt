package com.kiwi.manager.data.repository

import android.content.Context
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
    private val context: Context,
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
                    try {
                        resolveGitHubVersions(app)
                    } catch (_: Exception) {
                        app
                    }
                } else {
                    app
                }
            }
            
            val updatedManifest = manifest.copy(apps = processedApps)
            dataStoreManager.setCachedCatalog(json.encodeToString(updatedManifest))
            
            val displayInfos = processedApps.map { resolveAppDisplayInfo(it) }
            Result.success(displayInfos)
        } else {
            // If remote fails, try using cached or fallback to bundled assets/catalog.json
            val cached = getCachedApps()
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error fetching catalog"))
            }
        }
    }

    suspend fun getCachedApps(): List<AppDisplayInfo> = withContext(Dispatchers.IO) {
        var cachedJson = dataStoreManager.getCachedCatalog()
        if (cachedJson.isNullOrEmpty()) {
            // Fallback to bundled assets/catalog.json
            cachedJson = try {
                context.assets.open("catalog.json").bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                null
            }
        }

        if (cachedJson.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val manifest = json.decodeFromString<CatalogManifest>(cachedJson)
                // Invalidate old cache if schemaVersion < 2 or apps list is outdated
                if (manifest.schemaVersion < 2) {
                    val bundled = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
                    dataStoreManager.setCachedCatalog(bundled)
                    val newManifest = json.decodeFromString<CatalogManifest>(bundled)
                    newManifest.apps.map { resolveAppDisplayInfo(it) }
                } else {
                    manifest.apps.map { resolveAppDisplayInfo(it) }
                }
            } catch (e: Exception) {
                // If decoding fails, fallback to bundled assets
                try {
                    val bundled = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
                    dataStoreManager.setCachedCatalog(bundled)
                    val newManifest = json.decodeFromString<CatalogManifest>(bundled)
                    newManifest.apps.map { resolveAppDisplayInfo(it) }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
    }

    suspend fun getApp(appId: String): AppDisplayInfo? = withContext(Dispatchers.IO) {
        val apps = getCachedApps()
        val item = apps.find { it.app.id == appId } ?: return@withContext null
        val phoneMissing = item.app.phone != null && item.app.phone.downloadUrl.isNullOrEmpty()
        val watchMissing = item.app.watch != null && item.app.watch.downloadUrl.isNullOrEmpty()
        if ((phoneMissing || watchMissing) && item.app.source == "github_releases") {
            try {
                val updatedApp = resolveGitHubVersions(item.app)
                return@withContext resolveAppDisplayInfo(updatedApp)
            } catch (_: Exception) {
                // Ignore network errors, return item as is
            }
        }
        item
    }

    suspend fun refreshSingleApp(app: AppInfo): AppDisplayInfo = withContext(Dispatchers.IO) {
        if (app.source == "github_releases") {
            try {
                val updatedApp = resolveGitHubVersions(app)
                return@withContext resolveAppDisplayInfo(updatedApp)
            } catch (_: Exception) {}
        }
        resolveAppDisplayInfo(app)
    }

    private fun resolveAppDisplayInfo(app: AppInfo): AppDisplayInfo {
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
            val asset = release.assets.find { matchesPattern(it.name, phoneInfo.assetPattern) }
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
            val asset = release.assets.find { matchesPattern(it.name, watchInfo.assetPattern) }
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

    private fun matchesPattern(assetName: String, pattern: String): Boolean {
        if (pattern.isEmpty()) return true
        val cleanP = pattern.trim().lowercase()
        val cleanA = assetName.trim().lowercase()
        if (cleanP.contains("*") || cleanP.contains("?")) {
            val regex = cleanP
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
            return try {
                Regex(regex, RegexOption.IGNORE_CASE).matches(cleanA)
            } catch (_: Exception) {
                cleanA.contains(cleanP.replace("*", "").replace("?", ""))
            }
        }
        return cleanA.contains(cleanP)
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
