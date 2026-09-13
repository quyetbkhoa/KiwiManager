package com.kiwi.manager.ui.screen.adbconnect

import com.kiwi.manager.domain.model.AdbDevice
import com.kiwi.manager.domain.model.WatchDeviceInfo

data class AdbConnectUiState(
    val ipAddress: String = "",
    val port: String = "5555",
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val connectionError: String? = null,
    val savedDevices: List<AdbDevice> = emptyList(),
    val log: List<String> = emptyList(),
    val deviceInfo: WatchDeviceInfo? = null
)
