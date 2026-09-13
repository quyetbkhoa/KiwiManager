package com.kiwi.manager.ui.screen.adbconnect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kiwi.manager.domain.model.AdbDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdbConnectViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AdbConnectUiState())
    val uiState: StateFlow<AdbConnectUiState> = _uiState.asStateFlow()

    init {
        // Construct AdbRepository here (mocked)
        loadSavedDevices()
    }

    private fun loadSavedDevices() {
        viewModelScope.launch {
            // Mock saved devices
            _uiState.update { 
                it.copy(
                    savedDevices = listOf(
                        AdbDevice("192.168.1.100", 5555, false, System.currentTimeMillis()),
                        AdbDevice("192.168.1.101", 5555, false, System.currentTimeMillis() - 86400000)
                    )
                )
            }
        }
    }

    fun updateIp(ip: String) {
        _uiState.update { it.copy(ipAddress = ip) }
    }

    fun updatePort(port: String) {
        _uiState.update { it.copy(port = port) }
    }

    fun connect() {
        viewModelScope.launch {
            val ip = _uiState.value.ipAddress
            val port = _uiState.value.port
            
            if (ip.isBlank()) {
                _uiState.update { it.copy(connectionError = "IP Address cannot be empty") }
                return@launch
            }

            _uiState.update { it.copy(isConnecting = true, connectionError = null) }
            addLog("Connecting to $ip:$port...")

            try {
                // Simulate connection process
                delay(1500)
                addLog("Successfully connected to $ip:$port")
                _uiState.update { 
                    it.copy(isConnecting = false, isConnected = true) 
                }
            } catch (e: Exception) {
                addLog("Failed to connect: ${e.message}")
                _uiState.update { 
                    it.copy(isConnecting = false, connectionError = e.message ?: "Connection failed") 
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            val ip = _uiState.value.ipAddress
            val port = _uiState.value.port
            addLog("Disconnecting from $ip:$port...")
            delay(500)
            addLog("Disconnected")
            _uiState.update { it.copy(isConnected = false) }
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
