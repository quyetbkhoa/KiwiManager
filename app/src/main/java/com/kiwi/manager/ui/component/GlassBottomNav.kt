package com.kiwi.manager.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiwi.manager.ui.theme.KiwiGlow
import com.kiwi.manager.ui.theme.KiwiNeon

enum class NavTab(val title: String, val icon: ImageVector, val route: String) {
    HOME("Kho App", Icons.Default.GridView, "home"),
    WATCH_APPS("App Watch", Icons.Default.Watch, "watch_apps"),
    ADB("Wireless ADB", Icons.Default.Cable, "adb_connect"),
    SETTINGS("Cài Đặt", Icons.Default.Settings, "settings")
}

@Composable
fun GlassBottomNav(
    currentRoute: String,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // Glass specular highlight border gradient
    val dockBorderGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.08f),
            KiwiNeon.copy(alpha = 0.20f)
        )
    )

    // Inner subtle sheen
    val innerSheen = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.06f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Frosted Glass Dock Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .drawBehind {
                    // Soft neon ambient underglow
                    drawCircle(
                        color = KiwiGlow.copy(alpha = 0.25f),
                        radius = size.width * 0.25f
                    )
                }
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                .background(innerSheen)
                .border(1.2.dp, dockBorderGradient, RoundedCornerShape(32.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = currentRoute.startsWith(tab.route)

                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "tab_scale_${tab.name}"
                    )

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "icon_tint_${tab.name}"
                    )

                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) KiwiNeon else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "label_color_${tab.name}"
                    )

                    val pillBackground by animateColorAsState(
                        targetValue = if (isSelected) KiwiNeon.copy(alpha = 0.16f) else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "pill_bg_${tab.name}"
                    )

                    val pillBorderColor by animateColorAsState(
                        targetValue = if (isSelected) KiwiNeon.copy(alpha = 0.35f) else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "pill_border_${tab.name}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .scale(animatedScale)
                                .clip(RoundedCornerShape(18.dp))
                                .background(pillBackground)
                                .border(1.dp, pillBorderColor, RoundedCornerShape(18.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = iconTint,
                                    modifier = Modifier.size(22.dp)
                                )

                                // Subtle indicator dot for active tab
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 3.dp, y = (-2).dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(KiwiNeon)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = tab.title,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = labelColor,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
