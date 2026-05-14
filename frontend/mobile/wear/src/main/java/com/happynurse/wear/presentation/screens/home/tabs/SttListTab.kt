// SttListTab — 홈 타이머 탭. 등록된 STT 알람 목록을 카드로 보여줌.
package com.happynurse.wear.presentation.screens.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.domain.model.SttTimer
import com.happynurse.wear.presentation.theme.remainingTimeColor

@Composable
fun SttListTab(
    items: List<SttTimer>,
    onCardClick: (SttTimer) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading && items.isEmpty() -> StatusMessage("불러오는 중…", modifier)
        errorMessage != null && items.isEmpty() -> StatusMessage(errorMessage, modifier)
        items.isEmpty() -> StatusMessage("등록한 알람이 없어요", modifier)
        else -> {
            val listState = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(items, key = { it.sttReminderId }) { stt ->
                    SttCardContent(stt = stt, onClick = { onCardClick(stt) })
                }
            }
        }
    }
}

@Composable
private fun SttCardContent(stt: SttTimer, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stt.contentSummary,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = "종료 ${stt.endAtDisplay}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = stt.remainingTimeText,
            style = MaterialTheme.typography.bodyMedium,
            color = remainingTimeColor(stt.remainingSec),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
