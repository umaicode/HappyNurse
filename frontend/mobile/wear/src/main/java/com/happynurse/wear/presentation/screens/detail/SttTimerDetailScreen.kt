// SttTimerDetailScreen (s20a) — STT 타이머 상세. 좌(남은시간 카운트다운) + 우(알림 시각) + STT 원문.
// 5분 이하 시 좌측 시간이 빨강 + 펄스. 카운트다운은 LaunchedEffect 로 1초 tick.
package com.happynurse.wear.presentation.screens.detail

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.presentation.components.HnRoomBedPill
import com.happynurse.wear.presentation.theme.HnSttPurple
import com.happynurse.wear.presentation.theme.TabularNumStyle
import kotlinx.coroutines.delay

@Composable
fun SttTimerDetailScreen(
    stt: SttTimer,
    onBack: () -> Unit,
) {
    var remaining by remember(stt.sttTimerId) { mutableIntStateOf(stt.remainingSec) }
    LaunchedEffect(stt.sttTimerId) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    val urgent = remaining in 1..(5 * 60)

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 14.dp)
                .clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "뒤로",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "알람 리스트",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 36.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stt.patientName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            HnRoomBedPill(text = stt.patientRoomBed)

            TimeBoxes(
                remaining = remaining,
                endAt = stt.endAtDisplay,
                urgent = urgent,
            )

            Text(
                text = "STT 내용",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stt.sttText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TimeBoxes(remaining: Int, endAt: String, urgent: Boolean) {
    val transition = rememberInfiniteTransition(label = "urgentPulse")
    val animatedAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), repeatMode = RepeatMode.Reverse),
        label = "urgentAlpha",
    )
    val pulseAlpha = if (urgent) animatedAlpha else 1f
    val leftColor = if (urgent) MaterialTheme.colorScheme.error else HnSttPurple
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimeColumn(
            label = "남은 시간",
            value = formatMmSs(remaining),
            color = leftColor,
            modifier = Modifier.weight(1f).alpha(pulseAlpha),
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(36.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
        TimeColumn(
            label = "알림 시각",
            value = endAt,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TimeColumn(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.merge(TabularNumStyle),
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
    Spacer(Modifier)
}

private fun formatMmSs(sec: Int): String {
    val s = if (sec < 0) 0 else sec
    val m = s / 60
    val r = s % 60
    return "%02d:%02d".format(m, r)
}
