package com.kiwi.manager.ui.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Construct DataStoreManager and read current theme
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Mock loading version and theme
            _uiState.update { 
                it.copy(
                    appVersion = "1.0.0 (Beta)",
                    currentTheme = ThemeMode.DARK
                )
            }
        }
    }

    fun setTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            // Mock saving to DataStore
            _uiState.update { it.copy(currentTheme = themeMode) }
        }
    }
}
