package com.kiwi.manager.ui.screen.appdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.domain.model.DownloadProgress
import kotlinx.coroutines.delay
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

    init {
        // Construct dependencies here (mocked for now)
        loadAppDetail()
    }

    private fun loadAppDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Simulate loading from CatalogRepository
                delay(1000)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        // app = catalogRepository.getApp(appId)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load app details"
                    )
                }
            }
        }
    }

    fun installPhone() {
        viewModelScope.launch {
            _uiState.update { it.copy(phoneInstalling = true) }
            addLog("Starting phone download...")
            
            // Simulate download progress
            for (i in 1..100 step 10) {
                delay(200)
                val totalBytes = 10L * 1024 * 1024 // 10MB
                val downloadedBytes = totalBytes * i / 100
                _uiState.update { 
                    it.copy(
                        phoneDownloadProgress = DownloadProgress(
                            bytesDownloaded = downloadedBytes,
                            totalBytes = totalBytes,
                            speedBytesPerSec = 1024 * 1024
                        )
                    )
                }
            }
            
            addLog("Phone download complete.")
            addLog("Triggering Package Installer...")
            
            delay(1000)
            _uiState.update { 
                it.copy(phoneInstalling = false, phoneDownloadProgress = null) 
            }
            addLog("Phone installation process started.")
        }
    }

    fun installWatch() {
        viewModelScope.launch {
            _uiState.update { it.copy(watchInstalling = true) }
            addLog("Starting watch download...")
            
            // Simulate download progress
            for (i in 1..100 step 10) {
                delay(200)
                val totalBytes = 5L * 1024 * 1024 // 5MB
                val downloadedBytes = totalBytes * i / 100
                _uiState.update { 
                    it.copy(
                        watchDownloadProgress = DownloadProgress(
                            bytesDownloaded = downloadedBytes,
                            totalBytes = totalBytes,
                            speedBytesPerSec = 512 * 1024
                        )
                    )
                }
            }
            
            addLog("Watch download complete.")
            addLog("Installing via ADB...")
            
            delay(2000)
            addLog("Watch installation successful.")
            _uiState.update { 
                it.copy(watchInstalling = false, watchDownloadProgress = null) 
            }
        }
    }

    private fun addLog(message: String) {
        _uiState.update { 
            val newLog = it.installLog + message
            it.copy(installLog = newLog) 
        }
    }
}
