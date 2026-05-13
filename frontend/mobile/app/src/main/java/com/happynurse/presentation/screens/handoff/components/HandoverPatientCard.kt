// 인수인계 환자 카드 — Pager 내부에서 1명을 표현. 항상 펼친 상태로 AI 인수인계 본문을 표시.
// 상단에 확인 체크리스트(요약 항목 평탄화), 본문에 PASS-BAR 섹션, 하단에 근거 리스트.
package com.happynurse.presentation.screens.handoff.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.Citation
import com.happynurse.domain.model.HandoverDetail
import com.happynurse.domain.model.Patient
import com.happynurse.domain.model.RosterPatientItem
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors

@Composable
fun HandoverPatientCard(
    item: RosterPatientItem,
    patient: Patient?,
    detail: HandoverDetail?,
    loadingDetail: Boolean,
    onOpenPatient: () -> Unit = {},
) {
    HnCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 14.dp) {
        Column(Modifier.fillMaxWidth()) {
            // 환자 헤더 — 탭하면 환자 상세로 이동
            if (patient != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPatient() },
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                patient.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HnColors.Text,
                            )
                            Spacer(Modifier.width(8.dp))
                            val sub = listOfNotNull(
                                patient.sex.takeIf { it.isNotBlank() },
                                patient.birthdate.takeIf { it.isNotBlank() },
                            ).joinToString(" · ")
                            if (sub.isNotBlank()) {
                                Text(
                                    sub,
                                    fontSize = 14.sp,
                                    color = HnColors.TextSecondary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Text(
                item.header.ifBlank { "AI 요약 준비중입니다." },
                fontSize = 14.sp,
                color = HnColors.Text,
                lineHeight = 20.sp,
            )

            if (item.rulesFiredBrief.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                RulesRow(item.rulesFiredBrief)
            }

            Spacer(Modifier.height(12.dp))
            when {
                loadingDetail -> {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = HnColors.Primary,
                        )
                    }
                }
                detail?.payload != null -> {
                    val payload = detail.payload
                    // 상단 — 확인 체크리스트(요약 항목들을 평탄화)
                    val flat = remember(payload) { flattenSlotItems(payload) }
                    if (flat.isNotEmpty()) {
                        ConfirmChecklist(items = flat, keyPrefix = item.handoverId.ifBlank { item.encounterId })
                        Spacer(Modifier.height(14.dp))
                    }
                    // 본문 PASS-BAR
                    PassBarSlotSection(payload = payload, fallbackText = detail.autoSummary)
                    // 근거 리스트
                    if (payload.citations.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        CitationsSection(
                            citations = payload.citations,
                            onCitationClick = { onOpenPatient() },
                        )
                    }
                }
                detail?.autoSummary != null -> {
                    Text(
                        detail.autoSummary,
                        fontSize = 13.sp,
                        color = HnColors.TextSecondary,
                        lineHeight = 19.sp,
                    )
                }
                else -> {
                    Text(
                        "상세 인수인계 정보를 불러오는 중입니다…",
                        fontSize = 12.sp,
                        color = HnColors.TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmChecklist(
    items: List<Pair<String, com.happynurse.domain.model.SlotItem>>,
    keyPrefix: String,
) {
    val checked = remember(keyPrefix) { mutableStateMapOf<Int, Boolean>() }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HnColors.PrimaryLight.copy(alpha = 0.45f))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "확인 체크리스트",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HnColors.Primary,
            )
            Spacer(Modifier.width(6.dp))
            val doneCount = checked.values.count { it }
            Text(
                "$doneCount / ${items.size}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = HnColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        items.forEachIndexed { idx, (section, item) ->
            if (idx > 0) Spacer(Modifier.height(6.dp))
            val isChecked = checked[idx] == true
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { checked[idx] = !isChecked }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isChecked) HnColors.Primary else HnColors.Surface)
                        .border(
                            BorderStroke(1.5.dp, if (isChecked) HnColors.Primary else HnColors.BorderStrong),
                            RoundedCornerShape(5.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isChecked) {
                        Icon(
                            Icons.Outlined.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        section,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HnColors.Primary,
                    )
                    Text(
                        item.value ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isChecked) HnColors.TextTertiary else HnColors.Text,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CitationsSection(
    citations: List<Citation>,
    onCitationClick: (Citation) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HnColors.SurfaceAlt)
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Icon(
                Icons.Outlined.Description,
                null,
                tint = HnColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "요약 근거",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HnColors.Text,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "${citations.size}건",
                fontSize = 12.sp,
                color = HnColors.TextSecondary,
            )
            Box(Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "접기" else "펼치기",
                tint = HnColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            citations.forEachIndexed { idx, c ->
                if (idx > 0) Spacer(Modifier.height(6.dp))
                CitationRow(c) { onCitationClick(c) }
            }
        }
    }
}

@Composable
private fun CitationRow(c: Citation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(HnColors.Surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(HnColors.Primary),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            val label = c.label.ifBlank { c.recordId.ifBlank { "근거" } }
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = HnColors.Text,
            )
            val sub = listOfNotNull(
                c.ts.takeIf { it.isNotBlank() },
                c.lineRange.takeIf { it.isNotEmpty() }?.joinToString("-")?.let { "L.$it" },
            ).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(sub, fontSize = 10.sp, color = HnColors.TextTertiary)
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            null,
            tint = HnColors.TextTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun RulesRow(rules: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rules.take(4).forEach { rule ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(HnColors.Warning),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    rule,
                    fontSize = 12.sp,
                    color = HnColors.TextSecondary,
                    lineHeight = 17.sp,
                )
            }
        }
        if (rules.size > 4) {
            Text(
                "+${rules.size - 4}건 더",
                fontSize = 11.sp,
                color = HnColors.TextTertiary,
                modifier = Modifier.padding(start = 11.dp),
            )
        }
    }
}
