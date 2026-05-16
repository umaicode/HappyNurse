// HandoverPayload.slots 를 8개 섹션으로 나눠 인수인계 멘트를 렌더한다.
// Outlined 카드 + 섹션별 헤더 배경 톤으로 시각적 구분을 강화.
package com.happynurse.presentation.screens.handoff.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.HandoverPayload
import com.happynurse.domain.model.SeverityFlag
import com.happynurse.domain.model.Slot
import com.happynurse.domain.model.SlotItem
import com.happynurse.presentation.theme.HnColors

private data class SbarGroup(
    val key: String,
    val title: String,
    val accent: Color,
    val headerBg: Color,
    val slot: Slot?,
)

// 8개 섹션 고유 팔레트 — 차분한 톤. accent(헤더 텍스트)는 충분히 진하게, 배경은 옅게.
private val Indigo    = Color(0xFF243286) // 상황
private val Rose      = Color(0xFF832E4A) // 환자 문제
private val Teal      = Color(0xFF1D6E68) // 배경
private val Amber     = Color(0xFF835E1B) // 평가
private val Coral     = Color(0xFF7C2323) // 안전
private val Tangerine = Color(0xFF8A4A1B) // 조치
private val Lavender  = Color(0xFF4A338D) // 권고
private val Sage      = Color(0xFF237A46) // 종합

private fun groupsOf(p: HandoverPayload): List<SbarGroup> {
    val s = p.slots
    return listOf(
        SbarGroup("situation",      "상황",     Indigo,    Indigo.copy(alpha    = 0.07f), s?.situation),
        SbarGroup("patientProblem", "환자 문제", Rose,      Rose.copy(alpha      = 0.07f), s?.patientProblem),
        SbarGroup("background",     "배경",     Teal,      Teal.copy(alpha      = 0.07f), s?.background),
        SbarGroup("assessment",     "평가",     Amber,     Amber.copy(alpha     = 0.08f), s?.assessment),
        SbarGroup("safety",         "안전",     Coral,     Coral.copy(alpha     = 0.07f), s?.safety),
        SbarGroup("action",         "조치",     Tangerine, Tangerine.copy(alpha = 0.07f), s?.action),
        SbarGroup("recommendation", "권고",     Lavender,  Lavender.copy(alpha  = 0.07f), s?.recommendation),
        SbarGroup("synthesis",      "종합",     Sage,      Sage.copy(alpha      = 0.08f), s?.synthesis),
    )
}

/** synthesis 슬롯 items 만 추출 — SynthesisChecklist 에서 사용. */
fun synthesisItemsOf(payload: HandoverPayload): List<SlotItem> =
    payload.slots?.synthesis?.items.orEmpty().filter { !it.value.isNullOrBlank() }

@Composable
fun PassBarSlotSection(
    payload: HandoverPayload,
    fallbackText: String?,
) {
    Column(Modifier.fillMaxWidth()) {
        if (payload.slots == null) {
            if (payload.header.isNotBlank()) {
                Text(payload.header, fontSize = 16.sp, color = HnColors.Text, lineHeight = 20.sp)
                Spacer(Modifier.height(8.dp))
            }
            if (!fallbackText.isNullOrBlank()) {
                Text(fallbackText, fontSize = 14.sp, color = HnColors.TextSecondary, lineHeight = 19.sp)
            }
            return
        }
        groupsOf(payload).forEachIndexed { idx, group ->
            if (idx > 0) Spacer(Modifier.height(10.dp))
            SbarBlock(group)
        }
    }
}

@Composable
private fun SbarBlock(group: SbarGroup) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HnColors.Surface)
            .border(BorderStroke(1.dp, HnColors.Border), RoundedCornerShape(12.dp)),
    ) {
        // 헤더 — 섹션별 accent 톤 배경
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(group.headerBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(group.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = group.accent)
            Spacer(Modifier.weight(1f))
            val itemCount = group.slot?.items.orEmpty().count { !it.value.isNullOrBlank() || !it.quote.isNullOrBlank() }
            if (itemCount > 0) {
                Text(
                    "${itemCount}건",
                    fontSize = 14.sp,
                    color = group.accent,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = HnColors.Border)
        // 본문
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            val items = group.slot?.items.orEmpty()
                .filter { !it.value.isNullOrBlank() || !it.quote.isNullOrBlank() }
            if (items.isEmpty()) {
                Text("기록 없음", fontSize = 14.sp, color = HnColors.TextTertiary)
            } else {
                items.forEachIndexed { idx, item ->
                    if (idx > 0) Spacer(Modifier.height(8.dp))
                    SlotItemLine(item)
                }
            }
        }
    }
}

@Composable
private fun SlotItemLine(item: SlotItem) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        val markerColor = when (item.severityFlag) {
            SeverityFlag.UNSTABLE -> HnColors.Danger
            SeverityFlag.WATCHER -> HnColors.Warning
            SeverityFlag.STABLE -> HnColors.Success
            else -> HnColors.BorderStrong
        }
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(markerColor),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.fillMaxWidth()) {
            if (!item.value.isNullOrBlank()) {
                Text(
                    item.value,
                    fontSize = 14.sp,
                    color = HnColors.Text,
                    lineHeight = 19.sp,
                )
            }
            if (!item.quote.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HnColors.PrimarySoft)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        "“${item.quote}”",
                        fontSize = 14.sp,
                        color = HnColors.TextSecondary,
                        lineHeight = 17.sp,
                    )
                }
            }
            val subs = buildList {
                item.timeWindow?.takeIf { it.isNotBlank() }?.let { add(it) }
                item.trend?.takeIf { it.isNotBlank() }?.let { add("추이: $it") }
                item.contingency?.takeIf { it.isNotBlank() }?.let { add("대응: $it") }
            }
            if (subs.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    subs.joinToString(" · "),
                    fontSize = 12.sp,
                    color = HnColors.TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
