// IvListTab — 홈 수액 탭. 카드 한 장은 환자/호실, 수액 이름, 남은 시간 3줄로 구성된다.
// 카드 탭 시 수액 진행 상세 화면(IvProgressScreen) 으로 진입한다.
package com.happynurse.wear.presentation.screens.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.happynurse.wear.presentation.theme.remainingTimeColor

@Composable
fun IvListTab(
    items: List<IvInfusionTimer>,
    onCardClick: (IvInfusionTimer) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading && items.isEmpty() -> StatusMessage("불러오는 중…", modifier)
        errorMessage != null && items.isEmpty() -> StatusMessage(errorMessage, modifier)
        items.isEmpty() -> StatusMessage("진행 중인 수액이 없어요", modifier)
        else -> {
            val listState = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(items, key = { it.ivInfusionId }) { iv ->
                    IvCardContent(iv = iv, onClick = { onCardClick(iv) })
                }
            }
        }
    }
}

@Composable
private fun IvCardContent(iv: IvInfusionTimer, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
            Text(
                text = buildString {
                    append(iv.patientName.ifBlank { "환자" })
                    if (iv.patientRoomBed.isNotBlank() && iv.patientRoomBed != "-") {
                        append("  ")
                        append(iv.patientRoomBed)
                    }
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = iv.endAtDisplay,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        Text(
            text = iv.medicationLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = iv.remainingTimeText,
            style = MaterialTheme.typography.bodyMedium,
            color = remainingTimeColor(iv.remainingSec),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
internal fun StatusMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
