package com.kiwi.manager.ui.screen.appdetail

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiwi.manager.domain.model.AppDisplayInfo
import com.kiwi.manager.domain.model.DownloadProgress
import com.kiwi.manager.domain.model.InstallStatus
import com.kiwi.manager.ui.component.GlassBox
import com.kiwi.manager.ui.component.StatusBadge
import com.kiwi.manager.ui.component.TactilePillButton
import com.kiwi.manager.ui.theme.GlassBorderGradient
import com.kiwi.manager.ui.theme.KiwiGradient
import com.kiwi.manager.ui.theme.KiwiNeon
import kotlinx.coroutines.launch

@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAdbConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp)
        ) {
            // Header Bar
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

                Text(
                    text = uiState.app?.app?.name ?: "Chi tiết ứng dụng",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KiwiNeon, modifier = Modifier.size(36.dp))
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error ?: "", color = Color(0xFFFF5252))
                }
            } else if (uiState.app != null) {
                AppDetailContent(
                    appDisplayInfo = uiState.app!!,
                    uiState = uiState,
                    onInstallPhone = { viewModel.installPhone() },
                    onInstallWatch = { viewModel.installWatch() },
                    onNavigateToAdbConnect = onNavigateToAdbConnect
                )
            }
        }
    }
}

@Composable
fun AppDetailContent(
    appDisplayInfo: AppDisplayInfo,
    uiState: AppDetailUiState,
    onInstallPhone: () -> Unit,
    onInstallWatch: () -> Unit,
    onNavigateToAdbConnect: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.installLog.size) {
        if (uiState.installLog.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.installLog.size - 1)
            }
        }
    }

    val (appEmoji, iconGradient) = when (appDisplayInfo.app.id) {
        "kiwi_manager" -> "🥝" to Brush.linearGradient(listOf(Color(0xFF76FF03), Color(0xFF388E3C)))
        "gemini_wear" -> "🤖" to Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF7E57C2)))
        "open_navigation" -> "🗺️" to Brush.linearGradient(listOf(Color(0xFFFFB74D), Color(0xFFF4511E)))
        "custom_vibration" -> "📳" to Brush.linearGradient(listOf(Color(0xFFEC407A), Color(0xFFAB47BC)))
        else -> "📦" to Brush.linearGradient(listOf(Color(0xFF78909C), Color(0xFF37474F)))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Hero Banner Card
        item {
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(iconGradient)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = appEmoji, fontSize = 34.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = appDisplayInfo.app.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = appDisplayInfo.app.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Phone Action Widget
        if (appDisplayInfo.app.phone != null) {
            item {
                BentoPlatformActionWidget(
                    title = "📱 Phiên Bản Mobile Companion",
                    packageName = appDisplayInfo.app.phone.packageName,
                    installedVersion = appDisplayInfo.phoneInstalled?.versionName,
                    latestVersion = appDisplayInfo.app.phone.versionName,
                    status = appDisplayInfo.phoneStatus,
                    isInstalling = uiState.phoneInstalling,
                    downloadProgress = uiState.phoneDownloadProgress,
                    onActionClick = onInstallPhone
                )
            }
        }

        // Watch Action Widget
        if (appDisplayInfo.app.watch != null) {
            item {
                BentoPlatformActionWidget(
                    title = "⌚ Phiên Bản Wear OS Watch",
                    packageName = appDisplayInfo.app.watch.packageName,
                    installedVersion = appDisplayInfo.watchInstalled?.versionName,
                    latestVersion = appDisplayInfo.app.watch.versionName,
                    status = appDisplayInfo.watchStatus,
                    isInstalling = uiState.watchInstalling,
                    downloadProgress = uiState.watchDownloadProgress,
                    onActionClick = onInstallWatch,
                    extraFooter = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = "Yêu cầu kết nối Wireless ADB",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Mở cài đặt ADB →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KiwiNeon,
                                modifier = Modifier.clickable { onNavigateToAdbConnect() }
                            )
                        }
                    }
                )
            }
        }

        // Install Log Console
        if (uiState.installLog.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "NHẬT KÝ TIẾN TRÌNH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF090D09))
                            .border(1.dp, Color(0x3076FF03), RoundedCornerShape(20.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.installLog) { line ->
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

@Composable
fun BentoPlatformActionWidget(
    title: String,
    packageName: String,
    installedVersion: String?,
    latestVersion: String,
    status: InstallStatus,
    isInstalling: Boolean,
    downloadProgress: DownloadProgress?,
    onActionClick: () -> Unit,
    extraFooter: @Composable (() -> Unit)? = null
) {
    GlassBox(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(status = status, latestVersion = latestVersion)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = packageName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Version specs row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Đang cài: ${installedVersion ?: "Chưa cài đặt"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Bản phát hành: v$latestVersion",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KiwiNeon
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action or Progress
            if (isInstalling) {
                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.percent / 100f },
                        color = KiwiNeon,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Đang tải: ${downloadProgress.percent}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KiwiNeon
                        )
                        Text(
                            text = "${downloadProgress.downloadedMb} / ${downloadProgress.totalMb} MB",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LinearProgressIndicator(
                        color = KiwiNeon,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Đang chuẩn bị gói cài đặt...",
                        fontSize = 11.sp,
                        color = KiwiNeon
                    )
                }
            } else {
                val buttonText = when (status) {
                    InstallStatus.NOT_INSTALLED -> "Tải & Cài Đặt Ngay"
                    InstallStatus.UPDATE_AVAILABLE -> "Nâng Cấp Lên v$latestVersion"
                    InstallStatus.UP_TO_DATE -> "Cài Đè Lại Bản Này"
                    InstallStatus.UNKNOWN -> "Cài Đặt"
                }

                TactilePillButton(
                    text = buttonText,
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = true
                )
            }

            extraFooter?.invoke()
        }
    }
}
