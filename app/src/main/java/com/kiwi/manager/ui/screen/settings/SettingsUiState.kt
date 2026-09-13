package com.kiwi.manager.ui.screen.settings

import com.kiwi.manager.domain.model.ThemeMode

data class SettingsUiState(
    val currentTheme: ThemeMode = ThemeMode.DARK,
    val appVersion: String = "1.0.0"
)
