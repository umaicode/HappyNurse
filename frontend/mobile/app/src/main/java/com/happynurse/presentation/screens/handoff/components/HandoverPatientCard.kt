// 인수인계 환자 카드 — Pager 내부에서 1명을 표현. 토글 아이콘으로 PASS-BAR 펼침.
package com.happynurse.presentation.screens.handoff.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.happynurse.domain.model.HandoverDetail
import com.happynurse.domain.model.Patient
import com.happynurse.domain.model.RosterPatientItem
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors

@Composable
fun HandoverPatientCard(
    item: RosterPatientItem,
    patient: Patient?,
    expanded: Boolean,
    detail: HandoverDetail?,
    loadingDetail: Boolean,
    onToggle: () -> Unit,
) {
    HnCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 14.dp) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    if (patient != null) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                patient.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HnColors.Text,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${patient.sex} · ${patient.age}세",
                                fontSize = 12.sp,
                                color = HnColors.TextSecondary,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        item.header.ifBlank { "AI 요약 준비중입니다." },
                        fontSize = 14.sp,
                        color = HnColors.Text,
                        lineHeight = 20.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                ToggleButton(expanded = expanded, onToggle = onToggle)
            }
            if (item.rulesFiredBrief.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                RulesRow(item.rulesFiredBrief)
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
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
                            PassBarSlotSection(detail.payload, fallbackText = detail.autoSummary)
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
                                "상세 인수인계 정보를 불러오지 못했습니다.",
                                fontSize = 12.sp,
                                color = HnColors.TextTertiary,
                            )
                        }
                    }
                }
            }
        }
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

@Composable
private fun ToggleButton(expanded: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(HnColors.PrimarySoft)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "접기" else "펼치기",
            tint = HnColors.Primary,
            modifier = Modifier.size(18.dp),
        )
    }
}
