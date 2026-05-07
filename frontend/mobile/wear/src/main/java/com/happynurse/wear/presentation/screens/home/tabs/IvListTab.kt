// IvListTab — 홈 수액 탭. 카드 탭 시 s08 IvProgressScreen 진입, 좌측 스와이프 시 삭제 버튼.
package com.happynurse.wear.presentation.screens.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.presentation.components.HnSwipeToDeleteCard
import com.happynurse.wear.presentation.theme.HnIvBlue

@Composable
fun IvListTab(
    items: List<IvInfusionTimer>,
    onCardClick: (IvInfusionTimer) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyMessage(text = "진행 중인 수액이 없어요", modifier = modifier)
        return
    }
    val listState = rememberScalingLazyListState()
    ScalingLazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items, key = { it.ivInfusionId }) { iv ->
            HnSwipeToDeleteCard(
                onDelete = { onDelete(iv.ivInfusionId) },
                onClick = { onCardClick(iv) },
            ) {
                IvCardContent(iv)
            }
        }
    }
}

@Composable
private fun IvCardContent(iv: IvInfusionTimer) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = iv.patientName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "  ·  ${iv.medicationName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = iv.remainingTimeText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (iv.isUrgent) MaterialTheme.colorScheme.error else HnIvBlue,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = iv.patientRoomBed,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
