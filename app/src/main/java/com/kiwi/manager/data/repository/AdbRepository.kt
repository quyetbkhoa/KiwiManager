package com.kiwi.manager.data.repository

import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.domain.model.AdbDevice
import com.kiwi.manager.domain.model.InstallResult
import com.kiwi.manager.domain.model.InstalledVersionInfo
import com.kiwi.manager.util.Constants
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AdbRepository(private val dataStoreManager: DataStoreManager) {

    suspend fun connect(host: String, port: Int): Result<Dadb> = withContext(Dispatchers.IO) {
        try {
            val dadb = Dadb.create(host, port)
            val device = AdbDevice(host, port, isConnected = true, lastConnected = System.currentTimeMillis())
            saveDevice(device)
            Result.success(dadb)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installApk(dadb: Dadb, apkFile: File, packageName: String): InstallResult = withContext(Dispatchers.IO) {
        try {
            dadb.push(apkFile, Constants.ADB_TEMP_PATH)
            val response = dadb.shell("pm install -r -d -t -g ${Constants.ADB_TEMP_PATH}")
            dadb.shell("rm ${Constants.ADB_TEMP_PATH}")

            val output = response.output
            when {
                output.contains("Success") -> InstallResult.Success
                output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") -> 
                    InstallResult.Error("Signature conflict. Please uninstall the existing app first.", true)
                else -> InstallResult.Error(output)
            }
        } catch (e: Exception) {
            InstallResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun getInstalledVersion(dadb: Dadb, packageName: String): InstalledVersionInfo? = withContext(Dispatchers.IO) {
        try {
            val response = dadb.shell("dumpsys package $packageName")
            val output = response.output
            if (!output.contains("versionName=")) return@withContext null

            val versionName = output.substringAfter("versionName=").substringBefore("\n").trim()
            val versionCodeStr = output.substringAfter("versionCode=").substringBefore(" ").trim()
            val versionCode = versionCodeStr.toLongOrNull() ?: 0L

            InstalledVersionInfo(
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                isInstalled = true
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSavedDevices(): List<AdbDevice> = dataStoreManager.getSavedDevices()

    suspend fun saveDevice(device: AdbDevice) = dataStoreManager.saveDevice(device)

    fun disconnect(dadb: Dadb) {
        try {
            dadb.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
