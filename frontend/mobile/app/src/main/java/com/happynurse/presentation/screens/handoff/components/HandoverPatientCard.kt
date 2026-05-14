// 인수인계 환자 카드 — Pager 내부에서 1명을 표현.
// 상단: 확인 체크리스트(synthesis 슬롯, 서버 sync), 본문: PASS-BAR 섹션, 하단: 근거 리스트(접힘).
package com.happynurse.presentation.screens.handoff.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.CheckMeta
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
    checkedMap: Map<Int, CheckMeta>?,
    inFlight: Set<Int>,
    onToggleCheck: (index: Int, newValue: Boolean) -> Unit,
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
                                fontSize = 18.sp,
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
                                    fontSize = 16.sp,
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
                fontSize = 16.sp,
                color = HnColors.Text,
                lineHeight = 20.sp,
            )

            if (item.rulesFiredBrief.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                RulesRow(item.rulesFiredBrief)
            }

            Spacer(Modifier.height(15.dp))
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
                    // 상단 — synthesis 체크리스트
                    val synthesisItems = remember(payload) { synthesisItemsOf(payload) }
                    if (synthesisItems.isNotEmpty()) {
                        SynthesisChecklist(
                            items = synthesisItems,
                            checkedMap = checkedMap,
                            inFlight = inFlight,
                            onToggle = onToggleCheck,
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                    // 본문 PASS-BAR
                    PassBarSlotSection(payload = payload, fallbackText = detail.autoSummary)
                    // 근거 리스트 (접힘 기본)
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
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = HnColors.Text,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "${citations.size}건",
                fontSize = 14.sp,
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
        Column(Modifier.weight(1f)) {
            val label = c.label.ifBlank { c.recordId.ifBlank { "근거" } }
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = HnColors.Text,
            )
            val sub = listOfNotNull(
                c.ts.takeIf { it.isNotBlank() },
                c.lineRange.takeIf { it.isNotEmpty() }?.joinToString("-")?.let { "L.$it" },
            ).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(sub, fontSize = 12.sp, color = HnColors.TextTertiary)
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
