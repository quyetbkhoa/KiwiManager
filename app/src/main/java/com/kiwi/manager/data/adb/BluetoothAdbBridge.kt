package com.kiwi.manager.data.adb

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

enum class BluetoothBridgeMode {
    DIRECT_RFCOMM,
    WEAR_OS_ADB_HUB
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val isLikelyWatch: Boolean
)

object BluetoothAdbBridge {
    private const val TAG = "BluetoothAdbBridge"
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val isRunning = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var localPort: Int = 0
    private var activeTcpSocket: Socket? = null
    private var activeBtSocket: BluetoothSocket? = null
    private var activeLocalSocket: LocalSocket? = null
    private var bridgeThread: Thread? = null

    val isBridgeActive: Boolean
        get() = isRunning.get() && serverSocket != null && !serverSocket!!.isClosed

    fun getLocalPort(): Int = localPort

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            val bonded = adapter.bondedDevices ?: emptySet()
            bonded.map { device ->
                val name = device.name ?: "Thiết bị không tên"
                val address = device.address
                val devClass = device.bluetoothClass?.majorDeviceClass ?: 0
                // 0x0700: WEARABLE
                val isLikelyWatch = devClass == 0x0700 ||
                        name.contains("Watch", ignoreCase = true) ||
                        name.contains("OPPO", ignoreCase = true) ||
                        name.contains("Wear", ignoreCase = true)

                BluetoothDeviceInfo(
                    name = name,
                    address = address,
                    isBonded = true,
                    isLikelyWatch = isLikelyWatch
                )
            }.sortedWith(compareByDescending<BluetoothDeviceInfo> { it.isLikelyWatch }.thenBy { it.name })
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy danh sách thiết bị Bluetooth đã ghép đôi: ${e.message}")
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun startBridge(
        deviceAddress: String,
        mode: BluetoothBridgeMode = BluetoothBridgeMode.DIRECT_RFCOMM
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            stopBridge()

            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (mode == BluetoothBridgeMode.DIRECT_RFCOMM) {
                if (adapter == null || !adapter.isEnabled) {
                    return@withContext Result.failure(IllegalStateException("Bluetooth trên điện thoại chưa được bật!"))
                }
            }

            // 1. Mở ServerSocket trên localhost với cổng ngẫu nhiên (ephemeral port)
            val srv = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
            serverSocket = srv
            localPort = srv.localPort
            isRunning.set(true)
            Log.d(TAG, "Đã mở ServerSocket trung gian tại 127.0.0.1:$localPort")

            if (mode == BluetoothBridgeMode.DIRECT_RFCOMM) {
                Log.d(TAG, "Đang kết nối Bluetooth RFCOMM tới $deviceAddress...")
                val device = adapter!!.getRemoteDevice(deviceAddress)
                // Ưu tiên insecure RFCOMM để bỏ qua popup mã PIN nếu đã ghép đôi
                val bt = try {
                    device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                } catch (_: Exception) {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                }

                try {
                    bt.connect()
                    activeBtSocket = bt
                    Log.d(TAG, "✓ Đã kết nối Bluetooth RFCOMM thành công tới $deviceAddress!")
                } catch (e: Exception) {
                    stopBridge()
                    return@withContext Result.failure(
                        Exception("Không thể kết nối Bluetooth tới đồng hồ ($deviceAddress). Hãy đảm bảo đồng hồ đang bật Bluetooth và app đồng hồ đã mở hoặc đã bật quyền Trợ năng.\nChi tiết: ${e.message}")
                    )
                }

                // 2. Chạy luồng chờ Dadb kết nối tới ServerSocket
                bridgeThread = Thread({
                    val isSessionAlive = AtomicBoolean(true)
                    try {
                        Log.d(TAG, "Đang chờ dadb kết nối vào 127.0.0.1:$localPort...")
                        val tcp = srv.accept()
                        activeTcpSocket = tcp
                        Log.d(TAG, "✓ dadb đã kết nối tới cầu nối TCP nội bộ! Bắt đầu truyền dữ liệu 2 chiều...")

                        val tcpIn = tcp.getInputStream()
                        val tcpOut = tcp.getOutputStream()
                        val btIn = bt.inputStream
                        val btOut = bt.outputStream

                        val t1 = Thread({
                            pipeCoordinated(tcpIn, btOut, "DADB->BT", isSessionAlive, tcp, bt)
                        }, "Kiwi-Pipe-TCP-BT")

                        val t2 = Thread({
                            pipeCoordinated(btIn, tcpOut, "BT->DADB", isSessionAlive, tcp, bt)
                        }, "Kiwi-Pipe-BT-TCP")

                        t1.start()
                        t2.start()

                        t1.join()
                        t2.join()
                    } catch (e: Exception) {
                        Log.d(TAG, "Phiên cầu nối Bluetooth kết thúc: ${e.message}")
                    } finally {
                        stopBridge()
                    }
                }, "Kiwi-BT-Bridge-Worker").apply {
                    isDaemon = true
                    start()
                }

            } else {
                // Mode: WEAR_OS_ADB_HUB
                Log.d(TAG, "Đang kết nối tới socket localabstract:adb-hub của Wear OS companion app...")
                val localSock = LocalSocket()
                try {
                    localSock.connect(LocalSocketAddress("adb-hub", LocalSocketAddress.Namespace.ABSTRACT))
                    activeLocalSocket = localSock
                    Log.d(TAG, "✓ Đã kết nối tới localabstract:adb-hub!")
                } catch (e: Exception) {
                    stopBridge()
                    return@withContext Result.failure(
                        Exception("Không tìm thấy socket adb-hub của Wear OS. Hãy đảm bảo ứng dụng Wear OS trên điện thoại đã bật 'Gỡ lỗi qua Bluetooth'.")
                    )
                }

                bridgeThread = Thread({
                    val isSessionAlive = AtomicBoolean(true)
                    try {
                        val tcp = srv.accept()
                        activeTcpSocket = tcp
                        val tcpIn = tcp.getInputStream()
                        val tcpOut = tcp.getOutputStream()
                        val hubIn = localSock.inputStream
                        val hubOut = localSock.outputStream

                        val t1 = Thread({
                            pipeCoordinated(tcpIn, hubOut, "DADB->HUB", isSessionAlive, tcp, localSock)
                        }, "Kiwi-Pipe-TCP-HUB")

                        val t2 = Thread({
                            pipeCoordinated(hubIn, tcpOut, "HUB->DADB", isSessionAlive, tcp, localSock)
                        }, "Kiwi-Pipe-HUB-TCP")

                        t1.start()
                        t2.start()

                        t1.join()
                        t2.join()
                    } catch (e: Exception) {
                        Log.d(TAG, "Phiên adb-hub kết thúc: ${e.message}")
                    } finally {
                        stopBridge()
                    }
                }, "Kiwi-Hub-Bridge-Worker").apply {
                    isDaemon = true
                    start()
                }
            }

            Result.success(localPort)
        } catch (e: Exception) {
            stopBridge()
            Result.failure(e)
        }
    }

    fun stopBridge() {
        if (isRunning.compareAndSet(true, false)) {
            Log.d(TAG, "Đang đóng cầu nối Bluetooth...")
            try {
                serverSocket?.close()
            } catch (_: Exception) {}
            serverSocket = null

            try {
                activeTcpSocket?.close()
            } catch (_: Exception) {}
            activeTcpSocket = null

            try {
                activeBtSocket?.close()
            } catch (_: Exception) {}
            activeBtSocket = null

            try {
                activeLocalSocket?.close()
            } catch (_: Exception) {}
            activeLocalSocket = null

            bridgeThread?.interrupt()
            bridgeThread = null
            Log.d(TAG, "Đã đóng toàn bộ cầu nối Bluetooth ADB.")
        }
    }

    private fun pipeCoordinated(
        input: InputStream,
        output: OutputStream,
        name: String,
        isSessionAlive: AtomicBoolean,
        sockA: Closeable,
        sockB: Closeable
    ) {
        val buffer = ByteArray(8192)
        try {
            while (isRunning.get() && isSessionAlive.get()) {
                val bytesRead = input.read(buffer)
                if (bytesRead == -1) break
                output.write(buffer, 0, bytesRead)
                output.flush()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Pipe $name: ${e.message}")
        } finally {
            isSessionAlive.set(false)
            try { output.close() } catch (_: Exception) {}
            try { input.close() } catch (_: Exception) {}
            try { sockA.close() } catch (_: Exception) {}
            try { sockB.close() } catch (_: Exception) {}
        }
    }
}
