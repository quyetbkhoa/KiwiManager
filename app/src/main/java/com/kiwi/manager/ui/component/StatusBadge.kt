package com.kiwi.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kiwi.manager.domain.model.InstallStatus

@Composable
fun StatusBadge(status: InstallStatus, latestVersion: String = "") {
    val (backgroundColor, textColor, text) = when (status) {
        InstallStatus.NOT_INSTALLED -> Triple(
            Color.LightGray, 
            Color.DarkGray, 
            "Not installed"
        )
        InstallStatus.UPDATE_AVAILABLE -> Triple(
            Color(0xFFFFA000), // Amber
            Color.White, 
            "Update → $latestVersion"
        )
        InstallStatus.UP_TO_DATE -> Triple(
            Color(0xFF4CAF50), // Green
            Color.White, 
            "✓ Latest"
        )
        InstallStatus.UNKNOWN -> Triple(
            MaterialTheme.colorScheme.surfaceVariant, 
            MaterialTheme.colorScheme.onSurfaceVariant, 
            "—"
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
