package com.kiwi.manager.ui.screen.watchapps

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.data.adb.AdbConnectionManager
import com.kiwi.manager.data.adb.AdbSessionState
import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.data.repository.AdbRepository
import com.kiwi.manager.domain.model.WatchAppInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WatchAppViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)
    private val adbRepository = AdbRepository(dataStoreManager)

    private val _uiState = MutableStateFlow(WatchAppUiState())
    val uiState: StateFlow<WatchAppUiState> = _uiState.asStateFlow()

    init {
        observeConnection()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            AdbConnectionManager.sessionState.collect { state ->
                when (state) {
                    is AdbSessionState.Connected -> {
                        _uiState.update {
                            it.copy(
                                isConnected = true,
                                isConnecting = false,
                                deviceInfo = state.info
                            )
                        }
                        // Tự động tải danh sách app nếu chưa có
                        if (_uiState.value.apps.isEmpty()) {
                            loadWatchApps()
                        }
                    }
                    is AdbSessionState.Connecting -> {
                        _uiState.update {
                            it.copy(
                                isConnecting = true,
                                isConnected = false
                            )
                        }
                    }
                    is AdbSessionState.Disconnected, is AdbSessionState.Error -> {
                        _uiState.update {
                            it.copy(
                                isConnected = false,
                                isConnecting = false,
                                deviceInfo = null,
                                apps = emptyList()
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadWatchApps() {
        if (!AdbConnectionManager.isConnected) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = adbRepository.getInstalledWatchApps()
            result.onSuccess { appsList ->
                _uiState.update {
                    it.copy(
                        apps = appsList,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                val errorText = error.localizedMessage ?: "Lỗi tải danh sách app trên đồng hồ"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorText,
                        actionFeedbackMessage = if (it.apps.isNotEmpty()) "✗ $errorText" else it.actionFeedbackMessage
                    )
                }
            }
        }
    }

    fun connectToLastDevice() {
        viewModelScope.launch {
            val saved = adbRepository.getSavedDevices()
            val target = saved.maxByOrNull { it.lastConnected } ?: return@launch
            _uiState.update { it.copy(isConnecting = true) }
            adbRepository.connect(target.host, target.port)
        }
    }

    fun setFilter(filter: WatchAppFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Tắt / Buộc dừng ứng dụng trên đồng hồ
     */
    fun forceStopApp(app: WatchAppInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    currentActionPackage = app.packageName
                )
            }

            val result = adbRepository.forceStopApp(app.packageName)
            result.onSuccess {
                // Cập nhật trạng thái running ngay lập tức trên UI
                _uiState.update { state ->
                    val updated = state.apps.map {
                        if (it.packageName == app.packageName) it.copy(isRunning = false) else it
                    }
                    state.copy(
                        apps = updated,
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✓ Đã tắt ${app.appName}"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✗ ${err.localizedMessage}"
                    )
                }
            }
            clearFeedbackMessageDelayed()
        }
    }

    /**
     * Mở ứng dụng trên đồng hồ
     */
    fun launchApp(app: WatchAppInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    currentActionPackage = app.packageName
                )
            }

            val result = adbRepository.launchApp(app.packageName)
            result.onSuccess {
                _uiState.update { state ->
                    val updated = state.apps.map {
                        if (it.packageName == app.packageName) it.copy(isRunning = true) else it
                    }
                    state.copy(
                        apps = updated,
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✓ Đã mở ${app.appName} trên đồng hồ"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✗ ${err.localizedMessage}"
                    )
                }
            }
            clearFeedbackMessageDelayed()
        }
    }

    /**
     * Xóa dữ liệu của ứng dụng trên đồng hồ
     */
    fun clearAppData(app: WatchAppInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    currentActionPackage = app.packageName
                )
            }

            val result = adbRepository.clearAppData(app.packageName)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✓ Đã xóa dữ liệu ${app.appName}"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✗ ${err.localizedMessage}"
                    )
                }
            }
            clearFeedbackMessageDelayed()
        }
    }

    /**
     * Bật / Tắt (Disable / Enable) ứng dụng trên đồng hồ
     */
    fun toggleAppDisabled(app: WatchAppInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    currentActionPackage = app.packageName
                )
            }

            val targetEnable = !app.isEnabled
            val result = adbRepository.setAppEnabled(app.packageName, targetEnable)
            result.onSuccess {
                _uiState.update { state ->
                    val updated = state.apps.map {
                        if (it.packageName == app.packageName) {
                            it.copy(
                                isEnabled = targetEnable,
                                isRunning = if (!targetEnable) false else it.isRunning
                            )
                        } else it
                    }
                    state.copy(
                        apps = updated,
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = if (targetEnable) "✓ Đã kích hoạt ${app.appName}" else "✓ Đã vô hiệu hóa ${app.appName}"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✗ ${err.localizedMessage}"
                    )
                }
            }
            clearFeedbackMessageDelayed()
        }
    }

    fun promptUninstall(app: WatchAppInfo) {
        _uiState.update { it.copy(appToUninstall = app) }
    }

    fun dismissUninstallPrompt() {
        _uiState.update { it.copy(appToUninstall = null) }
    }

    fun confirmUninstall() {
        val app = _uiState.value.appToUninstall ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    appToUninstall = null,
                    isActionInProgress = true,
                    currentActionPackage = app.packageName
                )
            }

            val result = adbRepository.uninstallApp(app.packageName, app.isSystemApp)
            result.onSuccess {
                _uiState.update { state ->
                    val updated = if (app.isSystemApp) {
                        state.apps.map {
                            if (it.packageName == app.packageName) {
                                it.copy(isUninstalledUser0 = true, isEnabled = false, isRunning = false)
                            } else it
                        }
                    } else {
                        state.apps.filter { it.packageName != app.packageName }
                    }
                    state.copy(
                        apps = updated,
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✓ Đã gỡ cài đặt ${app.appName} ${if (app.isSystemApp) "(User 0)" else ""}"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✗ ${err.localizedMessage}"
                    )
                }
            }
            clearFeedbackMessageDelayed()
        }
    }

    /**
     * Khôi phục ứng dụng hệ thống đã gỡ ở User 0
     */
    fun restoreSystemApp(app: WatchAppInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isActionInProgress = true,
                    currentActionPackage = app.packageName
                )
            }

            val result = adbRepository.restoreSystemApp(app.packageName)
            result.onSuccess {
                _uiState.update { state ->
                    val updated = state.apps.map {
                        if (it.packageName == app.packageName) {
                            it.copy(isUninstalledUser0 = false, isEnabled = true)
                        } else it
                    }
                    state.copy(
                        apps = updated,
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✓ Đã khôi phục ${app.appName} thành công"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        currentActionPackage = null,
                        actionFeedbackMessage = "✗ ${err.localizedMessage}"
                    )
                }
            }
            clearFeedbackMessageDelayed()
        }
    }

    fun viewAppDetails(app: WatchAppInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedAppForDetails = app,
                    isLoadingDetails = true,
                    selectedAppDumpsys = null
                )
            }

            val result = adbRepository.getAppDetails(app.packageName)
            result.onSuccess { details ->
                _uiState.update {
                    it.copy(
                        isLoadingDetails = false,
                        selectedAppDumpsys = details
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingDetails = false,
                        selectedAppDumpsys = "Không thể lấy thông tin chi tiết: ${err.localizedMessage}"
                    )
                }
            }
        }
    }

    fun dismissAppDetails() {
        _uiState.update {
            it.copy(
                selectedAppForDetails = null,
                selectedAppDumpsys = null,
                isLoadingDetails = false
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(actionFeedbackMessage = null) }
    }

    private fun clearFeedbackMessageDelayed() {
        viewModelScope.launch {
            delay(3500)
            _uiState.update { it.copy(actionFeedbackMessage = null) }
        }
    }
}
