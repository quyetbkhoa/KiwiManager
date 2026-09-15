package com.kiwi.manager.ui.screen.adbconnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.data.adb.AdbConnectionManager
import com.kiwi.manager.data.adb.AdbSessionState
import com.kiwi.manager.data.adb.AdbTransportType
import com.kiwi.manager.data.adb.BluetoothBridgeMode
import com.kiwi.manager.data.adb.BluetoothDeviceInfo
import com.kiwi.manager.data.local.DataStoreManager
import com.kiwi.manager.data.repository.AdbRepository
import com.kiwi.manager.domain.model.AdbDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdbConnectViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)
    private val adbRepository = AdbRepository(dataStoreManager)

    private val _uiState = MutableStateFlow(AdbConnectUiState())
    val uiState: StateFlow<AdbConnectUiState> = _uiState.asStateFlow()

    init {
        loadSavedDevices()
        observeConnectionState()
        refreshPairedDevices()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            AdbConnectionManager.sessionState.collect { state ->
                when (state) {
                    is AdbSessionState.Connected -> {
                        _uiState.update {
                            it.copy(
                                isConnected = true,
                                isConnecting = false,
                                activeTransport = state.transport,
                                connectionError = null,
                                deviceInfo = state.info
                            )
                        }
                    }
                    is AdbSessionState.Connecting -> {
                        _uiState.update {
                            it.copy(
                                isConnecting = true,
                                activeTransport = state.transport,
                                connectionError = null
                            )
                        }
                    }
                    is AdbSessionState.Disconnected -> {
                        _uiState.update {
                            it.copy(
                                isConnected = false,
                                isConnecting = false,
                                deviceInfo = null
                            )
                        }
                    }
                    is AdbSessionState.Error -> {
                        _uiState.update {
                            it.copy(
                                isConnected = false,
                                isConnecting = false,
                                connectionError = state.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadSavedDevices() {
        viewModelScope.launch {
            val saved = adbRepository.getSavedDevices()
            _uiState.update { it.copy(savedDevices = saved) }
            if (saved.isNotEmpty() && _uiState.value.ipAddress.isEmpty()) {
                val latestWifi = saved.filter { !it.host.contains(":") || it.port > 0 }
                    .maxByOrNull { it.lastConnected }
                if (latestWifi != null) {
                    _uiState.update {
                        it.copy(
                            ipAddress = latestWifi.host,
                            port = latestWifi.port.toString()
                        )
                    }
                }
            }
        }
    }

    fun selectTab(tab: AdbTabMode) {
        _uiState.update { it.copy(selectedTab = tab, connectionError = null) }
        if (tab == AdbTabMode.BLUETOOTH) {
            refreshPairedDevices()
        }
    }

    fun refreshPairedDevices() {
        val paired = adbRepository.getPairedBluetoothDevices()
        _uiState.update { current ->
            val selected = current.selectedBluetoothDevice?.let { sel ->
                paired.find { it.address == sel.address }
            } ?: paired.firstOrNull { it.isLikelyWatch } ?: paired.firstOrNull()

            current.copy(
                pairedBluetoothDevices = paired,
                selectedBluetoothDevice = selected
            )
        }
    }

    fun selectBluetoothDevice(device: BluetoothDeviceInfo) {
        _uiState.update { it.copy(selectedBluetoothDevice = device, connectionError = null) }
    }

    fun updateBluetoothBridgeMode(mode: BluetoothBridgeMode) {
        _uiState.update { it.copy(bluetoothBridgeMode = mode) }
    }

    fun updateIp(ip: String) {
        _uiState.update { it.copy(ipAddress = ip.trim()) }
    }

    fun updatePort(port: String) {
        _uiState.update { it.copy(port = port.trim()) }
    }

    fun connect() {
        if (_uiState.value.selectedTab == AdbTabMode.BLUETOOTH) {
            connectBluetooth()
        } else {
            connectWifi()
        }
    }

    fun connectWifi() {
        viewModelScope.launch {
            val ip = _uiState.value.ipAddress.trim()
            val portStr = _uiState.value.port.trim()
            val port = portStr.toIntOrNull() ?: 5555

            if (ip.isBlank()) {
                _uiState.update { it.copy(connectionError = "Địa chỉ IP không được để trống!") }
                return@launch
            }

            _uiState.update { it.copy(isConnecting = true, connectionError = null) }
            addLog("Đang kết nối Wireless ADB tới $ip:$port...")

            val result = adbRepository.connect(ip, port)
            result.onSuccess {
                addLog("✓ Kết nối thành công tới $ip:$port!")
                logDeviceInfo()
                loadSavedDevices()
            }.onFailure { e ->
                val errorMsg = e.localizedMessage ?: "Kết nối thất bại"
                addLog("✗ Lỗi kết nối: $errorMsg")
                addLog("👉 Hãy đảm bảo đồng hồ đã bật 'Gỡ lỗi ADB' và 'Gỡ lỗi qua Wi-Fi' trong Tùy chọn nhà phát triển.")
            }
        }
    }

    fun connectBluetooth() {
        viewModelScope.launch {
            val device = _uiState.value.selectedBluetoothDevice
            if (device == null) {
                _uiState.update { it.copy(connectionError = "Vui lòng chọn thiết bị Bluetooth đồng hồ!") }
                return@launch
            }

            val mode = _uiState.value.bluetoothBridgeMode
            val modeStr = if (mode == BluetoothBridgeMode.DIRECT_RFCOMM) "Direct RFCOMM Bridge" else "Wear OS adb-hub"

            _uiState.update { it.copy(isConnecting = true, connectionError = null) }
            addLog("Đang thiết lập kết nối Bluetooth ADB ($modeStr)...")
            addLog("Mục tiêu: ${device.name} [${device.address}]")

            val result = adbRepository.connectBluetooth(device.address, device.name, mode)
            result.onSuccess {
                addLog("✓ Kết nối Bluetooth ADB thành công tới ${device.name}!")
                logDeviceInfo()
                loadSavedDevices()
            }.onFailure { e ->
                val errorMsg = e.localizedMessage ?: "Kết nối Bluetooth thất bại"
                addLog("✗ Lỗi kết nối Bluetooth: $errorMsg")
                if (mode == BluetoothBridgeMode.DIRECT_RFCOMM) {
                    addLog("👉 Hãy đảm bảo: Bluetooth trên cả 2 máy đã bật & ghép đôi, app Gemini Companion trên đồng hồ đang chạy.")
                } else {
                    addLog("👉 Hãy đảm bảo: Ứng dụng Wear OS companion trên điện thoại đã bật 'Gỡ lỗi qua Bluetooth'.")
                }
            }
        }
    }

    private suspend fun logDeviceInfo() {
        val info = adbRepository.getWatchDeviceInfo()
        if (info != null) {
            addLog("Thiết bị: ${info.manufacturer} ${info.model} (Android ${info.androidVersion})")
            if (info.batteryLevel != null) {
                addLog("Pin: ${info.batteryLevel}%${if (info.isCharging) " (Đang sạc)" else ""}")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            addLog("Đang ngắt kết nối ADB...")
            adbRepository.disconnect()
            addLog("✓ Đã ngắt kết nối ADB.")
        }
    }

    fun selectSavedDevice(device: AdbDevice) {
        if (device.port == 0 || device.host.contains(":")) {
            // Bluetooth device
            val found = _uiState.value.pairedBluetoothDevices.find { it.address.equals(device.host, ignoreCase = true) }
            if (found != null) {
                _uiState.update { it.copy(selectedTab = AdbTabMode.BLUETOOTH, selectedBluetoothDevice = found) }
            }
        } else {
            // Wi-Fi device
            _uiState.update {
                it.copy(
                    selectedTab = AdbTabMode.WIFI,
                    ipAddress = device.host,
                    port = device.port.toString()
                )
            }
        }
    }

    private fun addLog(message: String) {
        _uiState.update {
            it.copy(log = it.log + message)
        }
    }
}
