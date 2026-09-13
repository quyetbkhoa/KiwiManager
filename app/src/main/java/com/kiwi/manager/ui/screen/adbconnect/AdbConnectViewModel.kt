package com.kiwi.manager.ui.screen.adbconnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.data.adb.AdbConnectionManager
import com.kiwi.manager.data.adb.AdbSessionState
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
                                connectionError = null,
                                deviceInfo = state.info
                            )
                        }
                    }
                    is AdbSessionState.Connecting -> {
                        _uiState.update {
                            it.copy(
                                isConnecting = true,
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
                val latest = saved.maxByOrNull { it.lastConnected } ?: saved.first()
                _uiState.update {
                    it.copy(
                        ipAddress = latest.host,
                        port = latest.port.toString()
                    )
                }
            }
        }
    }

    fun updateIp(ip: String) {
        _uiState.update { it.copy(ipAddress = ip.trim()) }
    }

    fun updatePort(port: String) {
        _uiState.update { it.copy(port = port.trim()) }
    }

    fun connect() {
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
                val info = adbRepository.getWatchDeviceInfo()
                if (info != null) {
                    addLog("Thiết bị: ${info.manufacturer} ${info.model} (Android ${info.androidVersion})")
                    if (info.batteryLevel != null) {
                        addLog("Pin: ${info.batteryLevel}%${if (info.isCharging) " (Đang sạc)" else ""}")
                    }
                }
                loadSavedDevices()
            }.onFailure { e ->
                val errorMsg = e.localizedMessage ?: "Kết nối thất bại"
                addLog("✗ Lỗi kết nối: $errorMsg")
                addLog("👉 Hãy đảm bảo đồng hồ đã bật 'Gỡ lỗi ADB' và 'Gỡ lỗi qua Wi-Fi' trong Tùy chọn nhà phát triển.")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            val ip = _uiState.value.ipAddress
            val port = _uiState.value.port
            addLog("Đang ngắt kết nối $ip:$port...")
            adbRepository.disconnect()
            addLog("✓ Đã ngắt kết nối ADB.")
        }
    }

    fun selectSavedDevice(device: AdbDevice) {
        _uiState.update {
            it.copy(
                ipAddress = device.host,
                port = device.port.toString()
            )
        }
    }

    private fun addLog(message: String) {
        _uiState.update {
            it.copy(log = it.log + message)
        }
    }
}
