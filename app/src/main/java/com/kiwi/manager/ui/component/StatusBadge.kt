package com.kiwi.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiwi.manager.domain.model.InstallStatus
import com.kiwi.manager.ui.theme.StatusNotInstalled
import com.kiwi.manager.ui.theme.StatusSuccess
import com.kiwi.manager.ui.theme.StatusUpdate

@Composable
fun StatusBadge(
    status: InstallStatus,
    latestVersion: String = "",
    modifier: Modifier = Modifier
) {
    val visual = when (status) {
        InstallStatus.NOT_INSTALLED -> StatusVisual(StatusNotInstalled, "Chưa cài")
        InstallStatus.UPDATE_AVAILABLE -> StatusVisual(
            StatusUpdate,
            if (latestVersion.isNotEmpty()) "Có bản $latestVersion" else "Có bản mới"
        )
        InstallStatus.UP_TO_DATE -> StatusVisual(StatusSuccess, "Đã cập nhật")
        InstallStatus.UNKNOWN -> StatusVisual(MaterialTheme.colorScheme.outline, "Chưa xác định")
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 28.dp)
            .clip(MaterialTheme.shapes.small)
            .background(visual.color.copy(alpha = 0.12f))
            .border(1.dp, visual.color.copy(alpha = 0.42f), MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(visual.color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = visual.label,
                color = visual.color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class StatusVisual(val color: Color, val label: String)
