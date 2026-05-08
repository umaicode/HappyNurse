// SttListTab — 홈 STT 타이머 탭. 카드 탭 시 s20a SttTimerDetailScreen 진입.
package com.happynurse.wear.presentation.screens.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.presentation.components.HnSwipeToDeleteCard
import com.happynurse.wear.presentation.theme.HnSttPurple

@Composable
fun SttListTab(
    items: List<SttTimer>,
    onCardClick: (SttTimer) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyMessage(text = "예약된 타이머가 없어요", modifier = modifier)
        return
    }
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items, key = { it.sttTimerId }) { stt ->
            HnSwipeToDeleteCard(
                onDelete = { onDelete(stt.sttTimerId) },
                onClick = { onCardClick(stt) },
            ) {
                SttCardContent(stt)
            }
        }
    }
}

@Composable
private fun SttCardContent(stt: SttTimer) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stt.patientName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "  ·  ${stt.contentSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "STT · ${formatRemain(stt.remainingSec)}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (stt.isUrgent) MaterialTheme.colorScheme.error else HnSttPurple,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stt.patientRoomBed,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatRemain(sec: Int): String {
    if (sec <= 0) return "0분 남음"
    val m = sec / 60
    val s = sec % 60
    return when {
        m >= 60 -> "${m / 60}h ${m % 60}m 남음"
        m > 0 -> "${m}분 남음"
        else -> "${s}초 남음"
    }
}
