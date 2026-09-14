package com.kiwi.manager.ui.screen.appdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.data.local.PackageManagerWrapper
import com.kiwi.manager.data.remote.CatalogService
import com.kiwi.manager.data.remote.GitHubApiService
import com.kiwi.manager.data.repository.CatalogRepository
import com.kiwi.manager.data.manager.InstallManager
import com.kiwi.manager.domain.model.InstallStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    init {
        loadAppDetail()
        observeInstallTasks()
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

    private fun observeInstallTasks() {
        viewModelScope.launch {
            InstallManager.tasks.collect { map ->
                val watchTask = map[InstallManager.taskKey(appId, true)]
                val phoneTask = map[InstallManager.taskKey(appId, false)]

                if (watchTask?.isSuccess == true) {
                    refreshInstalledStatus()
                }

                val combinedLogs = (phoneTask?.logs ?: emptyList()) + (watchTask?.logs ?: emptyList())

                _uiState.update { current ->
                    current.copy(
                        watchInstalling = watchTask?.isInstalling == true,
                        watchStage = watchTask?.stage ?: InstallStage.IDLE,
                        watchDownloadProgress = watchTask?.downloadProgress,
                        watchPushProgress = watchTask?.pushProgress,
                        watchStatusMessage = watchTask?.statusMessage ?: "",
                        phoneInstalling = phoneTask?.isInstalling == true,
                        phoneStage = phoneTask?.stage ?: InstallStage.IDLE,
                        phoneDownloadProgress = phoneTask?.downloadProgress,
                        phonePushProgress = phoneTask?.pushProgress,
                        phoneStatusMessage = phoneTask?.statusMessage ?: "",
                        installLog = if (combinedLogs.isNotEmpty()) combinedLogs else current.installLog
                    )
                }
            }
        }
    }

    fun refreshInstalledStatus() {
        viewModelScope.launch {
            val appDisplay = catalogRepository.getApp(appId)
            if (appDisplay != null) {
                _uiState.update { it.copy(app = appDisplay) }
            }
        }
    }

    fun installPhone() {
        val currentDisplay = _uiState.value.app ?: return
        InstallManager.installPhone(currentDisplay)
    }

    fun installWatch() {
        val currentDisplay = _uiState.value.app ?: return
        InstallManager.installWatch(currentDisplay)
    }
}
