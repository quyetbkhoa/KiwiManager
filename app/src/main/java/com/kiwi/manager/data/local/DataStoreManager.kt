package com.kiwi.manager.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.kiwi.manager.domain.model.AdbDevice
import com.kiwi.manager.domain.model.InstalledVersionInfo
import com.kiwi.manager.domain.model.ThemeMode
import com.kiwi.manager.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

class DataStoreManager(private val context: Context) {
    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val SAVED_DEVICES_KEY = stringPreferencesKey("saved_adb_devices")
        val CACHED_CATALOG_KEY = stringPreferencesKey("cached_catalog_json")
        val LAST_CATALOG_REFRESH_KEY = longPreferencesKey("last_catalog_refresh")
        val WATCH_INSTALLED_APPS_KEY = stringPreferencesKey("watch_installed_apps_json")
    }

    val themeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val modeString = prefs[THEME_MODE_KEY] ?: ThemeMode.DARK.name
        try {
            ThemeMode.valueOf(modeString)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    suspend fun getThemeMode(): ThemeMode = withContext(Dispatchers.IO) {
        themeFlow.first()
    }

    suspend fun setThemeMode(mode: ThemeMode) = withContext(Dispatchers.IO) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    suspend fun getSavedDevices(): List<AdbDevice> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val devicesJson = prefs[SAVED_DEVICES_KEY]
        if (devicesJson.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                Json.decodeFromString<List<AdbDevice>>(devicesJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveDevice(device: AdbDevice) = withContext(Dispatchers.IO) {
        val current = getSavedDevices().toMutableList()
        current.removeAll { it.host == device.host && it.port == device.port }
        current.add(device)
        context.dataStore.edit { prefs ->
            prefs[SAVED_DEVICES_KEY] = Json.encodeToString(current)
        }
    }

    suspend fun getCachedCatalog(): String? = withContext(Dispatchers.IO) {
        context.dataStore.data.first()[CACHED_CATALOG_KEY]
    }

    suspend fun setCachedCatalog(catalogJson: String) = withContext(Dispatchers.IO) {
        context.dataStore.edit { prefs ->
            prefs[CACHED_CATALOG_KEY] = catalogJson
            prefs[LAST_CATALOG_REFRESH_KEY] = System.currentTimeMillis()
        }
    }

    suspend fun getWatchInstalledApps(): Map<String, InstalledVersionInfo> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val jsonStr = prefs[WATCH_INSTALLED_APPS_KEY]
        if (jsonStr.isNullOrEmpty()) {
            emptyMap()
        } else {
            try {
                Json.decodeFromString<Map<String, InstalledVersionInfo>>(jsonStr)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun saveWatchInstalledApp(info: InstalledVersionInfo) = withContext(Dispatchers.IO) {
        val current = getWatchInstalledApps().toMutableMap()
        current[info.packageName] = info
        context.dataStore.edit { prefs ->
            prefs[WATCH_INSTALLED_APPS_KEY] = Json.encodeToString(current)
        }
    }

    suspend fun saveAllWatchInstalledApps(apps: Map<String, InstalledVersionInfo>) = withContext(Dispatchers.IO) {
        val current = getWatchInstalledApps().toMutableMap()
        current.putAll(apps)
        context.dataStore.edit { prefs ->
            prefs[WATCH_INSTALLED_APPS_KEY] = Json.encodeToString(current)
        }
    }
}
