// 인수인계 상단 — AI 통합 요약. 헤더에 토글이 있어 펼침/접힘이 가능.
package com.happynurse.presentation.screens.handoff.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.RosterSummary
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors

@Composable
fun HandoverSummaryCard(summary: RosterSummary) {
    var expanded by remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }

    HnCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) { expanded = !expanded },
            ) {
                Text(
                    "AI 통합 요약",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HnColors.Primary,
                )
                Box(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(HnColors.PrimarySoft),
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

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(10.dp))
                    val raw = summary.narrativeHeader.ifBlank {
                        "이번 시프트 담당 환자 ${summary.patientCount}명에 대한 요약입니다."
                    }
                    // 마침표/물음표/느낌표 뒤에 줄바꿈을 넣어 문장 단위로 줄을 분리.
                    val text = raw.splitSentences()
                    Text(
                        text,
                        fontSize = 16.sp,
                        color = HnColors.Text,
                        lineHeight = 20.sp,
                    )
                    if (summary.watcherCount > 0) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatChip(
                                icon = {
                                    Icon(
                                        Icons.Outlined.Notifications,
                                        null,
                                        tint = HnColors.TagWatchStrongFg,
                                        modifier = Modifier.size16(),
                                    )
                                },
                                label = "주의 ${summary.watcherCount}명",
                                bg = HnColors.TagWatchStrongBg,
                                fg = HnColors.TagWatchStrongFg,
                            )
                        }
                    }
                }
            }
        }
    }
}

// "문장. 다음 문장." → "문장.\n다음 문장." — 이미 들어있는 \n 도 보존.
private fun String.splitSentences(): String =
    this.replace(Regex("([.!?。])\\s+"), "$1\n").trim()

@Composable
private fun StatChip(
    icon: @Composable () -> Unit,
    label: String,
    bg: Color,
    fg: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

private fun Modifier.size16(): Modifier = this.then(Modifier.height(14.dp).width(14.dp))
