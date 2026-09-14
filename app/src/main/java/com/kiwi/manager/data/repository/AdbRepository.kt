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
     * Lấy danh sách toàn bộ ứng dụng trên đồng hồ qua ADB, phân loại User/System, kiểm tra app đang chạy,
     * app bị vô hiệu hóa, và app hệ thống đã gỡ ở User 0.
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

            // 4. Lấy danh sách Uninstalled Apps (-u) để phát hiện system apps đã bị debloat cho User 0
            val uninstalledOutput = dadb.shell("pm list packages -u").output
            val allPackagesSet = uninstalledOutput.lines()
                .map { it.trim().removePrefix("package:") }
                .filter { it.isNotEmpty() }
                .toSet()

            // 5. Lấy danh sách tiến trình đang chạy (ps -A)
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
                        isUninstalledUser0 = false,
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
                        isUninstalledUser0 = false,
                        apkPath = apkPath
                    )
                )
            }

            // Thêm các app hệ thống đã gỡ ở User 0 (xuất hiện trong -u nhưng không có trong userMap và sysMap)
            allPackagesSet.forEach { pkg ->
                if (!userMap.containsKey(pkg) && !sysMap.containsKey(pkg)) {
                    allApps.add(
                        WatchAppInfo(
                            packageName = pkg,
                            appName = formatFriendlyAppName(pkg),
                            isSystemApp = true,
                            isRunning = false,
                            isEnabled = false,
                            isUninstalledUser0 = true,
                            apkPath = null
                        )
                    )
                }
            }

            // Cập nhật số lượng app vào WatchDeviceInfo
            AdbConnectionManager.deviceInfo.value?.let { currentInfo ->
                AdbConnectionManager.updateDeviceInfo(
                    currentInfo.copy(
                        totalApps = allApps.count { !it.isUninstalledUser0 },
                        userApps = userMap.size,
                        systemApps = sysMap.size,
                        runningApps = allApps.count { it.isRunning && !it.isUninstalledUser0 }
                    )
                )
            }

            // Sắp xếp: User apps lên trước, sau đó theo trạng thái chạy, trạng thái bật và tên
            val sorted = allApps.sortedWith(
                compareBy<WatchAppInfo> { it.isUninstalledUser0 }
                    .thenBy { it.isSystemApp }
                    .thenByDescending { it.isRunning }
                    .thenByDescending { it.isEnabled }
                    .thenBy { it.appName.lowercase() }
            )

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Tắt / Buộc dừng ứng dụng trên đồng hồ (báo thật dựa trên exit code)
     */
    suspend fun forceStopApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        val res = AdbConnectionManager.executeShellRaw("am force-stop $packageName")
        res.fold(
            onSuccess = { response ->
                if (response.exitCode != 0) {
                    val cleanMsg = extractCleanErrorMessage(response.allOutput, "Không thể dừng ứng dụng")
                    Result.failure(Exception(cleanMsg))
                } else {
                    Result.success("Đã dừng ứng dụng $packageName")
                }
            },
            onFailure = { e -> Result.failure(e) }
        )
    }

    /**
     * Khởi chạy ứng dụng trên đồng hồ (kiểm tra thật xem có Launcher Activity không)
     */
    suspend fun launchApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        val res = AdbConnectionManager.executeShellRaw("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
        res.fold(
            onSuccess = { response ->
                val text = response.allOutput.trim()
                if (text.contains("No activities found to run", ignoreCase = true)) {
                    Result.failure(Exception("Ứng dụng không có màn hình mở (đây là dịch vụ chạy nền)"))
                } else if (text.contains("Events injected: 1", ignoreCase = true)) {
                    Result.success("Đã mở ứng dụng")
                } else if (response.exitCode != 0 || text.contains("Exception", ignoreCase = true)) {
                    Result.failure(Exception(extractCleanErrorMessage(text, "Không thể mở ứng dụng")))
                } else {
                    Result.success("Đã gửi lệnh mở")
                }
            },
            onFailure = { e -> Result.failure(e) }
        )
    }

    /**
     * Xóa dữ liệu và bộ nhớ đệm của ứng dụng (báo thật)
     */
    suspend fun clearAppData(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        val res = AdbConnectionManager.executeShellRaw("pm clear $packageName")
        res.fold(
            onSuccess = { response ->
                val text = response.allOutput.trim()
                if (text.contains("Success", ignoreCase = true)) {
                    Result.success("Đã xóa dữ liệu của $packageName")
                } else {
                    Result.failure(Exception(extractCleanErrorMessage(text, "Không thể xóa dữ liệu (Hệ thống từ chối)")))
                }
            },
            onFailure = { e -> Result.failure(e) }
        )
    }

    /**
     * Vô hiệu hóa hoặc kích hoạt lại ứng dụng (báo thật 100%, kiểm tra lỗi SecurityException hoặc exitCode)
     */
    suspend fun setAppEnabled(packageName: String, enable: Boolean): Result<String> = withContext(Dispatchers.IO) {
        val cmd = if (enable) "pm enable $packageName" else "pm disable-user --user 0 $packageName"
        val res = AdbConnectionManager.executeShellRaw(cmd)
        res.fold(
            onSuccess = { response ->
                val text = response.allOutput.trim()
                val exitCode = response.exitCode

                if (exitCode != 0 || text.contains("Exception", ignoreCase = true) || text.contains("Error:", ignoreCase = true)) {
                    val cleanMsg = extractCleanErrorMessage(text, "Không thể ${if (enable) "kích hoạt" else "vô hiệu hóa"} $packageName")
                    Result.failure(Exception(cleanMsg))
                } else if (text.contains("new state:", ignoreCase = true)) {
                    Result.success(text)
                } else {
                    // Xác thực lại trạng thái thực tế
                    val verifyRes = AdbConnectionManager.executeShell("pm list packages -d $packageName")
                    val isDisabled = verifyRes.getOrNull()?.contains(packageName) == true
                    if (enable && !isDisabled) {
                        Result.success("Đã kích hoạt $packageName")
                    } else if (!enable && isDisabled) {
                        Result.success("Đã vô hiệu hóa $packageName")
                    } else {
                        Result.failure(Exception("Hệ thống không đổi được trạng thái: $text"))
                    }
                }
            },
            onFailure = { e -> Result.failure(e) }
        )
    }

    /**
     * Gỡ cài đặt ứng dụng (báo thật 100% dựa trên chữ 'Success' từ ADB)
     */
    suspend fun uninstallApp(packageName: String, isSystemApp: Boolean): Result<String> = withContext(Dispatchers.IO) {
        val cmd = if (isSystemApp) {
            "pm uninstall -k --user 0 $packageName"
        } else {
            "pm uninstall $packageName"
        }
        val res = AdbConnectionManager.executeShellRaw(cmd)
        res.fold(
            onSuccess = { response ->
                val text = response.allOutput.trim()
                if (text.contains("Success", ignoreCase = true)) {
                    Result.success("Đã gỡ cài đặt $packageName")
                } else {
                    val cleanMsg = extractCleanErrorMessage(text, "Gỡ cài đặt thất bại")
                    Result.failure(Exception(cleanMsg))
                }
            },
            onFailure = { e -> Result.failure(e) }
        )
    }

    /**
     * Khôi phục ứng dụng hệ thống đã gỡ ở User 0
     */
    suspend fun restoreSystemApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        val cmd = "cmd package install-existing $packageName"
        val res = AdbConnectionManager.executeShellRaw(cmd)
        res.fold(
            onSuccess = { response ->
                val text = response.allOutput.trim()
                if (text.contains("installed for user", ignoreCase = true) || text.contains("Success", ignoreCase = true)) {
                    Result.success("Đã khôi phục $packageName")
                } else {
                    val cleanMsg = extractCleanErrorMessage(text, "Khôi phục ứng dụng thất bại")
                    Result.failure(Exception(cleanMsg))
                }
            },
            onFailure = { e -> Result.failure(e) }
        )
    }

    private fun extractCleanErrorMessage(rawOutput: String, fallback: String): String {
        val lines = rawOutput.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val failureLine = lines.firstOrNull { it.startsWith("Failure", ignoreCase = true) }
        if (failureLine != null) return failureLine

        val exLine = lines.firstOrNull { it.contains("SecurityException", ignoreCase = true) }
            ?: lines.firstOrNull { it.contains("Exception:", ignoreCase = true) }
            ?: lines.firstOrNull { it.startsWith("Error:", ignoreCase = true) }

        if (exLine != null) {
            val msg = exLine.substringAfter(":").trim()
            return msg.ifEmpty { exLine }
        }
        return lines.firstOrNull() ?: fallback
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
