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
import kotlinx.coroutines.withTimeout
import java.io.File

enum class AdbTransportType {
    WIFI,
    BLUETOOTH
}

sealed class AdbSessionState {
    data object Disconnected : AdbSessionState()
    data class Connecting(
        val host: String,
        val port: Int = 0,
        val transport: AdbTransportType = AdbTransportType.WIFI
    ) : AdbSessionState()
    data class Connected(
        val device: AdbDevice,
        val info: WatchDeviceInfo,
        val transport: AdbTransportType = AdbTransportType.WIFI
    ) : AdbSessionState()
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

    private var currentTransport: AdbTransportType = AdbTransportType.WIFI
    val transportType: AdbTransportType
        get() = currentTransport

    suspend fun connect(host: String, port: Int): Result<Dadb> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Bắt đầu kết nối ADB tới $host:$port...")
            _sessionState.value = AdbSessionState.Connecting(host, port, AdbTransportType.WIFI)
            
            // Close any existing connection first
            disconnectInternal()

            val keyPair = getOrCreateKeyPair()
            val dadb = Dadb.create(host, port, keyPair)
            dadbInstance = dadb
            currentHost = host
            currentPort = port
            currentTransport = AdbTransportType.WIFI

            val info = queryDeviceInfo(dadb, host, port)
            _deviceInfo.value = info

            val device = AdbDevice(
                host = host,
                port = port,
                isConnected = true,
                lastConnected = System.currentTimeMillis()
            )
            _sessionState.value = AdbSessionState.Connected(device, info, AdbTransportType.WIFI)
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

    suspend fun connectBluetooth(
        deviceAddress: String,
        deviceName: String = "",
        mode: BluetoothBridgeMode = BluetoothBridgeMode.DIRECT_RFCOMM
    ): Result<Dadb> = withContext(Dispatchers.IO) {
        try {
            val displayName = deviceName.ifBlank { deviceAddress }
            Log.d(TAG, "Bắt đầu kết nối Bluetooth ADB tới $displayName ($deviceAddress)...")
            _sessionState.value = AdbSessionState.Connecting(displayName, 0, AdbTransportType.BLUETOOTH)

            disconnectInternal()

            // 1. Khởi động cầu nối Bluetooth
            val bridgeRes = BluetoothAdbBridge.startBridge(deviceAddress, mode)
            val localPort = bridgeRes.getOrThrow()

            // 2. Kết nối dadb tới local TCP port
            val keyPair = getOrCreateKeyPair()
            val dadb = Dadb.create("127.0.0.1", localPort, keyPair)
            dadbInstance = dadb
            currentHost = "BT: $displayName"
            currentPort = localPort
            currentTransport = AdbTransportType.BLUETOOTH

            // 3. Xác thực kết nối ADB thực tế bằng lệnh shell
            Log.d(TAG, "Đang gửi lệnh xác thực ADB tới đồng hồ qua Bluetooth...")
            val info = try {
                withTimeout(15_000L) {
                    queryDeviceInfo(dadb, currentHost ?: displayName, localPort)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xác thực ADB qua Bluetooth: ${e.message}", e)
                throw e
            }
            _deviceInfo.value = info

            val device = AdbDevice(
                host = deviceAddress,
                port = localPort,
                isConnected = true,
                lastConnected = System.currentTimeMillis()
            )
            _sessionState.value = AdbSessionState.Connected(device, info, AdbTransportType.BLUETOOTH)
            Log.d(TAG, "✓ Đã kết nối Bluetooth ADB thành công tới $displayName (Model: ${info.model})")

            Result.success(dadb)
        } catch (e: Exception) {
            disconnectInternal()
            val rawMsg = e.localizedMessage ?: ""
            val cleanMsg = when {
                rawMsg.contains("unauthorized", ignoreCase = true) || rawMsg.contains("auth", ignoreCase = true) ->
                    "Đồng hồ chưa được ủy quyền! Vui lòng mở sáng màn hình đồng hồ, tích vào 'Luôn cho phép' và bấm OK."
                rawMsg.contains("refused", ignoreCase = true) ->
                    "Cổng ADB trên đồng hồ chưa mở. Hãy đảm bảo app Gemini trên đồng hồ đang chạy."
                else -> e.localizedMessage ?: "Không thể kết nối Bluetooth tới $deviceAddress"
            }
            Log.e(TAG, "✗ Kết nối Bluetooth ADB thất bại: $cleanMsg", e)
            _sessionState.value = AdbSessionState.Error(cleanMsg)
            Result.failure(Exception(cleanMsg, e))
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
        try {
            BluetoothAdbBridge.stopBridge()
        } catch (_: Exception) {}
    }
}
