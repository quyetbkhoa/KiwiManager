package com.kiwi.manager.ui.screen.appdetail

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.data.local.PackageManagerWrapper
import com.kiwi.manager.data.remote.CatalogService
import com.kiwi.manager.data.remote.GitHubApiService
import com.kiwi.manager.data.repository.AdbRepository
import com.kiwi.manager.data.repository.CatalogRepository
import com.kiwi.manager.data.repository.DownloadRepository
import com.kiwi.manager.domain.model.DownloadProgress
import com.kiwi.manager.domain.model.InstallResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class AppDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val appId: String = checkNotNull(savedStateHandle["appId"])
    
    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    private val catalogService = CatalogService()
    private val gitHubApiService = GitHubApiService()
    private val dataStoreManager = DataStoreManager(application)
    private val packageManagerWrapper = PackageManagerWrapper(application)
    private val catalogRepository = CatalogRepository(
        application,
        catalogService,
        gitHubApiService,
        dataStoreManager,
        packageManagerWrapper
    )
    private val downloadRepository = DownloadRepository(application)
    private val adbRepository = AdbRepository(dataStoreManager)

    init {
        loadAppDetail()
    }

    private fun loadAppDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val appDisplay = catalogRepository.getApp(appId)
            if (appDisplay != null) {
                _uiState.update { it.copy(app = appDisplay, isLoading = false) }
            } else {
                _uiState.update { 
                    it.copy(isLoading = false, error = "Không tìm thấy thông tin ứng dụng!") 
                }
            }
        }
    }

    fun installPhone() {
        val currentDisplay = _uiState.value.app ?: return
        viewModelScope.launch {
            var app = currentDisplay.app
            var downloadUrl = app.phone?.downloadUrl

            if (downloadUrl.isNullOrEmpty()) {
                addLog("Đang truy xuất link tải mới nhất từ GitHub...")
                val refreshed = catalogRepository.refreshSingleApp(app)
                _uiState.update { it.copy(app = refreshed) }
                app = refreshed.app
                downloadUrl = app.phone?.downloadUrl
            }

            if (downloadUrl.isNullOrEmpty()) {
                addLog("✗ Không tìm thấy link tải APK cho Mobile trên GitHub Release!")
                return@launch
            }

            _uiState.update { it.copy(phoneInstalling = true, phoneDownloadProgress = null) }
            addLog("Bắt đầu tải file APK Mobile...")
            
            val fileName = "${app.id}_phone.apk"
            val result = downloadRepository.downloadApk(downloadUrl, fileName) { progress ->
                _uiState.update { it.copy(phoneDownloadProgress = progress) }
            }

            result.onSuccess { apkFile ->
                addLog("✓ Đã tải xong APK Mobile (${apkFile.length() / 1024} KB)")
                addLog("Đang mở trình cài đặt Android...")
                launchPackageInstaller(apkFile)
                _uiState.update { it.copy(phoneInstalling = false, phoneDownloadProgress = null) }
            }.onFailure { err ->
                addLog("✗ Lỗi tải APK Mobile: ${err.localizedMessage}")
                _uiState.update { it.copy(phoneInstalling = false, phoneDownloadProgress = null) }
            }
        }
    }

    fun installWatch() {
        val currentDisplay = _uiState.value.app ?: return
        viewModelScope.launch {
            var app = currentDisplay.app
            var downloadUrl = app.watch?.downloadUrl

            if (downloadUrl.isNullOrEmpty()) {
                addLog("Đang truy xuất link tải APK Wear OS từ GitHub...")
                val refreshed = catalogRepository.refreshSingleApp(app)
                _uiState.update { it.copy(app = refreshed) }
                app = refreshed.app
                downloadUrl = app.watch?.downloadUrl
            }

            if (downloadUrl.isNullOrEmpty()) {
                addLog("✗ Không tìm thấy link tải APK cho Đồng hồ Wear OS!")
                return@launch
            }

            _uiState.update { it.copy(watchInstalling = true, watchDownloadProgress = null) }
            addLog("Bắt đầu tải file APK Wear OS...")
            
            val fileName = "${app.id}_watch.apk"
            val result = downloadRepository.downloadApk(downloadUrl, fileName) { progress ->
                _uiState.update { it.copy(watchDownloadProgress = progress) }
            }

            result.onSuccess { apkFile ->
                addLog("✓ Đã tải xong APK Wear OS (${apkFile.length() / 1024} KB)")
                addLog("Kiểm tra kết nối Wireless ADB tới đồng hồ...")
                
                // Lấy thiết bị đã lưu
                val savedDevices = adbRepository.getSavedDevices()
                val targetDevice = savedDevices.firstOrNull()
                if (targetDevice == null) {
                    addLog("⚠️ Chưa kết nối đồng hồ qua ADB! Vui lòng vào trang 'Kết nối ADB' để thiết lập trước.")
                    _uiState.update { it.copy(watchInstalling = false, watchDownloadProgress = null) }
                    return@launch
                }

                addLog("Đang kết nối ADB tới ${targetDevice.host}:${targetDevice.port}...")
                val connectResult = adbRepository.connect(targetDevice.host, targetDevice.port)
                connectResult.onSuccess { dadb ->
                    addLog("✓ Đã kết nối ADB! Đang đẩy file APK và cài đặt...")
                    val pkgName = app.watch?.packageName ?: ""
                    val installRes = adbRepository.installApk(dadb, apkFile, pkgName)
                    when (installRes) {
                        is InstallResult.Success -> {
                            addLog("🎉 CÀI ĐẶT THÀNH CÔNG lên đồng hồ qua Wireless ADB!")
                        }
                        is InstallResult.Error -> {
                            addLog("✗ Lỗi cài đặt ADB: ${installRes.message}")
                            if (installRes.isSignatureConflict) {
                                addLog("👉 Gợi ý: Gỡ bản cũ trên đồng hồ trước nếu bản cũ khác chữ ký.")
                            }
                        }
                        is InstallResult.Cancelled -> {
                            addLog("Tiến trình cài đặt bị hủy.")
                        }
                    }
                    adbRepository.disconnect(dadb)
                    _uiState.update { it.copy(watchInstalling = false, watchDownloadProgress = null) }
                }.onFailure { connErr ->
                    addLog("✗ Không thể kết nối ADB: ${connErr.localizedMessage}")
                    addLog("👉 Hãy đảm bảo đồng hồ và điện thoại cùng mạng Wi-Fi và đã bật 'Gỡ lỗi qua Wi-Fi'.")
                    _uiState.update { it.copy(watchInstalling = false, watchDownloadProgress = null) }
                }
            }.onFailure { err ->
                addLog("✗ Lỗi tải APK Wear OS: ${err.localizedMessage}")
                _uiState.update { it.copy(watchInstalling = false, watchDownloadProgress = null) }
            }
        }
    }

    private fun launchPackageInstaller(apkFile: File) {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManagerWrapper.canInstallPackages()) {
                addLog("👉 Cần cấp quyền 'Cài đặt nguồn không xác định' cho Kiwi Manager...")
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
            addLog("✗ Không thể mở PackageInstaller: ${e.localizedMessage}")
        }
    }

    private fun addLog(message: String) {
        _uiState.update { 
            val newLog = it.installLog + message
            it.copy(installLog = newLog) 
        }
    }
}
