// HandoverPayload.slots 를 8개 섹션으로 나눠 인수인계 멘트를 렌더한다.
// 체크박스/검증 배지/좌측 컬러 보더는 사용하지 않는다 — 본문은 텍스트 위주.
package com.happynurse.presentation.screens.handoff.components

import androidx.compose.foundation.background
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
    val slot: Slot?,
)

private fun groupsOf(p: HandoverPayload): List<SbarGroup> {
    val s = p.slots
    return listOf(
        SbarGroup("situation", "상황", HnColors.Primary, s?.situation),
        SbarGroup("patientProblem", "환자 문제", HnColors.Primary, s?.patientProblem),
        SbarGroup("background", "배경", HnColors.Info, s?.background),
        SbarGroup("assessment", "평가", HnColors.Warning, s?.assessment),
        SbarGroup("safety", "안전", HnColors.Danger, s?.safety),
        SbarGroup("action", "조치", HnColors.Warning, s?.action),
        SbarGroup("recommendation", "권고", HnColors.Success, s?.recommendation),
        SbarGroup("synthesis", "종합", HnColors.Success, s?.synthesis),
    )
}

/** 환자 카드에서 상단 체크리스트를 만들기 위해 사용 — 모든 SlotItem 을 평탄화해 (그룹 타이틀, item) 형태로 반환. */
fun flattenSlotItems(payload: HandoverPayload): List<Pair<String, SlotItem>> {
    val slots = payload.slots ?: return emptyList()
    return groupsOf(payload).flatMap { g ->
        g.slot?.items.orEmpty()
            .filter { !it.value.isNullOrBlank() }
            .map { g.title to it }
    }
}

@Composable
fun PassBarSlotSection(
    payload: HandoverPayload,
    fallbackText: String?,
) {
    Column(Modifier.fillMaxWidth()) {
        if (payload.slots == null) {
            if (payload.header.isNotBlank()) {
                Text(payload.header, fontSize = 14.sp, color = HnColors.Text, lineHeight = 20.sp)
                Spacer(Modifier.height(8.dp))
            }
            if (!fallbackText.isNullOrBlank()) {
                Text(fallbackText, fontSize = 13.sp, color = HnColors.TextSecondary, lineHeight = 19.sp)
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
            .background(HnColors.SurfaceAlt)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(group.accent),
            )
            Spacer(Modifier.width(8.dp))
            Text(group.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        }
        Spacer(Modifier.height(8.dp))
        val items = group.slot?.items.orEmpty()
            .filter { !it.value.isNullOrBlank() || !it.quote.isNullOrBlank() }
        if (items.isEmpty()) {
            Text("기록 없음", fontSize = 12.sp, color = HnColors.TextTertiary)
        } else {
            items.forEachIndexed { idx, item ->
                if (idx > 0) Spacer(Modifier.height(6.dp))
                SlotItemLine(item)
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
                    fontSize = 13.sp,
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
                        fontSize = 12.sp,
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
                    fontSize = 11.sp,
                    color = HnColors.TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

