package com.kiwi.manager.ui.screen.adbconnect

import com.kiwi.manager.data.adb.AdbTransportType
import com.kiwi.manager.data.adb.BluetoothBridgeMode
import com.kiwi.manager.data.adb.BluetoothDeviceInfo
import com.kiwi.manager.domain.model.AdbDevice
import com.kiwi.manager.domain.model.WatchDeviceInfo

enum class AdbTabMode {
    WIFI,
    BLUETOOTH
}

data class AdbConnectUiState(
    val selectedTab: AdbTabMode = AdbTabMode.WIFI,
    // Wi-Fi
    val ipAddress: String = "",
    val port: String = "5555",
    // Bluetooth
    val pairedBluetoothDevices: List<BluetoothDeviceInfo> = emptyList(),
    val selectedBluetoothDevice: BluetoothDeviceInfo? = null,
    val bluetoothBridgeMode: BluetoothBridgeMode = BluetoothBridgeMode.DIRECT_RFCOMM,
    val hasBluetoothPermission: Boolean = true,
    // Common
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val activeTransport: AdbTransportType = AdbTransportType.WIFI,
    val connectionError: String? = null,
    val savedDevices: List<AdbDevice> = emptyList(),
    val log: List<String> = emptyList(),
    val deviceInfo: WatchDeviceInfo? = null
)
