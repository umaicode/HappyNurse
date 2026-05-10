// 워치알람 카드 — STT 음성 메모 알람 (응답 필드: contentSummary, fireAtEpochMillis, sttText)
package com.happynurse.presentation.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.WatchAlarm
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FIRE_AT_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 HH:mm")

@Composable
fun WatchAlarmCard(alarm: WatchAlarm) {
    HnCard(padding = 14.dp) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(HnColors.PrimarySoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = HnColors.Primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = formatFireAt(alarm.fireAtEpochMillis),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HnColors.Primary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = alarm.contentSummary.ifBlank { "(내용 없음)" },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = HnColors.Text,
            )
            if (alarm.sttText.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = alarm.sttText,
                    fontSize = 12.sp,
                    color = HnColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatFireAt(epochMillis: Long?): String {
    if (epochMillis == null) return "-"
    return runCatching {
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(FIRE_AT_FORMATTER)
    }.getOrDefault("-")
}
