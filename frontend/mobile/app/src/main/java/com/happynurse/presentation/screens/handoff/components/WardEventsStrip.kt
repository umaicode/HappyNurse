// 인수인계 상단 — 오늘 병동 입원/퇴원 환자 가로 스크롤 strip.
// 기존 "AI 통합 요약(narrativeHeader)" 카드 대체.
package com.happynurse.presentation.screens.handoff.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.WardEventEntry
import com.happynurse.domain.model.WardEvents
import com.happynurse.presentation.theme.HnColors

private val AdmissionColor = Color(0xFF2563EB) // 입원 — 진한 파랑
private val DischargeColor = Color(0xFF0EA5A4) // 퇴원 — 청록

@Composable
fun WardEventsStrip(
    events: WardEvents?,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // 헤더 — 토글 가능
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 6.dp, horizontal = 2.dp),
        ) {
            Text(
                "오늘 입퇴원 환자",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HnColors.Text,
            )
            Spacer(Modifier.width(8.dp))
            if (events != null) {
                CountBadge(label = "입원", count = events.admissions.size, color = AdmissionColor)
                Spacer(Modifier.width(6.dp))
                CountBadge(label = "퇴원", count = events.discharges.size, color = DischargeColor)
            }
            Spacer(Modifier.weight(1f))
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.6.dp,
                    color = HnColors.Primary,
                )
                Spacer(Modifier.width(6.dp))
            }
            // 토글 버튼
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HnColors.SurfaceAlt),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "접기" else "펼치기",
                    tint = HnColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(6.dp))
                when {
                    error != null && events == null -> ErrorRow(onRetry = onRetry)
                    events == null -> LoadingRow()
                    events.isEmpty -> EmptyRow()
                    else -> {
                        val combined = remember(events) {
                            buildList {
                                events.admissions.forEach { add(WardEventCardData(it, isAdmission = true)) }
                                events.discharges.forEach { add(WardEventCardData(it, isAdmission = false)) }
                            }
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                        ) {
                            itemsIndexed(
                                combined,
                                key = { _, c -> "${c.isAdmission}-${c.entry.encounterId}-${c.entry.timestamp}" },
                            ) { _, data -> WardEventCard(data) }
                        }
                    }
                }
            }
        }
    }
}

private data class WardEventCardData(
    val entry: WardEventEntry,
    val isAdmission: Boolean,
)

@Composable
private fun WardEventCard(data: WardEventCardData) {
    val accent = if (data.isAdmission) AdmissionColor else DischargeColor
    Row(
        Modifier
            .width(230.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HnColors.Surface)
            .border(BorderStroke(1.dp, HnColors.Border), RoundedCornerShape(12.dp)),
    ) {
        // 좌측 컬러 스트라이프로 입원/퇴원 구분
        Box(
            Modifier
                .width(4.dp)
                .background(accent),
        ) { Spacer(Modifier.height(72.dp)) }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (data.isAdmission) "입원" else "퇴원",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
                Spacer(Modifier.weight(1f))
                data.entry.timestamp?.let { ts ->
                    Text(
                        formatShortDateTime(ts),
                        fontSize = 12.sp,
                        color = HnColors.TextTertiary,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    data.entry.patientName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HnColors.Text,
                )
                if (data.entry.location.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        data.entry.location,
                        fontSize = 12.sp,
                        color = HnColors.TextSecondary,
                    )
                }
            }
            if (data.entry.primaryLabel.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    data.entry.primaryLabel,
                    fontSize = 12.sp,
                    color = HnColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun CountBadge(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            count.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun LoadingRow() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HnColors.SurfaceAlt),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = HnColors.Primary,
        )
    }
}

@Composable
private fun EmptyRow() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HnColors.SurfaceAlt)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("오늘 입퇴원한 환자가 없습니다", fontSize = 14.sp, color = HnColors.TextTertiary)
    }
}

@Composable
private fun ErrorRow(onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HnColors.SurfaceAlt)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "입퇴원 정보를 불러오지 못했습니다",
            fontSize = 12.sp,
            color = HnColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onRetry() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Refresh, null, tint = HnColors.Primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("다시 시도", fontSize = 12.sp, color = HnColors.Primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

// "2026-05-13T09:15:00" / "2026-05-13T09:15:00+09:00" → "05/13 09:15"
private fun formatShortDateTime(iso: String): String {
    return try {
        val t = iso.substringBefore('+').substringBefore('Z')
        val datePart = t.substringBefore('T')
        val timePart = t.substringAfter('T', missingDelimiterValue = "")
        val md = datePart.substringAfter('-', missingDelimiterValue = datePart).replace('-', '/')
        val hm = timePart.take(5)
        if (hm.isBlank()) md else "$md $hm"
    } catch (_: Throwable) {
        iso
    }
}
