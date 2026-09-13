package com.kiwi.manager.data.repository

import com.kiwi.manager.data.adb.AdbConnectionManager
import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.domain.model.AdbDevice
import com.kiwi.manager.domain.model.InstallResult
import com.kiwi.manager.domain.model.InstalledVersionInfo
import com.kiwi.manager.domain.model.WatchAppInfo
import com.kiwi.manager.domain.model.WatchDeviceInfo
import com.kiwi.manager.util.Constants
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AdbRepository(private val dataStoreManager: DataStoreManager) {

    suspend fun connect(host: String, port: Int): Result<Dadb> = withContext(Dispatchers.IO) {
        val result = AdbConnectionManager.connect(host, port)
        result.onSuccess {
            val device = AdbDevice(host, port, isConnected = true, lastConnected = System.currentTimeMillis())
            saveDevice(device)
        }
        result
    }

    suspend fun executeShell(command: String): Result<String> = AdbConnectionManager.executeShell(command)

    fun isConnected(): Boolean = AdbConnectionManager.isConnected

    fun getActiveDadb(): Dadb? = AdbConnectionManager.getActiveDadb()

    fun disconnect(dadb: Dadb? = null) {
        AdbConnectionManager.disconnect()
    }

    suspend fun getWatchDeviceInfo(): WatchDeviceInfo? = AdbConnectionManager.refreshDeviceInfo()

    /**
     * Lấy danh sách toàn bộ ứng dụng trên đồng hồ qua ADB, phân loại User/System và kiểm tra app đang chạy.
     */
    suspend fun getInstalledWatchApps(): Result<List<WatchAppInfo>> = withContext(Dispatchers.IO) {
        try {
            val dadb = AdbConnectionManager.getActiveDadb()
                ?: return@withContext Result.failure(IllegalStateException("Chưa kết nối ADB tới đồng hồ"))

            // 1. Lấy danh sách User Apps (-3)
            val userOutput = dadb.shell("pm list packages -3 -f").output
            val userMap = parsePackageList(userOutput)

            // 2. Lấy danh sách System Apps (-s)
            val sysOutput = dadb.shell("pm list packages -s -f").output
            val sysMap = parsePackageList(sysOutput)

            // 3. Lấy danh sách Disabled Apps (-d)
            val disabledOutput = dadb.shell("pm list packages -d").output
            val disabledSet = disabledOutput.lines()
                .map { it.trim().removePrefix("package:") }
                .filter { it.isNotEmpty() }
                .toSet()

            // 4. Lấy danh sách tiến trình đang chạy (ps -A)
            val psOutput = dadb.shell("ps -A").output
            val runningSet = mutableSetOf<String>()
            psOutput.lines().forEach { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.isNotEmpty()) {
                    val processName = parts.last()
                    runningSet.add(processName)
                }
            }

            // Tổng hợp danh sách ứng dụng
            val allApps = mutableListOf<WatchAppInfo>()

            // Thêm User apps
            userMap.forEach { (pkg, apkPath) ->
                val isRunning = runningSet.any { it == pkg || it.startsWith("$pkg:") }
                val isEnabled = !disabledSet.contains(pkg)
                allApps.add(
                    WatchAppInfo(
                        packageName = pkg,
                        appName = formatFriendlyAppName(pkg),
                        isSystemApp = false,
                        isRunning = isRunning,
                        isEnabled = isEnabled,
                        apkPath = apkPath
                    )
                )
            }

            // Thêm System apps
            sysMap.forEach { (pkg, apkPath) ->
                val isRunning = runningSet.any { it == pkg || it.startsWith("$pkg:") }
                val isEnabled = !disabledSet.contains(pkg)
                allApps.add(
                    WatchAppInfo(
                        packageName = pkg,
                        appName = formatFriendlyAppName(pkg),
                        isSystemApp = true,
                        isRunning = isRunning,
                        isEnabled = isEnabled,
                        apkPath = apkPath
                    )
                )
            }

            // Cập nhật số lượng app vào WatchDeviceInfo
            AdbConnectionManager.deviceInfo.value?.let { currentInfo ->
                AdbConnectionManager.updateDeviceInfo(
                    currentInfo.copy(
                        totalApps = allApps.size,
                        userApps = userMap.size,
                        systemApps = sysMap.size,
                        runningApps = allApps.count { it.isRunning }
                    )
                )
            }

            // Sắp xếp: User apps lên trước, sau đó theo tên
            val sorted = allApps.sortedWith(
                compareBy<WatchAppInfo> { it.isSystemApp }
                    .thenByDescending { it.isRunning }
                    .thenBy { it.appName.lowercase() }
            )

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tắt / Buộc dừng ứng dụng trên đồng hồ
     */
    suspend fun forceStopApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = AdbConnectionManager.executeShell("am force-stop $packageName")
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Khởi chạy ứng dụng trên đồng hồ
     */
    suspend fun launchApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Dùng monkey để mở launcher activity mặc định mà không cần biết Activity class
            val res = AdbConnectionManager.executeShell("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xóa dữ liệu và bộ nhớ đệm của ứng dụng
     */
    suspend fun clearAppData(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = AdbConnectionManager.executeShell("pm clear $packageName")
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Vô hiệu hóa hoặc kích hoạt lại ứng dụng
     */
    suspend fun setAppEnabled(packageName: String, enable: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cmd = if (enable) "pm enable $packageName" else "pm disable-user --user 0 $packageName"
            val res = AdbConnectionManager.executeShell(cmd)
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gỡ cài đặt ứng dụng
     */
    suspend fun uninstallApp(packageName: String, isSystemApp: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cmd = if (isSystemApp) {
                // Với system app, gỡ cho user 0 (debloat an toàn không mất gốc)
                "pm uninstall -k --user 0 $packageName"
            } else {
                "pm uninstall $packageName"
            }
            val res = AdbConnectionManager.executeShell(cmd)
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lấy chi tiết kỹ thuật của ứng dụng (dumpsys package)
     */
    suspend fun getAppDetails(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = AdbConnectionManager.executeShell("dumpsys package $packageName")
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parsePackageList(output: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        output.lines().forEach { line ->
            val clean = line.trim().removePrefix("package:")
            if (clean.contains("=")) {
                val apkPath = clean.substringBeforeLast("=")
                val pkgName = clean.substringAfterLast("=")
                if (pkgName.isNotBlank()) {
                    map[pkgName] = apkPath
                }
            } else if (clean.isNotBlank()) {
                map[clean] = ""
            }
        }
        return map
    }

    private fun formatFriendlyAppName(packageName: String): String {
        val lastSegment = packageName.substringAfterLast(".")
        val words = lastSegment.split("(?<=[a-z])(?=[A-Z])|_|-".toRegex())
            .filter { it.isNotBlank() }
            .map { it.replaceFirstChar { ch -> ch.uppercase() } }
        val candidate = words.joinToString(" ")
        return candidate.ifBlank { packageName }
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
}
