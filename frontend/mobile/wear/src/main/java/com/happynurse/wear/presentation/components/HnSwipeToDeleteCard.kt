// HnSwipeToDeleteCard — 카드를 좌측으로 30dp 이상 스와이프 시 우측에 삭제 버튼이 노출되는 컨테이너.
// 페이지 전환과 충돌하지 않도록 가로 드래그가 우세할 때만 카드 transformX 를 갱신한다.
package com.happynurse.wear.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme

private const val SWIPE_THRESHOLD_DP = 30
private const val OPENED_OFFSET_DP = 78

@Composable
fun HnSwipeToDeleteCard(
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.dp.toPx() }
    val openedPx = with(density) { OPENED_OFFSET_DP.dp.toPx() }

    var opened by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    val target = if (opened) -openedPx else 0f
    val animated by animateFloatAsState(targetValue = target + dragX, label = "swipeOffset")

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(OPENED_OFFSET_DP.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .clickable {
                        opened = false
                        dragX = 0f
                        onDelete()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = animated }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onDragEnd = {
                            val passedThreshold =
                                if (opened) dragX > thresholdPx else dragX < -thresholdPx
                            opened = if (opened) !passedThreshold else passedThreshold
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f },
                        onHorizontalDrag = { _, delta ->
                            dragX = (dragX + delta).coerceIn(-openedPx, openedPx)
                        },
                    )
                }
                .clickable(enabled = !opened) { onClick() },
        ) {
            content()
        }
    }
}
