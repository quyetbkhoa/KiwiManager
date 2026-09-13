package com.kiwi.manager.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kiwi.manager.domain.model.ThemeMode
import com.kiwi.manager.ui.component.GlassBox
import com.kiwi.manager.ui.theme.GlassBorderGradient
import com.kiwi.manager.ui.theme.KiwiGradient
import com.kiwi.manager.ui.theme.KiwiNeon

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(themeMode) {
        if (uiState.currentTheme != themeMode) {
            viewModel.setTheme(themeMode)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                        text = "Cài Đặt & Giao Diện",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tùy chỉnh phong cách hiển thị và hệ sinh thái",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Appearance Section
                item {
                    Column {
                        Text(
                            text = "CHẾ ĐỘ GIAO DIỆN (THEME)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KiwiNeon,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ModernThemeCard(
                                title = "Sáng",
                                icon = "☀️",
                                isSelected = uiState.currentTheme == ThemeMode.LIGHT,
                                onClick = {
                                    viewModel.setTheme(ThemeMode.LIGHT)
                                    onThemeChange(ThemeMode.LIGHT)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ModernThemeCard(
                                title = "Tối",
                                icon = "🌙",
                                isSelected = uiState.currentTheme == ThemeMode.DARK,
                                onClick = {
                                    viewModel.setTheme(ThemeMode.DARK)
                                    onThemeChange(ThemeMode.DARK)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ModernThemeCard(
                                title = "OLED",
                                icon = "🖤",
                                isSelected = uiState.currentTheme == ThemeMode.OLED,
                                onClick = {
                                    viewModel.setTheme(ThemeMode.OLED)
                                    onThemeChange(ThemeMode.OLED)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // About & System Section
                item {
                    Column {
                        Text(
                            text = "THÔNG TIN HỆ SINH THÁI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        GlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SettingItemRow(
                                    icon = Icons.Default.Palette,
                                    title = "Phiên bản Kiwi Manager",
                                    subtitle = "v${uiState.appVersion} (Official Release)"
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.08f)
                                )
                                SettingItemRow(
                                    icon = Icons.Default.Watch,
                                    title = "Hỗ trợ Wear OS",
                                    subtitle = "Tương thích Wear OS 2.0+ & ColorOS Watch"
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.08f)
                                )
                                SettingItemRow(
                                    icon = Icons.Default.Code,
                                    title = "Mã nguồn mở GitHub",
                                    subtitle = "quyetbkhoa/KiwiManager",
                                    isClickable = true,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/quyetbkhoa/KiwiManager"))
                                        context.startActivity(intent)
                                    }
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
fun ModernThemeCard(
    title: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "theme_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .height(116.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) KiwiNeon.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
            .border(
                1.5.dp,
                if (isSelected) KiwiNeon else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) KiwiNeon else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = KiwiNeon,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isClickable) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KiwiNeon,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
        if (isClickable) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
