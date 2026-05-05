// 수액 타이머 카드 — bar/ring 변형, 잔량 비율에 따라 색 변경(녹색→주황→빨강)
package com.happynurse.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.IVTimer
import com.happynurse.presentation.theme.HnColors

enum class IVTimerLayout { BAR, RING }

@Composable
fun IVTimerCard(
    iv: IVTimer,
    layout: IVTimerLayout = IVTimerLayout.BAR,
) {
    val pct = (iv.elapsedMin.toFloat() / iv.totalMin).coerceIn(0f, 1f)
    val color: Color = when {
        pct < 0.5f -> HnColors.Success
        pct < 0.8f -> HnColors.Warning
        else       -> HnColors.Danger
    }
    if (layout == IVTimerLayout.RING) RingCard(iv, pct, color) else BarCard(iv, pct, color)
}

@Composable
private fun BarCard(iv: IVTimer, pct: Float, color: Color) {
    HnCard {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(iv.patient, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Spacer(Modifier.size(6.dp))
                TagChip(iv.room, fg = HnColors.Primary, bg = HnColors.PrimarySoft)
                Spacer(Modifier.weight(1f))
                Text("종료 ${iv.endsAt}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(iv.drug, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HnColors.Border),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(pct)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${iv.elapsedMin / 60}시간 ${iv.elapsedMin % 60}분 / ${iv.totalMin / 60}시간 ${iv.totalMin % 60}분",
                fontSize = 12.sp,
                color = HnColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun RingCard(iv: IVTimer, pct: Float, color: Color) {
    HnCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(pct = pct, color = color)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(iv.patient, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    Spacer(Modifier.size(6.dp))
                    TagChip(iv.room, fg = HnColors.Primary, bg = HnColors.PrimarySoft)
                }
                Text(iv.drug, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text, modifier = Modifier.padding(top = 4.dp))
                val remaining = iv.totalMin - iv.elapsedMin
                Text(
                    "남은 시간 ${remaining / 60}h ${remaining % 60}m · 종료 ${iv.endsAt}",
                    fontSize = 12.sp,
                    color = HnColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(pct: Float, color: Color, sizeDp: Int = 56) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(sizeDp.dp)) {
        Canvas(Modifier.size(sizeDp.dp)) {
            val stroke = 5.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
            drawArc(
                color = HnColors.Border,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * pct, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text("${(pct * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
