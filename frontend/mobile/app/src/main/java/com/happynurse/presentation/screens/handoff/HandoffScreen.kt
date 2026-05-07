// 인수인계 탭 — 상단 AI 요약 카드(그라데이션) + 환자별 인계(아코디언, 체크리스트)
package com.happynurse.presentation.screens.handoff

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.core.sample.SampleData
import com.happynurse.domain.model.HandoffItem
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.components.NotifBell
import com.happynurse.presentation.components.PageHeader
import com.happynurse.presentation.components.TagChip
import com.happynurse.presentation.theme.HnColors

@Composable
fun HandoffScreen(
    onOpenNotifications: () -> Unit,
    upcomingCount: Int,
) {
    Column(Modifier.fillMaxWidth()) {
        PageHeader(
            title = "인수인계",
            sub = "데이 → 이브닝",
            right = { NotifBell(unreadCount = upcomingCount, onClick = onOpenNotifications) },
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            item { AiSummaryCard() }
            item {
                Text(
                    "담당 환자별 인계",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = HnColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(SampleData.handoffs, key = { it.id }) { HandoffCard(it) }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun AiSummaryCard() {
    var open by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(HnColors.Primary, HnColors.PrimaryDark)),
            )
            .padding(18.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.size(8.dp))
                Text("AI 인계 요약", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("14:30 생성", color = HnColors.Text, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.2f)),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = !open },
            ) {
                Text("상세 보기", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            if (open) {
                Spacer(Modifier.height(10.dp))
                val summaryLines = remember {
                    SampleData.handoffs.flatMap { it.aiSummary }.take(3)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summaryLines.forEach { line ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        ) {
                            Text(line, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HandoffCard(h: HandoffItem) {
    var open by remember { mutableStateOf(false) }
    val checks = remember { mutableStateMapOf<Int, Boolean>().also { m -> h.checklist.forEachIndexed { i, c -> m[i] = c.done } } }

    HnCard(padding = 0.dp) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open = !open }
                    .padding(14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(h.patient, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                        Spacer(Modifier.size(6.dp))
                        TagChip(h.room, fg = HnColors.Primary, bg = HnColors.PrimarySoft)
                    }
                    Text(h.tag, fontSize = 12.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                }
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = HnColors.TextSecondary)
            }
            if (open) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(HnColors.SurfaceAlt)
                            .padding(12.dp),
                    ) { Text(h.note, fontSize = 13.sp, color = HnColors.Text) }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(HnColors.PrimarySoft)
                            .border(1.dp, HnColors.PrimaryLight, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        Column {
                            Text("AI 요약", color = HnColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            h.aiSummary.forEach {
                                Text("• $it", fontSize = 13.sp, color = HnColors.Text)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .padding(8.dp),
                            ) {
                                Icon(Icons.Outlined.Warning, contentDescription = null, tint = HnColors.Danger, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(h.warnings, fontSize = 12.sp, color = HnColors.Danger)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("✅ 체크리스트", color = HnColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            h.checklist.forEachIndexed { i, c ->
                                val done = checks[i] ?: c.done
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(HnColors.Surface)
                                        .clickable { checks[i] = !done }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (done) HnColors.Primary else HnColors.Surface)
                                            .border(1.5.dp, if (done) HnColors.Primary else HnColors.BorderStrong, RoundedCornerShape(4.dp)),
                                    ) {
                                        if (done) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        c.text,
                                        fontSize = 13.sp,
                                        color = if (done) HnColors.TextTertiary else HnColors.Text,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
