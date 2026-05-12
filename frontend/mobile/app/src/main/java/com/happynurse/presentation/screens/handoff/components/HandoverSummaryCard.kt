// 인수인계 상단 — 시프트 전체 narrative + 환자 카운트/주의 환자수 칩.
package com.happynurse.presentation.screens.handoff.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    HnCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HnColors.PrimaryLight)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "AI 통합 요약",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HnColors.Primary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val text = summary.narrativeHeader.ifBlank {
                "이번 시프트 담당 환자 ${summary.patientCount}명에 대한 요약입니다."
            }
            Text(
                text,
                fontSize = 14.sp,
                color = HnColors.Text,
                lineHeight = 20.sp,
            )
            if (summary.watcherCount > 0) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(
                        icon = { Icon(Icons.Outlined.Notifications, null, tint = HnColors.TagWatchStrongFg, modifier = Modifier.size16()) },
                        label = "주의 ${summary.watcherCount}명",
                        bg = HnColors.TagWatchStrongBg,
                        fg = HnColors.TagWatchStrongFg,
                    )
                }
            }
        }
    }
}

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
