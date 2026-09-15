package com.kiwi.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kiwi.manager.domain.model.AppDisplayInfo
import com.kiwi.manager.domain.model.InstallStatus
import com.kiwi.manager.ui.theme.KiwiSize
import com.kiwi.manager.ui.theme.KiwiSpacing

@Composable
fun AppCard(
    appDisplayInfo: AppDisplayInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (appSymbol, iconGradient) = appVisual(appDisplayInfo.app.id)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KiwiSpacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(KiwiSize.appIcon)
                        .clip(MaterialTheme.shapes.medium)
                        .background(iconGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = appSymbol, style = MaterialTheme.typography.headlineSmall)
                }

                Spacer(Modifier.width(KiwiSpacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = appDisplayInfo.app.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (appDisplayInfo.app.isManager) {
                            Spacer(Modifier.width(KiwiSpacing.xs))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "TRUNG TÂM",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(KiwiSpacing.xxs))
                    Text(
                        text = appDisplayInfo.app.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(KiwiSpacing.md))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 340.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(KiwiSpacing.xs)) {
                        PlatformPanels(appDisplayInfo, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(KiwiSpacing.xs)) {
                        PlatformPanels(appDisplayInfo, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformPanels(
    appDisplayInfo: AppDisplayInfo,
    panelModifier: Modifier
) {
    appDisplayInfo.app.phone?.let { phone ->
        PlatformStatusPanel(
            label = "Điện thoại",
            symbol = "📱",
            version = appDisplayInfo.phoneInstalled?.versionName ?: "v${phone.versionName}",
            status = appDisplayInfo.phoneStatus,
            latestVersion = phone.versionName,
            modifier = panelModifier
        )
    }
    appDisplayInfo.app.watch?.let { watch ->
        PlatformStatusPanel(
            label = "Wear OS",
            symbol = "⌚",
            version = appDisplayInfo.watchInstalled?.versionName ?: "v${watch.versionName}",
            status = appDisplayInfo.watchStatus,
            latestVersion = watch.versionName,
            modifier = panelModifier
        )
    }
}

@Composable
private fun PlatformStatusPanel(
    label: String,
    symbol: String,
    version: String,
    status: InstallStatus,
    latestVersion: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(KiwiSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$symbol $label",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = version,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(KiwiSpacing.xs))
        StatusBadge(status = status, latestVersion = latestVersion)
    }
}

private fun appVisual(id: String): Pair<String, Brush> = when (id) {
    "kiwi_manager" -> "🥝" to Brush.linearGradient(listOf(Color(0xFF9BEF67), Color(0xFF3C8C35)))
    "gemini_wear" -> "✦" to Brush.linearGradient(listOf(Color(0xFF71C4FF), Color(0xFF7867D9)))
    "open_navigation" -> "↗" to Brush.linearGradient(listOf(Color(0xFFFFD166), Color(0xFFE76F51)))
    "custom_vibration" -> "≋" to Brush.linearGradient(listOf(Color(0xFFFF8FAB), Color(0xFF9B5DE5)))
    else -> "▦" to Brush.linearGradient(listOf(Color(0xFF90A4AE), Color(0xFF455A64)))
}
