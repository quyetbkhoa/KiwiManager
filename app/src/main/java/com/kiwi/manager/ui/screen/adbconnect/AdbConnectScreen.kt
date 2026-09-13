package com.kiwi.manager.ui.screen.adbconnect

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiwi.manager.ui.component.GlassBox
import com.kiwi.manager.ui.component.TactilePillButton
import com.kiwi.manager.ui.theme.GlassBorderGradient
import com.kiwi.manager.ui.theme.KiwiGradient
import com.kiwi.manager.ui.theme.KiwiNeon
import kotlinx.coroutines.launch

@Composable
fun AdbConnectScreen(
    viewModel: AdbConnectViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll log
    LaunchedEffect(uiState.log.size) {
        if (uiState.log.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.log.size - 1)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 100.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
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

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Wireless ADB",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cài đặt ứng dụng không dây lên Wear OS",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Steps Guide Bento Card
                item {
                    GlassBox(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "HƯỚNG DẪN 3 BƯỚC",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KiwiNeon,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            StepRow(number = "1", title = "Kết nối Wi-Fi", desc = "Điện thoại và đồng hồ cùng vào 1 mạng Wi-Fi (hoặc phát Hotspot từ đt).")
                            Spacer(modifier = Modifier.height(8.dp))
                            StepRow(number = "2", title = "Bật ADB trên đồng hồ", desc = "Vào Cài đặt > Tùy chọn nhà phát triển > Bật Gỡ lỗi qua Wi-Fi.")
                            Spacer(modifier = Modifier.height(8.dp))
                            StepRow(number = "3", title = "Nhập IP & Port", desc = "Nhập dãy IP:Port hiển thị trên màn hình đồng hồ vào ô dưới đây.")
                        }
                    }
                }

                // Connection Input Bento Box
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
                                // Status Pill
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
                                        text = if (uiState.isConnecting) "Đang nối..." else "Kết Nối ADB",
                                        onClick = { viewModel.connect() },
                                        enabled = !uiState.isConnecting,
                                        isPrimary = true
                                    )
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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White.copy(alpha = 0.07f))
                                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                            .clickable { viewModel.selectSavedDevice(device) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "${device.host}:${device.port}",
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
                                    text = "> Sẵn sàng nhận lệnh kết nối ADB...",
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
