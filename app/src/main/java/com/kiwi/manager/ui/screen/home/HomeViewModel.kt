package com.kiwi.manager.ui.screen.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.data.local.PackageManagerWrapper
import com.kiwi.manager.data.remote.CatalogService
import com.kiwi.manager.data.remote.GitHubApiService
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

    init {
        loadInitialApps()
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
