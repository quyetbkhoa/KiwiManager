package com.kiwi.manager.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiwi.manager.domain.model.InstallStatus
import com.kiwi.manager.ui.theme.KiwiNeon

@Composable
fun StatusBadge(
    status: InstallStatus,
    latestVersion: String = "",
    modifier: Modifier = Modifier
) {
    val (dotColor, borderColor, bgColor, label) = when (status) {
        InstallStatus.NOT_INSTALLED -> Quadruple(
            Color(0xFF90A4AE),
            Color(0x33B0BEC5),
            Color(0x1590A4AE),
            "Chưa cài"
        )
        InstallStatus.UPDATE_AVAILABLE -> Quadruple(
            Color(0xFFFF9100),
            Color(0x66FF9100),
            Color(0x22FF9100),
            if (latestVersion.isNotEmpty()) "Cập nhật v$latestVersion" else "Có bản mới"
        )
        InstallStatus.UP_TO_DATE -> Quadruple(
            KiwiNeon,
            Color(0x6676FF03),
            Color(0x2076FF03),
            "✓ Đã mới nhất"
        )
        InstallStatus.UNKNOWN -> Quadruple(
            Color(0xFF78909C),
            Color(0x3378909C),
            Color(0x1078909C),
            "—"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (status == InstallStatus.UP_TO_DATE) KiwiNeon else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
