// 녹음 중 실시간 amplitude 막대그래프 — 정지 버튼 뒤에 가로로 펼쳐져 우→좌로 흐름.
package com.happynurse.wear.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

private val MinBarHeight = 3.dp
private val MaxBarHeight = 56.dp
// 막대 폭 : 간격 = 2 : 1.5 비율로 가용 너비에 비례 분배.
private const val BarWidthRatio = 2f
private const val GapRatio = 1.5f

@Composable
fun HnAudioWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.errorContainer,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(MaxBarHeight),
    ) {
        val count = amplitudes.size
        if (count == 0) return@Canvas
        // 가용 너비를 막대+간격 으로 분배. 막대 count 개, 간격 (count-1) 개.
        val slotWidth = size.width / (count * BarWidthRatio + (count - 1).coerceAtLeast(0) * GapRatio)
        val barWidthPx = slotWidth * BarWidthRatio
        val gapPx = slotWidth * GapRatio
        val minHeightPx = MinBarHeight.toPx()
        val maxHeightPx = MaxBarHeight.toPx()
        val cornerPx = barWidthPx / 2f
        val centerY = size.height / 2f
        amplitudes.forEachIndexed { index, normalized ->
            val clamped = normalized.coerceIn(0f, 1f)
            val barHeight = minHeightPx + (maxHeightPx - minHeightPx) * clamped
            val left = index * (barWidthPx + gapPx)
            val top = centerY - barHeight / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
            )
        }
    }
}
