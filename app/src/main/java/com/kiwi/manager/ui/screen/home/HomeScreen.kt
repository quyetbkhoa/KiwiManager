package com.kiwi.manager.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiwi.manager.data.manager.InstallManager
import com.kiwi.manager.domain.model.InstallStatus
import com.kiwi.manager.ui.component.AppCard
import com.kiwi.manager.ui.component.FloatingHeader
import com.kiwi.manager.ui.component.TactilePillButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kiwi.manager.ui.theme.KiwiNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAppDetail: (String) -> Unit,
    onNavigateToAdbConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installTasks by InstallManager.tasks.collectAsStateWithLifecycle()
    val activeTasks = remember(installTasks) {
        installTasks.values.filter { it.isInstalling }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadLocalStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Calculate update stats
    val updatesCount = uiState.apps.count { 
        it.phoneStatus == InstallStatus.UPDATE_AVAILABLE || it.watchStatus == InstallStatus.UPDATE_AVAILABLE 
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Floating Pill Header
            FloatingHeader(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshCatalog() },
                isWatchConnected = uiState.isWatchConnected,
                onAdbClick = onNavigateToAdbConnect
            )

            // Active Background Installation Banner(s)
            activeTasks.forEach { task ->
                val progressPercent = task.downloadProgress?.percent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(KiwiNeon.copy(alpha = 0.12f))
                        .border(1.dp, KiwiNeon.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable { onNavigateToAppDetail(task.appId) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (progressPercent != null && progressPercent in 1..99) {
                                CircularProgressIndicator(
                                    progress = { progressPercent / 100f },
                                    modifier = Modifier.size(20.dp),
                                    color = KiwiNeon,
                                    strokeWidth = 2.5.dp,
                                    trackColor = KiwiNeon.copy(alpha = 0.2f)
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = KiwiNeon,
                                    strokeWidth = 2.5.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Đang cài đặt ${task.appName} (${if (task.isWatch) "Wear OS" else "Mobile"})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KiwiNeon
                                )
                                Text(
                                    text = if (progressPercent != null && progressPercent in 1..99) "Đang tải $progressPercent% · Chạm để xem" else "${task.statusMessage} · Chạm để xem",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = KiwiNeon,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Offline Banner if applicable
            if (uiState.isOffline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE65100).copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chế độ Ngoại tuyến · Đang dùng dữ liệu bộ nhớ đệm",
                            fontSize = 12.sp,
                            color = Color(0xFFFFB74D),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            val pullRefreshState = rememberPullToRefreshState()

            // Main Content with Pull-To-Refresh
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshCatalog() },
                state = pullRefreshState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullRefreshState,
                        isRefreshing = uiState.isRefreshing,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = KiwiNeon,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            ) {
                if (uiState.isLoading && uiState.apps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = KiwiNeon,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else if (uiState.error != null && uiState.apps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Không thể tải kho ứng dụng",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.error ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TactilePillButton(
                                text = "Thử lại",
                                onClick = { viewModel.onRetry() }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 10.dp,
                            bottom = 110.dp // Spacing for floating bottom bar
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Summary Widget
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "KHO ỨNG DỤNG (${uiState.apps.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                if (updatesCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFF9100).copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$updatesCount bản cập nhật",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFB74D)
                                        )
                                    }
                                }
                            }
                        }

                        // App Bento Cards
                        items(uiState.apps, key = { it.app.id }) { appDisplayInfo ->
                            AppCard(
                                appDisplayInfo = appDisplayInfo,
                                onClick = { onNavigateToAppDetail(appDisplayInfo.app.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
