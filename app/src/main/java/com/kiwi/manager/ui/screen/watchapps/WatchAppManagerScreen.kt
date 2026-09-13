package com.kiwi.manager.ui.screen.watchapps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiwi.manager.domain.model.WatchAppInfo
import com.kiwi.manager.domain.model.WatchDeviceInfo
import com.kiwi.manager.ui.component.GlassBox
import com.kiwi.manager.ui.component.TactilePillButton
import com.kiwi.manager.ui.theme.GlassBorderGradient
import com.kiwi.manager.ui.theme.KiwiGradient
import com.kiwi.manager.ui.theme.KiwiNeon

@Composable
fun WatchAppManagerScreen(
    viewModel: WatchAppViewModel,
    onNavigateToAdbConnect: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 96.dp)
        ) {
            // Header Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "App Trên Đồng Hồ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uiState.isConnected) {
                            uiState.deviceInfo?.let { "${it.manufacturer} ${it.model}" } ?: "Đã kết nối qua ADB"
                        } else {
                            "Chưa kết nối ADB"
                        },
                        fontSize = 12.sp,
                        color = if (uiState.isConnected) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.isConnected) {
                    IconButton(
                        onClick = { viewModel.loadWatchApps() },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = KiwiNeon,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Làm mới",
                                tint = KiwiNeon
                            )
                        }
                    }
                }
            }

            if (!uiState.isConnected) {
                // Not Connected Card
                WatchDisconnectedCard(
                    isConnecting = uiState.isConnecting,
                    onConnectLast = { viewModel.connectToLastDevice() },
                    onNavigateToAdb = onNavigateToAdbConnect
                )
            } else {
                // Device Quick Status Card
                uiState.deviceInfo?.let { info ->
                    WatchDeviceStatusCard(info = info, uiState = uiState)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Search Box
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    placeholder = { Text("Tìm kiếm app hoặc package...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Xóa tìm kiếm",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KiwiNeon,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Filter Tabs Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(WatchAppFilter.entries) { filter ->
                        val isSelected = uiState.activeFilter == filter
                        val count = when (filter) {
                            WatchAppFilter.USER -> uiState.userAppCount
                            WatchAppFilter.SYSTEM -> uiState.systemAppCount
                            WatchAppFilter.RUNNING -> uiState.runningAppCount
                            WatchAppFilter.ALL -> uiState.apps.size
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(filter) },
                            label = {
                                Text(
                                    text = "${filter.label} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KiwiNeon.copy(alpha = 0.2f),
                                selectedLabelColor = KiwiNeon,
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) KiwiNeon else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Apps List or Loading
                if (uiState.isLoading && uiState.apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = KiwiNeon)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Đang quét ứng dụng trên đồng hồ qua ADB...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (uiState.filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "Không tìm thấy app nào phù hợp" else "Không có ứng dụng nào trong danh mục này",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.filteredApps,
                            key = { it.packageName }
                        ) { app ->
                            WatchAppCardItem(
                                app = app,
                                isActionInProgress = uiState.isActionInProgress && uiState.currentActionPackage == app.packageName,
                                onForceStop = { viewModel.forceStopApp(app) },
                                onLaunch = { viewModel.launchApp(app) },
                                onClearData = { viewModel.clearAppData(app) },
                                onToggleDisable = { viewModel.toggleAppDisabled(app) },
                                onUninstall = { viewModel.promptUninstall(app) },
                                onViewDetails = { viewModel.viewAppDetails(app) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Feedback Banner (SnackBar)
        AnimatedVisibility(
            visible = uiState.actionFeedbackMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            uiState.actionFeedbackMessage?.let { msg ->
                GlassBox(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (msg.startsWith("✓")) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (msg.startsWith("✓")) KiwiNeon else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearFeedbackMessage() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Uninstall Confirmation Dialog
        uiState.appToUninstall?.let { app ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissUninstallPrompt() },
                title = {
                    Text(
                        text = if (app.isSystemApp) "Gỡ ứng dụng hệ thống?" else "Gỡ cài đặt ứng dụng?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text("Bạn có chắc chắn muốn gỡ bỏ \"${app.appName}\"?")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.packageName,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (app.isSystemApp) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Cảnh báo: Đây là ứng dụng hệ thống! Lệnh gỡ bỏ an toàn (user 0) sẽ được thực thi. Bạn có thể khôi phục lại qua lệnh pm install-existing nếu cần.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmUninstall() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Gỡ Cài Đặt", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUninstallPrompt() }) {
                        Text("Hủy")
                    }
                }
            )
        }

        // App Details Dialog
        uiState.selectedAppForDetails?.let { app ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissAppDetails() },
                title = {
                    Column {
                        Text(text = app.appName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = app.packageName,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (uiState.isLoadingDetails) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = KiwiNeon)
                            }
                        } else {
                            Column {
                                if (app.apkPath != null) {
                                    Text("Đường dẫn APK:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(
                                        text = app.apkPath,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Text("Thông tin kỹ thuật (dumpsys):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.selectedAppDumpsys ?: "Không có thông tin chi tiết",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissAppDetails() }) {
                        Text("Đóng")
                    }
                }
            )
        }
    }
}

@Composable
fun WatchDisconnectedCard(
    isConnecting: Boolean,
    onConnectLast: () -> Unit,
    onNavigateToAdb: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(KiwiNeon.copy(alpha = 0.15f))
                        .border(1.dp, KiwiNeon.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = KiwiNeon,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Chưa Kết Nối Đồng Hồ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Kết nối Wireless ADB tới Smartwatch để quản lý ứng dụng, tắt app ngầm, xem app hệ thống và dọn dẹp bộ nhớ.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                TactilePillButton(
                    text = if (isConnecting) "Đang kết nối lại..." else "Kết nối thiết bị gần nhất",
                    icon = Icons.Default.Cable,
                    onClick = onConnectLast,
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onNavigateToAdb,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mở cài đặt Wireless ADB", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun WatchDeviceStatusCard(
    info: WatchDeviceInfo,
    uiState: WatchAppUiState
) {
    GlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = KiwiNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${info.manufacturer} ${info.model}".trim(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (info.batteryLevel != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (info.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = "Pin",
                            tint = if (info.isCharging) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${info.batteryLevel}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (info.isCharging) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ADB: ${info.host}:${info.port}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Tổng: ${uiState.apps.size} | Đang chạy: ${uiState.runningAppCount}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = KiwiNeon
                )
            }
        }
    }
}

@Composable
fun WatchAppCardItem(
    app: WatchAppInfo,
    isActionInProgress: Boolean,
    onForceStop: () -> Unit,
    onLaunch: () -> Unit,
    onClearData: () -> Unit,
    onToggleDisable: () -> Unit,
    onUninstall: () -> Unit,
    onViewDetails: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    GlassBox(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon Avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (app.isRunning) KiwiNeon.copy(alpha = 0.2f)
                            else if (app.isSystemApp) Color.White.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.firstOrNull()?.uppercase() ?: "A",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (app.isRunning) KiwiNeon else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // App Title & Package
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (app.isRunning) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(KiwiNeon)
                            )
                        }
                    }

                    Text(
                        text = app.packageName,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badges
                if (app.isSystemApp) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Hệ thống", fontSize = 10.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Medium)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(KiwiNeon.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Người dùng", fontSize = 10.sp, color = KiwiNeon, fontWeight = FontWeight.Medium)
                    }
                }

                // More Menu Button
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Tác vụ",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Xem chi tiết", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showMenu = false
                                onViewDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Xóa dữ liệu (Clear Data)", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.CleaningServices, null, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showMenu = false
                                onClearData()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (app.isEnabled) "Vô hiệu hóa app" else "Kích hoạt app",
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (app.isEnabled) Icons.Default.Block else Icons.Default.Check,
                                    null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleDisable()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Gỡ cài đặt",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onUninstall()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: Tắt App (Force Stop) & Mở App (Launch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = KiwiNeon
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang xử lý...", fontSize = 12.sp, color = KiwiNeon)
                } else {
                    // Nút Tắt App (Buộc dừng)
                    FilledTonalButton(
                        onClick = onForceStop,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                            contentColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tắt App", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Nút Mở App
                    FilledTonalButton(
                        onClick = onLaunch,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = KiwiNeon.copy(alpha = 0.18f),
                            contentColor = KiwiNeon
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mở App", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
