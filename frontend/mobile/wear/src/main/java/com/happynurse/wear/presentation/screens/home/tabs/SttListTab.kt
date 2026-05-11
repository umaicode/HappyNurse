// SttListTab — 홈 타이머 탭. 카드 왼쪽 스와이프 시 알람 취소(/reminders/stt/{id} DELETE).
package com.happynurse.wear.presentation.screens.home.tabs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.presentation.theme.remainingTimeColor
import kotlinx.coroutines.launch

@Composable
fun SttListTab(
    items: List<SttTimer>,
    onCardClick: (SttTimer) -> Unit,
    onDelete: (SttTimer) -> Unit = {},
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
                    SwipeDeletableSttCard(
                        stt = stt,
                        onClick = { onCardClick(stt) },
                        onDelete = { onDelete(stt) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeDeletableSttCard(
    stt: SttTimer,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 96.dp.toPx() }
    val offsetX = remember(stt.sttReminderId) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        // 뒷판 — 삭제 표시
        if (offsetX.value < -4f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onError.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "삭제",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // 상단 카드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(stt.sttReminderId) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value <= -dismissThresholdPx) {
                                    offsetX.animateTo(-size.width.toFloat(), tween(160))
                                    onDelete()
                                } else {
                                    offsetX.animateTo(0f, tween(160))
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val next = (offsetX.value + dragAmount).coerceAtMost(0f)
                                offsetX.snapTo(next)
                            }
                        },
                    )
                },
        ) {
            SttCardContent(stt = stt, onClick = onClick)
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
