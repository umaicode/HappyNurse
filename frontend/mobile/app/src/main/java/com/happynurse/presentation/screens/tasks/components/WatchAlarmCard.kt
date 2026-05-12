// 워치알람 카드 — STT 음성 메모 알람 (응답 필드: contentSummary, fireAtEpochMillis, sttText)
package com.happynurse.presentation.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FIRE_AT_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 HH:mm")

@Composable
fun WatchAlarmCard(alarm: WatchAlarm) {
    // 1초마다 현재 시각 tick — 남은시간 라벨이 실시간으로 줄어들도록 갱신
    val nowMillis by produceState(initialValue = System.currentTimeMillis(), alarm.fireAtEpochMillis) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    HnCard(modifier = Modifier.fillMaxWidth(), padding = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium ,
                    color = HnColors.Primary,
                )
            }
            Spacer(Modifier.height(15.dp))
            Text(
                text = alarm.contentSummary.ifBlank { "(내용 없음)" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium ,
                color = HnColors.Text,
            )
            val remaining = formatRemaining(alarm.fireAtEpochMillis, nowMillis)
            if (remaining != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = remaining,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HnColors.TextSecondary,
                )
            }
        }
    }
}

private fun formatRemaining(epochMillis: Long?, nowMillis: Long): String? {
    if (epochMillis == null) return null
    val diffMs = epochMillis - nowMillis
    if (diffMs <= 0L) return null
    val totalSec = diffMs / 1000L
    // 1분 미만: 초 단위로 표시
    if (totalSec < 60L) return "남은 시간 ${totalSec}초"
    val totalMin = totalSec / 60L
    val days = totalMin / (60 * 24)
    val hours = (totalMin % (60 * 24)) / 60
    val minutes = totalMin % 60
    return buildString {
        append("남은 시간 ")
        if (days > 0) append("${days}일 ")
        if (hours > 0 || days > 0) append("${hours}시간 ")
        append("${minutes}분")
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
