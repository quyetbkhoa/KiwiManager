package com.kiwi.manager.data.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.kiwi.manager.KiwiManagerApp
import com.kiwi.manager.MainActivity
import com.kiwi.manager.R
import com.kiwi.manager.data.adb.AdbConnectionManager
import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.data.local.PackageManagerWrapper
import com.kiwi.manager.data.remote.CatalogService
import com.kiwi.manager.data.remote.GitHubApiService
import com.kiwi.manager.data.repository.AdbRepository
import com.kiwi.manager.data.repository.CatalogRepository
import com.kiwi.manager.data.repository.DownloadRepository
import com.kiwi.manager.domain.model.AppDisplayInfo
import com.kiwi.manager.domain.model.DownloadProgress
import com.kiwi.manager.domain.model.InstallResult
import com.kiwi.manager.domain.model.InstalledVersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class InstallStage {
    IDLE,
    FETCHING_INFO,
    DOWNLOADING,
    CONNECTING_ADB,
    PUSHING_TO_WATCH,
    INSTALLING_ON_WATCH,
    OPENING_INSTALLER,
    SUCCESS,
    ERROR
}

data class InstallTask(
    val appId: String,
    val isWatch: Boolean,
    val appName: String,
    val packageName: String,
    val stage: InstallStage = InstallStage.IDLE,
    val isInstalling: Boolean = false,
    val downloadProgress: DownloadProgress? = null,
    val statusMessage: String = "",
    val logs: List<String> = emptyList(),
    val error: String? = null,
    val isSuccess: Boolean = false
)

object InstallManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val context: Context get() = KiwiManagerApp.instance

    private val dataStoreManager by lazy { DataStoreManager(context) }
    private val packageManagerWrapper by lazy { PackageManagerWrapper(context) }
    private val downloadRepository by lazy { DownloadRepository(context) }
    private val adbRepository by lazy { AdbRepository(dataStoreManager) }
    private val catalogRepository by lazy {
        CatalogRepository(
            context,
            CatalogService(),
            GitHubApiService(),
            dataStoreManager,
            packageManagerWrapper
        )
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private const val CHANNEL_ID = "kiwi_install_channel"
    private const val CHANNEL_NAME = "Kiwi Cài Đặt Ứng Dụng"

    private val _tasks = MutableStateFlow<Map<String, InstallTask>>(emptyMap())
    val tasks: StateFlow<Map<String, InstallTask>> = _tasks.asStateFlow()

    init {
        createNotificationChannel()
    }

    fun taskKey(appId: String, isWatch: Boolean): String =
        "$appId:${if (isWatch) "watch" else "phone"}"

    fun getTask(appId: String, isWatch: Boolean): InstallTask? {
        return _tasks.value[taskKey(appId, isWatch)]
    }

    private fun updateTask(appId: String, isWatch: Boolean, transform: (InstallTask) -> InstallTask) {
        val key = taskKey(appId, isWatch)
        _tasks.update { current ->
            val existing = current[key] ?: InstallTask(
                appId = appId,
                isWatch = isWatch,
                appName = "",
                packageName = ""
            )
            val updated = transform(existing)
            current + (key to updated)
        }
    }

    private fun addLog(appId: String, isWatch: Boolean, message: String) {
        updateTask(appId, isWatch) { task ->
            task.copy(
                statusMessage = message,
                logs = task.logs + message
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Thông báo tiến trình tải và cài đặt ứng dụng"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        notificationId: Int,
        title: String,
        content: String,
        progress: Int = -1,
        max: Int = 100,
        indeterminate: Boolean = false,
        ongoing: Boolean = true
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            if (progress >= 0 || indeterminate) {
                builder.setProgress(max, progress, indeterminate)
            }

            notificationManager.notify(notificationId, builder.build())
        } catch (_: Exception) {}
    }

    private fun cancelNotification(notificationId: Int) {
        try {
            notificationManager.cancel(notificationId)
        } catch (_: Exception) {}
    }

    fun installWatch(appDisplay: AppDisplayInfo) {
        val app = appDisplay.app
        val appId = app.id
        val key = taskKey(appId, true)
        val notifId = (appId.hashCode() and 0x7FFF) + 1000

        if (_tasks.value[key]?.isInstalling == true) return

        scope.launch {
            try {
                updateTask(appId, true) {
                    it.copy(
                        appName = app.name,
                        packageName = app.watch?.packageName ?: "",
                        isInstalling = true,
                        isSuccess = false,
                        error = null,
                        stage = InstallStage.FETCHING_INFO,
                        downloadProgress = null,
                        logs = listOf("Bắt đầu tiến trình cài đặt Wear OS..."),
                        statusMessage = "Bắt đầu tiến trình..."
                    )
                }

                showNotification(
                    notificationId = notifId,
                    title = "⌚ Wear OS: ${app.name}",
                    content = "Đang kiểm tra gói tải về...",
                    indeterminate = true
                )

                var currentApp = app
                var downloadUrl = currentApp.watch?.downloadUrl

                if (downloadUrl.isNullOrEmpty()) {
                    addLog(appId, true, "Đang truy xuất link tải APK Wear OS từ GitHub...")
                    val refreshed = catalogRepository.refreshSingleApp(currentApp)
                    currentApp = refreshed.app
                    downloadUrl = currentApp.watch?.downloadUrl
                }

                if (downloadUrl.isNullOrEmpty()) {
                    val errMsg = "✗ Không tìm thấy link tải APK cho Đồng hồ Wear OS!"
                    addLog(appId, true, errMsg)
                    updateTask(appId, true) {
                        it.copy(isInstalling = false, stage = InstallStage.ERROR, error = errMsg)
                    }
                    showNotification(notifId, "⌚ Thất bại: ${app.name}", errMsg, ongoing = false)
                    return@launch
                }

                addLog(appId, true, "Bắt đầu tải file APK Wear OS...")
                updateTask(appId, true) { it.copy(stage = InstallStage.DOWNLOADING) }

                val fileName = "${app.id}_watch.apk"
                val downloadRes = downloadRepository.downloadApk(downloadUrl, fileName) { progress ->
                    updateTask(appId, true) { it.copy(downloadProgress = progress) }
                    val percent = progress.percent
                    showNotification(
                        notificationId = notifId,
                        title = "⌚ Đang tải: ${app.name}",
                        content = "$percent% (${progress.downloadedMb} / ${progress.totalMb} MB)",
                        progress = percent,
                        max = 100
                    )
                }

                if (downloadRes.isFailure) {
                    val err = downloadRes.exceptionOrNull()?.localizedMessage ?: "Lỗi tải file APK"
                    val errMsg = "✗ Lỗi tải APK Wear OS: $err"
                    addLog(appId, true, errMsg)
                    updateTask(appId, true) {
                        it.copy(isInstalling = false, stage = InstallStage.ERROR, error = errMsg)
                    }
                    showNotification(notifId, "⌚ Thất bại: ${app.name}", errMsg, ongoing = false)
                    return@launch
                }

                val apkFile = downloadRes.getOrThrow()
                addLog(appId, true, "✓ Đã tải xong APK Wear OS (${apkFile.length() / 1024} KB)")

                // Kiểm tra kết nối Wireless ADB
                updateTask(appId, true) { it.copy(stage = InstallStage.CONNECTING_ADB) }
                showNotification(
                    notificationId = notifId,
                    title = "⌚ ADB: ${app.name}",
                    content = "Đang kết nối đồng hồ...",
                    indeterminate = true
                )

                var dadb = AdbConnectionManager.getActiveDadb()
                if (dadb == null || !AdbConnectionManager.isConnected) {
                    val savedDevices = adbRepository.getSavedDevices()
                    val targetDevice = savedDevices.firstOrNull()
                    if (targetDevice == null) {
                        val errMsg = "⚠️ Chưa có thiết bị đồng hồ ADB! Vui lòng vào tab 'ADB' để kết nối trước."
                        addLog(appId, true, errMsg)
                        updateTask(appId, true) {
                            it.copy(isInstalling = false, stage = InstallStage.ERROR, error = errMsg)
                        }
                        showNotification(notifId, "⌚ Cần kết nối ADB: ${app.name}", errMsg, ongoing = false)
                        return@launch
                    }

                    addLog(appId, true, "Đang kết nối ADB tới ${targetDevice.host}:${targetDevice.port}...")
                    val connectResult = adbRepository.connect(targetDevice.host, targetDevice.port)
                    if (connectResult.isFailure) {
                        val connErr = connectResult.exceptionOrNull()?.localizedMessage ?: "Lỗi kết nối ADB"
                        val errMsg = "✗ Không thể kết nối ADB: $connErr"
                        addLog(appId, true, errMsg)
                        addLog(appId, true, "👉 Hãy đảm bảo đồng hồ và điện thoại cùng mạng Wi-Fi và đã bật 'Gỡ lỗi qua Wi-Fi'.")
                        updateTask(appId, true) {
                            it.copy(isInstalling = false, stage = InstallStage.ERROR, error = errMsg)
                        }
                        showNotification(notifId, "⌚ Lỗi ADB: ${app.name}", errMsg, ongoing = false)
                        return@launch
                    }
                    dadb = connectResult.getOrThrow()
                }

                addLog(appId, true, "✓ Đã kết nối ADB! Đang đẩy file APK và cài đặt...")
                updateTask(appId, true) { it.copy(stage = InstallStage.PUSHING_TO_WATCH) }
                showNotification(
                    notificationId = notifId,
                    title = "⌚ Đang cài đặt: ${app.name}",
                    content = "Đang đẩy APK và cài đặt lên Wear OS...",
                    indeterminate = true
                )

                updateTask(appId, true) { it.copy(stage = InstallStage.INSTALLING_ON_WATCH) }
                val pkgName = app.watch?.packageName ?: ""
                val installRes = adbRepository.installApk(dadb, apkFile, pkgName)

                when (installRes) {
                    is InstallResult.Success -> {
                        addLog(appId, true, "🎉 CÀI ĐẶT THÀNH CÔNG lên đồng hồ qua Wireless ADB!")
                        
                        // Lưu ngay thông tin app đã cài vào DataStore và cập nhật catalog
                        val installedInfo = InstalledVersionInfo(
                            packageName = pkgName,
                            versionName = app.watch?.versionName ?: "",
                            versionCode = app.watch?.versionCode?.toLong() ?: 0L,
                            isInstalled = true
                        )
                        dataStoreManager.saveWatchInstalledApp(installedInfo)
                        catalogRepository.refreshCatalog()

                        updateTask(appId, true) {
                            it.copy(
                                isInstalling = false,
                                isSuccess = true,
                                stage = InstallStage.SUCCESS,
                                statusMessage = "Cài đặt thành công!"
                            )
                        }
                        showNotification(
                            notificationId = notifId,
                            title = "🎉 Thành công: ${app.name}",
                            content = "Đã cài đặt thành công lên Wear OS!",
                            ongoing = false
                        )
                    }
                    is InstallResult.Error -> {
                        addLog(appId, true, "✗ Lỗi cài đặt ADB: ${installRes.message}")
                        if (installRes.isSignatureConflict) {
                            addLog(appId, true, "👉 Gợi ý: Gỡ bản cũ trên đồng hồ trước nếu bản cũ khác chữ ký.")
                        }
                        updateTask(appId, true) {
                            it.copy(
                                isInstalling = false,
                                stage = InstallStage.ERROR,
                                error = installRes.message
                            )
                        }
                        showNotification(
                            notificationId = notifId,
                            title = "✗ Lỗi cài đặt: ${app.name}",
                            content = installRes.message,
                            ongoing = false
                        )
                    }
                    is InstallResult.Cancelled -> {
                        addLog(appId, true, "Tiến trình cài đặt bị hủy.")
                        updateTask(appId, true) {
                            it.copy(isInstalling = false, stage = InstallStage.IDLE)
                        }
                        cancelNotification(notifId)
                    }
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Lỗi không xác định"
                addLog(appId, true, "✗ Ngoại lệ: $err")
                updateTask(appId, true) {
                    it.copy(isInstalling = false, stage = InstallStage.ERROR, error = err)
                }
                showNotification(notifId, "✗ Lỗi: ${app.name}", err, ongoing = false)
            }
        }
    }

    fun installPhone(appDisplay: AppDisplayInfo) {
        val app = appDisplay.app
        val appId = app.id
        val key = taskKey(appId, false)
        val notifId = (appId.hashCode() and 0x7FFF) + 2000

        if (_tasks.value[key]?.isInstalling == true) return

        scope.launch {
            try {
                updateTask(appId, false) {
                    it.copy(
                        appName = app.name,
                        packageName = app.phone?.packageName ?: "",
                        isInstalling = true,
                        isSuccess = false,
                        error = null,
                        stage = InstallStage.FETCHING_INFO,
                        downloadProgress = null,
                        logs = listOf("Bắt đầu tải APK Mobile..."),
                        statusMessage = "Bắt đầu tải về..."
                    )
                }

                showNotification(
                    notificationId = notifId,
                    title = "📱 Mobile: ${app.name}",
                    content = "Đang kiểm tra gói tải về...",
                    indeterminate = true
                )

                var currentApp = app
                var downloadUrl = currentApp.phone?.downloadUrl

                if (downloadUrl.isNullOrEmpty()) {
                    addLog(appId, false, "Đang truy xuất link tải mới nhất từ GitHub...")
                    val refreshed = catalogRepository.refreshSingleApp(currentApp)
                    currentApp = refreshed.app
                    downloadUrl = currentApp.phone?.downloadUrl
                }

                if (downloadUrl.isNullOrEmpty()) {
                    val errMsg = "✗ Không tìm thấy link tải APK cho Mobile trên GitHub Release!"
                    addLog(appId, false, errMsg)
                    updateTask(appId, false) {
                        it.copy(isInstalling = false, stage = InstallStage.ERROR, error = errMsg)
                    }
                    showNotification(notifId, "📱 Thất bại: ${app.name}", errMsg, ongoing = false)
                    return@launch
                }

                addLog(appId, false, "Bắt đầu tải file APK Mobile...")
                updateTask(appId, false) { it.copy(stage = InstallStage.DOWNLOADING) }

                val fileName = "${app.id}_phone.apk"
                val downloadRes = downloadRepository.downloadApk(downloadUrl, fileName) { progress ->
                    updateTask(appId, false) { it.copy(downloadProgress = progress) }
                    val percent = progress.percent
                    showNotification(
                        notificationId = notifId,
                        title = "📱 Đang tải: ${app.name}",
                        content = "$percent% (${progress.downloadedMb} / ${progress.totalMb} MB)",
                        progress = percent,
                        max = 100
                    )
                }

                if (downloadRes.isFailure) {
                    val err = downloadRes.exceptionOrNull()?.localizedMessage ?: "Lỗi tải APK Mobile"
                    val errMsg = "✗ Lỗi tải APK Mobile: $err"
                    addLog(appId, false, errMsg)
                    updateTask(appId, false) {
                        it.copy(isInstalling = false, stage = InstallStage.ERROR, error = errMsg)
                    }
                    showNotification(notifId, "📱 Thất bại: ${app.name}", errMsg, ongoing = false)
                    return@launch
                }

                val apkFile = downloadRes.getOrThrow()
                addLog(appId, false, "✓ Đã tải xong APK Mobile (${apkFile.length() / 1024} KB)")
                addLog(appId, false, "Đang mở trình cài đặt Android...")
                updateTask(appId, false) {
                    it.copy(isInstalling = false, stage = InstallStage.OPENING_INSTALLER)
                }

                launchPackageInstaller(apkFile, appId)
                cancelNotification(notifId)
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Lỗi không xác định"
                addLog(appId, false, "✗ Ngoại lệ: $err")
                updateTask(appId, false) {
                    it.copy(isInstalling = false, stage = InstallStage.ERROR, error = err)
                }
                showNotification(notifId, "✗ Lỗi: ${app.name}", err, ongoing = false)
            }
        }
    }

    private fun launchPackageInstaller(apkFile: File, appId: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManagerWrapper.canInstallPackages()) {
                addLog(appId, false, "👉 Cần cấp quyền 'Cài đặt nguồn không xác định' cho Kiwi Manager...")
                val manageIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(manageIntent)
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            addLog(appId, false, "✗ Không thể mở PackageInstaller: ${e.localizedMessage}")
        }
    }
}
