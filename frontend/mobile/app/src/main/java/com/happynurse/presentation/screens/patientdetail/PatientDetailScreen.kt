// 환자 상세 — 환자 정보 카드(접기/펼치기), 간호일지/의사오더 서브탭
package com.happynurse.presentation.screens.patientdetail

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.ExpandMore
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
import com.happynurse.core.sample.SampleData
import com.happynurse.domain.model.Note
import com.happynurse.domain.model.Order
import com.happynurse.domain.model.OrderKind
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.components.TagChip
import com.happynurse.presentation.theme.HnColors

@Composable
fun PatientDetailScreen(
    patientId: String,
    onBack: () -> Unit,
) {
    val p = remember(patientId) { SampleData.patients.firstOrNull { it.id == patientId } ?: SampleData.patients.first() }
    var expanded by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("notes") }

    Column(Modifier.fillMaxWidth().background(HnColors.Bg)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "뒤로",
                modifier = Modifier.size(28.dp).clickable(onClick = onBack),
            )
            Spacer(Modifier.size(4.dp))
            Text(p.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            Spacer(Modifier.size(8.dp))
            Text(
                "${if (p.sex == "여") "F" else "M"}/${p.age}",
                fontSize = 14.sp,
                color = HnColors.TextSecondary,
                fontWeight = FontWeight.Medium,
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            item {
                HnCard(padding = 14.dp) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("MRN: ${p.mrn}", fontSize = 13.sp, color = HnColors.TextSecondary)
                                Text("${p.room}호 ${p.bed}번 침대", fontSize = 13.sp, color = HnColors.TextSecondary)
                            }
                            Icon(
                                Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = HnColors.TextSecondary,
                                modifier = Modifier.size(22.dp).clickable { expanded = !expanded },
                            )
                        }
                        if (expanded) {
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(HnColors.Border))
                            Spacer(Modifier.height(12.dp))
                            InfoRow("생년월일", p.birthdate)
                            InfoRow("진료부서", p.department)
                            InfoRow("담당의", "${p.doctor} 의사")
                            InfoRow("주증상", p.chief)
                            InfoRow("수술", p.surgery)
                            Spacer(Modifier.height(8.dp))
                            Text("메모", fontSize = 12.sp, color = HnColors.TextTertiary)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HnColors.SurfaceAlt)
                                    .padding(10.dp),
                            ) { Text(p.memo, fontSize = 13.sp, color = HnColors.Text) }
                        }
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().background(HnColors.Surface, RoundedCornerShape(10.dp))) {
                    listOf("notes" to "간호일지", "orders" to "의사오더").forEach { (id, label) ->
                        val on = id == tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clickable { tab = id }.height(44.dp),
                        ) {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (on) HnColors.Primary else HnColors.TextSecondary,
                                )
                            }
                            Box(
                                Modifier.fillMaxWidth().height(2.dp)
                                    .background(if (on) HnColors.Primary else Color.Transparent),
                            )
                        }
                    }
                }
            }
            if (tab == "notes") {
                items(p.notes) { NoteRow(it) }
            } else {
                items(p.orders) { OrderRow(it) }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = HnColors.TextTertiary, modifier = Modifier.size(width = 64.dp, height = 18.dp))
        Text(value, fontSize = 14.sp, color = HnColors.Text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun NoteRow(n: Note) {
    HnCard(padding = 12.dp) {
        Column {
            val validTags = n.tags.filter { it == "투약" || it == "STT" }
            if (validTags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    validTags.forEach { t ->
                        if (t == "투약") TagChip("투약", fg = HnColors.Info, bg = HnColors.TagInjBg)
                        else TagChip("STT", fg = HnColors.Purple, bg = HnColors.TagFluidBg)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
                Spacer(Modifier.size(8.dp))
                Text(n.author, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(n.text, fontSize = 14.sp, color = HnColors.Text)
        }
    }
}

@Composable
private fun OrderRow(o: Order) {
    val (label, fg, bg) = when (o.kind) {
        OrderKind.INJ   -> Triple("투약", HnColors.Info,          HnColors.TagInjBg)
        OrderKind.FLUID -> Triple("수액", HnColors.Purple,        HnColors.TagFluidBg)
        OrderKind.ORDER -> Triple("지시", HnColors.TextSecondary, HnColors.TagOrderBg)
        OrderKind.LIS   -> Triple("LIS",  HnColors.Warning,       HnColors.TagLisBg)
        OrderKind.IMG   -> Triple("영상", HnColors.Cyan,          HnColors.TagImgBg)
        OrderKind.PILL  -> Triple("알약", HnColors.Success,       HnColors.TagPillBg)
    }
    HnCard(padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TagChip(label, fg = fg, bg = bg)
                Spacer(Modifier.size(8.dp))
                Text(o.code, fontSize = 12.sp, color = HnColors.TextTertiary)
            }
            Spacer(Modifier.height(8.dp))
            Text(o.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(HnColors.SurfaceAlt).padding(10.dp),
            ) {
                Column {
                    GridCell("1회량", o.dose); GridCell("횟수", o.freq); GridCell("단위", o.unit); GridCell("용법", o.usage)
                }
            }
            if (o.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("참고: ${o.note}", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun GridCell(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontSize = 11.sp, color = HnColors.TextTertiary, modifier = Modifier.size(width = 56.dp, height = 16.dp))
        Text(value, fontSize = 13.sp, color = HnColors.Text, fontWeight = FontWeight.SemiBold)
    }
}
