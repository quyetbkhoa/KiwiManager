package com.kiwi.manager.ui.screen.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.data.adb.AdbConnectionManager
import com.kiwi.manager.data.adb.AdbSessionState
import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.data.local.PackageManagerWrapper
import com.kiwi.manager.data.remote.CatalogService
import com.kiwi.manager.data.remote.GitHubApiService
import com.kiwi.manager.data.repository.AdbRepository
import com.kiwi.manager.data.repository.CatalogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
    private val adbRepository = AdbRepository(dataStoreManager)

    init {
        loadInitialApps()
        observeAdbConnection()
        autoConnectAdb()
    }

    private fun observeAdbConnection() {
        viewModelScope.launch {
            AdbConnectionManager.sessionState.collect { state ->
                when (state) {
                    is AdbSessionState.Connected -> {
                        _uiState.update { it.copy(isWatchConnected = true) }
                        // Tự động đồng bộ các app đã cài trên đồng hồ
                        catalogRepository.syncWatchInstalledApps()
                        val updated = catalogRepository.getCachedApps()
                        if (updated.isNotEmpty()) {
                            _uiState.update { it.copy(apps = updated) }
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isWatchConnected = false) }
                    }
                }
            }
        }
    }

    private fun autoConnectAdb() {
        viewModelScope.launch {
            if (!AdbConnectionManager.isConnected) {
                val saved = adbRepository.getSavedDevices().firstOrNull()
                if (saved != null) {
                    adbRepository.connect(saved.host, saved.port)
                }
            }
        }
    }

    private fun loadInitialApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val cached = catalogRepository.getCachedApps()
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(apps = cached, isLoading = false) }
            }
            refreshCatalog()
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            val result = catalogRepository.refreshCatalog()
            result.onSuccess { apps ->
                _uiState.update { 
                    it.copy(
                        apps = apps, 
                        isRefreshing = false, 
                        isLoading = false,
                        isOffline = false
                    ) 
                }
                if (AdbConnectionManager.isConnected) {
                    catalogRepository.syncWatchInstalledApps()
                    val synced = catalogRepository.getCachedApps()
                    if (synced.isNotEmpty()) {
                        _uiState.update { it.copy(apps = synced) }
                    }
                }
            }.onFailure { err ->
                val cached = catalogRepository.getCachedApps()
                _uiState.update { 
                    it.copy(
                        apps = if (cached.isNotEmpty()) cached else it.apps,
                        isRefreshing = false, 
                        isLoading = false,
                        isOffline = true,
                        error = if (it.apps.isEmpty() && cached.isEmpty()) err.localizedMessage else null
                    ) 
                }
            }
        }
    }

    fun reloadLocalStatus() {
        viewModelScope.launch {
            if (AdbConnectionManager.isConnected) {
                catalogRepository.syncWatchInstalledApps()
            }
            val cached = catalogRepository.getCachedApps()
            if (cached.isNotEmpty()) {
                _uiState.update { it.copy(apps = cached) }
            }
        }
    }

    fun onRetry() {
        refreshCatalog()
    }
}
