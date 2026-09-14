package com.kiwi.manager.data.adb

import android.util.Log
import com.kiwi.manager.KiwiManagerApp
import com.kiwi.manager.domain.model.AdbDevice
import com.kiwi.manager.domain.model.WatchDeviceInfo
import dadb.AdbKeyPair
import dadb.AdbShellResponse
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed class AdbSessionState {
    data object Disconnected : AdbSessionState()
    data class Connecting(val host: String, val port: Int) : AdbSessionState()
    data class Connected(val device: AdbDevice, val info: WatchDeviceInfo) : AdbSessionState()
    data class Error(val message: String) : AdbSessionState()
}

object AdbConnectionManager {
    private const val TAG = "KiwiAdb"
    private var dadbInstance: Dadb? = null
    private var currentHost: String? = null
    private var currentPort: Int = 5555
    private var cachedKeyPair: AdbKeyPair? = null

    private val _sessionState = MutableStateFlow<AdbSessionState>(AdbSessionState.Disconnected)
    val sessionState: StateFlow<AdbSessionState> = _sessionState.asStateFlow()

    private val _deviceInfo = MutableStateFlow<WatchDeviceInfo?>(null)
    val deviceInfo: StateFlow<WatchDeviceInfo?> = _deviceInfo.asStateFlow()

    val isConnected: Boolean
        get() = dadbInstance != null && _sessionState.value is AdbSessionState.Connected

    fun getActiveDadb(): Dadb? = dadbInstance

    private fun getOrCreateKeyPair(): AdbKeyPair {
        cachedKeyPair?.let { return it }
        val context = KiwiManagerApp.instance
        val keyDir = File(context.filesDir, "adb")
        if (!keyDir.exists()) {
            keyDir.mkdirs()
        }
        val privKey = File(keyDir, "adbkey")
        val pubKey = File(keyDir, "adbkey.pub")
        if (!privKey.exists() || !pubKey.exists()) {
            AdbKeyPair.generate(privKey, pubKey)
        }
        val keyPair = AdbKeyPair.read(privKey, pubKey)
        cachedKeyPair = keyPair
        return keyPair
    }

    suspend fun connect(host: String, port: Int): Result<Dadb> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Bắt đầu kết nối ADB tới $host:$port...")
            _sessionState.value = AdbSessionState.Connecting(host, port)
            
            // Close any existing connection first
            disconnectInternal()

            val keyPair = getOrCreateKeyPair()
            val dadb = Dadb.create(host, port, keyPair)
            dadbInstance = dadb
            currentHost = host
            currentPort = port

            val info = queryDeviceInfo(dadb, host, port)
            _deviceInfo.value = info

            val device = AdbDevice(
                host = host,
                port = port,
                isConnected = true,
                lastConnected = System.currentTimeMillis()
            )
            _sessionState.value = AdbSessionState.Connected(device, info)
            Log.d(TAG, "✓ Đã kết nối ADB thành công tới $host:$port (Model: ${info.model})")

            Result.success(dadb)
        } catch (e: Exception) {
            disconnectInternal()
            val errMsg = e.localizedMessage ?: "Không thể kết nối tới $host:$port"
            Log.e(TAG, "✗ Kết nối ADB thất bại: $errMsg", e)
            _sessionState.value = AdbSessionState.Error(errMsg)
            Result.failure(e)
        }
    }

    suspend fun executeShell(command: String): Result<String> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext Result.failure(IllegalStateException("Chưa kết nối ADB tới thiết bị"))
        try {
            val response = dadb.shell(command)
            Result.success(response.output)
        } catch (e: Exception) {
            // Check if connection died
            checkConnectionAlive()
            Result.failure(e)
        }
    }

    suspend fun executeShellRaw(command: String): Result<AdbShellResponse> = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext Result.failure(IllegalStateException("Chưa kết nối ADB tới thiết bị"))
        try {
            val response = dadb.shell(command)
            Result.success(response)
        } catch (e: Exception) {
            checkConnectionAlive()
            Result.failure(e)
        }
    }

    private fun checkConnectionAlive() {
        try {
            dadbInstance?.shell("echo 1")
        } catch (e: Exception) {
            disconnectInternal()
            _sessionState.value = AdbSessionState.Disconnected
        }
    }

    private fun queryDeviceInfo(dadb: Dadb, host: String, port: Int): WatchDeviceInfo {
        return try {
            val model = dadb.shell("getprop ro.product.model").output.trim().ifEmpty { "Wear OS Device" }
            val manufacturer = dadb.shell("getprop ro.product.manufacturer").output.trim()
            val androidVer = dadb.shell("getprop ro.build.version.release").output.trim()

            // Query battery status
            val batteryOutput = dadb.shell("dumpsys battery").output
            var level: Int? = null
            var isCharging = false
            batteryOutput.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("level:")) {
                    level = trimmed.substringAfter("level:").trim().toIntOrNull()
                } else if (trimmed.startsWith("status:")) {
                    val status = trimmed.substringAfter("status:").trim().toIntOrNull()
                    // 2: CHARGING, 5: FULL
                    isCharging = (status == 2 || status == 5)
                }
            }

            WatchDeviceInfo(
                host = host,
                port = port,
                model = model,
                manufacturer = manufacturer,
                androidVersion = androidVer,
                batteryLevel = level,
                isCharging = isCharging
            )
        } catch (e: Exception) {
            WatchDeviceInfo(host = host, port = port)
        }
    }

    fun updateDeviceInfo(info: WatchDeviceInfo) {
        _deviceInfo.value = info
        val current = _sessionState.value
        if (current is AdbSessionState.Connected) {
            _sessionState.value = current.copy(info = info)
        }
    }

    suspend fun refreshDeviceInfo(): WatchDeviceInfo? = withContext(Dispatchers.IO) {
        val dadb = dadbInstance ?: return@withContext null
        val host = currentHost ?: return@withContext null
        val info = queryDeviceInfo(dadb, host, currentPort)
        _deviceInfo.value = info
        info
    }

    fun disconnect() {
        disconnectInternal()
        _sessionState.value = AdbSessionState.Disconnected
        _deviceInfo.value = null
    }

    private fun disconnectInternal() {
        try {
            dadbInstance?.close()
        } catch (e: Exception) {
            // Ignore
        } finally {
            dadbInstance = null
        }
    }
}
