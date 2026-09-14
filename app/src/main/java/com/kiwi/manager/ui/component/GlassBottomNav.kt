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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiwi.manager.ui.theme.KiwiNeon

enum class NavTab(val title: String, val icon: ImageVector, val route: String) {
    HOME("Kho App", Icons.Default.GridView, "home"),
    WATCH_APPS("Đồng Hồ", Icons.Default.Watch, "watch_apps"),
    ADB("ADB", Icons.Default.Cable, "adb_connect"),
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
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.06f),
            KiwiNeon.copy(alpha = 0.18f)
        )
    )

    // Inner subtle sheen
    val innerSheen = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.05f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Frosted Glass Dock Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                    spotColor = KiwiNeon.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .background(innerSheen)
                .border(1.dp, dockBorderGradient, RoundedCornerShape(28.dp))
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
                        targetValue = if (isSelected) 1.06f else 1f,
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
                        targetValue = if (isSelected) KiwiNeon.copy(alpha = 0.14f) else Color.Transparent,
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
                            .clip(RoundedCornerShape(20.dp))
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(pillBackground)
                                .border(1.dp, pillBorderColor, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
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
                                            .offset(x = 3.dp, y = (-1).dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(KiwiNeon)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = labelColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
