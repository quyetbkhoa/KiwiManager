package com.kiwi.manager.ui.screen.adbconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiwi.manager.data.adb.AdbTransportType
import com.kiwi.manager.data.adb.BluetoothBridgeMode
import com.kiwi.manager.data.adb.BluetoothDeviceInfo
import com.kiwi.manager.ui.component.GlassBox
import com.kiwi.manager.ui.component.TactilePillButton
import com.kiwi.manager.ui.theme.KiwiNeon
import kotlinx.coroutines.launch

@Composable
fun AdbConnectScreen(
    viewModel: AdbConnectViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWatchApps: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Permission launcher for Android 12+ (BLUETOOTH_CONNECT)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refreshPairedDevices()
        }
    }

    // Auto scroll terminal log
    LaunchedEffect(uiState.log.size) {
        if (uiState.log.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.log.size - 1)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 100.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Kết Nối ADB Đồng Hồ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Quản lý ứng dụng qua Wi-Fi hoặc Bluetooth",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Transport Mode Tabs (Wi-Fi vs Bluetooth)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab Wi-Fi
                val isWifiSelected = uiState.selectedTab == AdbTabMode.WIFI
                val wifiBgColor by animateColorAsState(
                    targetValue = if (isWifiSelected) KiwiNeon.copy(alpha = 0.2f) else Color.Transparent,
                    label = "wifi_tab_bg"
                )
                val wifiTextColor by animateColorAsState(
                    targetValue = if (isWifiSelected) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "wifi_tab_text"
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(wifiBgColor)
                        .selectable(
                            selected = isWifiSelected,
                            role = Role.RadioButton,
                            onClick = { viewModel.selectTab(AdbTabMode.WIFI) }
                        )
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = wifiTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Wi-Fi (Mạng LAN)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = wifiTextColor
                    )
                }

                // Tab Bluetooth
                val isBtSelected = uiState.selectedTab == AdbTabMode.BLUETOOTH
                val btBgColor by animateColorAsState(
                    targetValue = if (isBtSelected) KiwiNeon.copy(alpha = 0.2f) else Color.Transparent,
                    label = "bt_tab_bg"
                )
                val btTextColor by animateColorAsState(
                    targetValue = if (isBtSelected) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "bt_tab_text"
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(btBgColor)
                        .selectable(
                            selected = isBtSelected,
                            role = Role.RadioButton
                        ) {
                            // Check Bluetooth connect permission on Android 12+
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                }
                            }
                            viewModel.selectTab(AdbTabMode.BLUETOOTH)
                        }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = btTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bluetooth ⚡",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = btTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Connected Watch Quick Navigation Banner
                if (uiState.isConnected) {
                    item {
                        GlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KiwiNeon.copy(alpha = 0.12f),
                            borderBrush = Brush.horizontalGradient(listOf(KiwiNeon, Color(0xFF22C55E)))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(KiwiNeon.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (uiState.activeTransport == AdbTransportType.BLUETOOTH) Icons.Default.BluetoothConnected else Icons.Default.Watch,
                                                contentDescription = null,
                                                tint = KiwiNeon,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = uiState.deviceInfo?.let { "${it.manufacturer} ${it.model}".trim() } ?: "Đồng Hồ Đã Kết Nối",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (uiState.activeTransport == AdbTransportType.BLUETOOTH) "Kết nối qua Bluetooth ⚡" else "Kết nối qua Wi-Fi",
                                                fontSize = 11.sp,
                                                color = KiwiNeon
                                            )
                                        }
                                    }

                                    if (uiState.deviceInfo?.batteryLevel != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (uiState.deviceInfo?.isCharging == true) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                                contentDescription = null,
                                                tint = KiwiNeon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${uiState.deviceInfo?.batteryLevel}%",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = KiwiNeon
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                TactilePillButton(
                                    text = "Mở Quản Lý App Đồng Hồ",
                                    icon = Icons.Default.Watch,
                                    onClick = { onNavigateToWatchApps?.invoke() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Wi-Fi Mode Section
                if (uiState.selectedTab == AdbTabMode.WIFI) {
                    // Steps Guide Bento Card (Wi-Fi)
                    item {
                        GlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "HƯỚNG DẪN KẾT NỐI WI-FI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KiwiNeon,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                StepRow(number = "1", title = "Kết nối cùng Wi-Fi", desc = "Điện thoại và đồng hồ cùng vào 1 mạng Wi-Fi (hoặc phát Hotspot từ đt).")
                                Spacer(modifier = Modifier.height(8.dp))
                                StepRow(number = "2", title = "Bật ADB trên đồng hồ", desc = "Cài đặt > Tùy chọn nhà phát triển > Bật Gỡ lỗi qua Wi-Fi.")
                                Spacer(modifier = Modifier.height(8.dp))
                                StepRow(number = "3", title = "Nhập IP & Port", desc = "Nhập dãy IP:Port hiển thị trên màn hình đồng hồ vào ô bên dưới.")
                            }
                        }
                    }

                    // Connection Input Box (Wi-Fi)
                    item {
                        GlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.ipAddress,
                                        onValueChange = { viewModel.updateIp(it) },
                                        label = { Text("IP Đồng hồ", fontSize = 12.sp) },
                                        placeholder = { Text("192.168.x.x") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(2.5f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = KiwiNeon,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = uiState.port,
                                        onValueChange = { viewModel.updatePort(it) },
                                        label = { Text("Port", fontSize = 12.sp) },
                                        placeholder = { Text("5555") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1.5f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = KiwiNeon,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    )
                                }

                                if (uiState.connectionError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = uiState.connectionError ?: "",
                                        color = Color(0xFFFF5252),
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Action buttons & Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (uiState.isConnected) KiwiNeon else Color(0xFF90A4AE))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (uiState.isConnected) "Đã kết nối" else "Chưa kết nối",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (uiState.isConnected) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (uiState.isConnected) {
                                        TactilePillButton(
                                            text = "Ngắt kết nối",
                                            onClick = { viewModel.disconnect() },
                                            isPrimary = false
                                        )
                                    } else {
                                        TactilePillButton(
                                            text = if (uiState.isConnecting) "Đang nối..." else "Kết Nối Wi-Fi",
                                            onClick = { viewModel.connectWifi() },
                                            enabled = !uiState.isConnecting,
                                            isPrimary = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bluetooth Mode Section
                if (uiState.selectedTab == AdbTabMode.BLUETOOTH) {
                    // Steps Guide Bento Card (Bluetooth)
                    item {
                        GlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "HƯỚNG DẪN KẾT NỐI BLUETOOTH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KiwiNeon,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                StepRow(number = "1", title = "Ghép đôi Bluetooth", desc = "Bật Bluetooth và đảm bảo điện thoại đã ghép đôi với đồng hồ.")
                                Spacer(modifier = Modifier.height(8.dp))
                                StepRow(number = "2", title = "Bật cầu nối trên đồng hồ", desc = "Mở app Gemini Companion trên đồng hồ (hoặc bật quyền Trợ năng cho app).")
                                Spacer(modifier = Modifier.height(8.dp))
                                StepRow(number = "3", title = "Chọn thiết bị & Kết nối", desc = "Chọn tên đồng hồ trong danh sách phía dưới và bấm 'Kết Nối Bluetooth'.")
                            }
                        }
                    }

                    // Paired Bluetooth Devices List Box
                    item {
                        GlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "THIẾT BỊ BLUETOOTH ĐÃ GHÉP ĐÔI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KiwiNeon,
                                        letterSpacing = 1.sp
                                    )

                                    IconButton(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                val hasPermission = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.BLUETOOTH_CONNECT
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (!hasPermission) {
                                                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                                    return@IconButton
                                                }
                                            }
                                            viewModel.refreshPairedDevices()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Làm mới",
                                            tint = KiwiNeon,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (uiState.pairedBluetoothDevices.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Chưa có thiết bị Bluetooth đã ghép đôi.\nVui lòng vào Cài đặt máy để ghép đôi đồng hồ trước.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        uiState.pairedBluetoothDevices.forEach { device ->
                                            val isSelected = uiState.selectedBluetoothDevice?.address == device.address
                                            val itemBorderColor by animateColorAsState(
                                                targetValue = if (isSelected) KiwiNeon else Color.White.copy(alpha = 0.1f),
                                                label = "item_border"
                                            )
                                            val itemBgColor by animateColorAsState(
                                                targetValue = if (isSelected) KiwiNeon.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f),
                                                label = "item_bg"
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(itemBgColor)
                                                    .border(1.dp, itemBorderColor, RoundedCornerShape(14.dp))
                                                    .selectable(
                                                        selected = isSelected,
                                                        role = Role.RadioButton,
                                                        onClick = { viewModel.selectBluetoothDevice(device) }
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = if (device.isLikelyWatch) Icons.Default.Watch else Icons.Default.Bluetooth,
                                                        contentDescription = null,
                                                        tint = if (isSelected) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = device.name,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (device.isLikelyWatch) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .background(KiwiNeon.copy(alpha = 0.2f))
                                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Đồng hồ",
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = KiwiNeon
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = device.address,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }

                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (isSelected) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (uiState.connectionError != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = uiState.connectionError ?: "",
                                        color = Color(0xFFFF5252),
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Bluetooth Action Button & Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (uiState.isConnected) KiwiNeon else Color(0xFF90A4AE))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (uiState.isConnected) "Đã kết nối Bluetooth" else "Chưa kết nối",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (uiState.isConnected) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (uiState.isConnected) {
                                        TactilePillButton(
                                            text = "Ngắt kết nối",
                                            onClick = { viewModel.disconnect() },
                                            isPrimary = false
                                        )
                                    } else {
                                        TactilePillButton(
                                            text = if (uiState.isConnecting) "Đang kết nối..." else "Kết Nối Bluetooth",
                                            onClick = { viewModel.connectBluetooth() },
                                            enabled = !uiState.isConnecting && uiState.selectedBluetoothDevice != null,
                                            isPrimary = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Saved Devices chips
                if (uiState.savedDevices.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = "THIẾT BỊ ĐÃ LƯU",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.savedDevices) { device ->
                                    val isBt = device.port == 0 || device.host.contains(":")
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.07f))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                            .clickable { viewModel.selectSavedDevice(device) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isBt) Icons.Default.Bluetooth else Icons.Default.Wifi,
                                                contentDescription = null,
                                                tint = KiwiNeon,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBt) device.host else "${device.host}:${device.port}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Terminal Console Card
                item {
                    Column {
                        Text(
                            text = "TERMINAL LOG CONSOLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF090D09))
                                .border(1.dp, Color(0x3076FF03), RoundedCornerShape(20.dp))
                                .padding(12.dp)
                        ) {
                            if (uiState.log.isEmpty()) {
                                Text(
                                    text = "> Sẵn sàng nhận lệnh kết nối ADB (Wi-Fi hoặc Bluetooth)...",
                                    color = Color(0x8076FF03),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(uiState.log) { line ->
                                        Text(
                                            text = line,
                                            color = KiwiNeon,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(KiwiNeon.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KiwiNeon
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}
