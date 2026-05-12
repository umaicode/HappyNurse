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
import com.happynurse.domain.model.IvTimer
import com.happynurse.presentation.theme.HnColors

enum class IvTimerLayout { BAR, RING }

// IV 타이머 카드 전용 색상 — 그린(여유) / 옐로우(주의) / 테라코타(임박)
private val IvSafeColor    = Color(0xFF2E7D5B)  // deep emerald   ─ 진행 < 50%   (여유)
private val IvCautionColor = Color(0xFFD4A017)  // mustard yellow ─ 50% ≤ 진행 < 80% (주의)
private val IvUrgentColor  = Color(0xFFC84B4B)  // terracotta     ─ 80% 이상      (임박)

@Composable
fun IvTimerCard(
    iv: IvTimer,
    layout: IvTimerLayout = IvTimerLayout.BAR,
) {
    val pct = (iv.elapsedMin.toFloat() / iv.totalMin).coerceIn(0f, 1f)
    val color: Color = when {
        pct < 0.5f -> IvSafeColor
        pct < 0.8f -> IvCautionColor
        else       -> IvUrgentColor
    }
    if (layout == IvTimerLayout.RING) RingCard(iv, pct, color) else BarCard(iv, pct, color)
}

@Composable
private fun BarCard(iv: IvTimer, pct: Float, color: Color) {
    val fillRatio = (1f - pct).coerceIn(0f, 1f)
    val gtt = iv.rateGttPerMin?.takeIf { it > 0 }
    val drugLines = iv.drug.split(" + ").map { it.trim() }.filter { it.isNotEmpty() }
    val remainingMin = (iv.totalMin - iv.elapsedMin).coerceAtLeast(0)
    HnCard {
        Row(verticalAlignment = Alignment.Top) {
            // 좌측: IV 백 + 똑똑 떨어지는 방울 (chamber 옆에 gtt/min 라벨이 함께 그려짐)
            IvDripAnimation(
                fillRatio = fillRatio,
                color = color,
                gttPerMin = gtt,
                modifier = Modifier.size(width = 90.dp, height = 120.dp),
            )
            Spacer(Modifier.size(4.dp))
            // 우측: 환자 + 약물 + 진행 바 + 남은시간
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(iv.patient, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    Spacer(Modifier.size(6.dp))
                    // 호실-침대: WardPatientListResponse 룩업값 사용 (slim IV 응답에 없음)
                    val roomBed = listOfNotNull(
                        iv.room.takeIf { it.isNotBlank() },
                        iv.bed.takeIf { it.isNotBlank() },
                    ).joinToString("-")
                    if (roomBed.isNotBlank()) TagChip(roomBed)
                    Spacer(Modifier.weight(1f))
                    Text("종료 ${iv.endsAt}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                }
                Spacer(Modifier.height(12.dp))
                // 약물 — 1개면 한 줄, 2개 이상이면 줄바꿈해서 깔끔히 나열
                drugLines.forEachIndexed { idx, line ->
                    Text(
                        line,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HnColors.Text,
                    )
                    if (idx < drugLines.lastIndex) Spacer(Modifier.height(2.dp))
                }
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
                    "남은시간 ${remainingMin / 60}시간 ${remainingMin % 60}분 / ${iv.totalMin / 60}시간 ${iv.totalMin % 60}분",
                    fontSize = 12.sp,
                    color = HnColors.Text,
                )
            }
        }
    }
}

@Composable
private fun RingCard(iv: IvTimer, pct: Float, color: Color) {
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
                    color = HnColors.Text,
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
