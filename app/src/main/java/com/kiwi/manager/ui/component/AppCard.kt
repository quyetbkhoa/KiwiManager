package com.kiwi.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kiwi.manager.domain.model.AppDisplayInfo

@Composable
fun AppCard(
    appDisplayInfo: AppDisplayInfo,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Icon placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appDisplayInfo.app.name.firstOrNull()?.toString() ?: "?",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = appDisplayInfo.app.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = appDisplayInfo.app.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Phone Status
            if (appDisplayInfo.app.phone != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📱")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = appDisplayInfo.phoneInstalled?.versionName ?: "Not installed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    StatusBadge(
                        status = appDisplayInfo.phoneStatus,
                        latestVersion = appDisplayInfo.app.phone.versionName
                    )
                }
            }
            
            // Watch Status
            if (appDisplayInfo.app.watch != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⌚")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = appDisplayInfo.watchInstalled?.versionName ?: "Not installed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    StatusBadge(
                        status = appDisplayInfo.watchStatus,
                        latestVersion = appDisplayInfo.app.watch.versionName
                    )
                }
            }
        }
    }
}
