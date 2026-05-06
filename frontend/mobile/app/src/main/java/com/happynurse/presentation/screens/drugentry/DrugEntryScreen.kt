// 약물 등록 — NFC 태깅으로 약물 추가, 종류별(수액/주사/알약) 그룹 + 수액은 타이머 설정 진입
package com.happynurse.presentation.screens.drugentry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.components.TagChip
import com.happynurse.presentation.theme.HnColors

enum class DrugKind { FLUID, INJ, PILL }
data class DrugItem(val id: Int, val kind: DrugKind, val name: String, val presc: String)

@Composable
fun DrugEntryScreen(
    onClose: () -> Unit,
    onTimer: () -> Unit,
) {
    val drugs = remember {
        mutableStateListOf(
            DrugItem(1, DrugKind.FLUID, "0.9% 생리식염수 500mL", "24시간 IV, 80 mL/hr"),
            DrugItem(2, DrugKind.FLUID, "5% 포도당 500mL", "혼합 가능"),
            DrugItem(3, DrugKind.INJ,   "세프트리악손 1g", "1g q12h IV"),
            DrugItem(4, DrugKind.PILL,  "아스피린 100mg", "1정 PO qd"),
        )
    }
    var tagging by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                "뒤로",
                modifier = Modifier.size(28.dp).clickable(onClick = onClose),
            )
            Spacer(Modifier.size(8.dp))
            Text("약물 등록", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            HnCard(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(HnColors.PrimarySoft),
                    ) { Icon(Icons.Outlined.Nfc, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("약물 NFC 태그", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                        Text(
                            if (tagging) "태그 인식 중..." else "디바이스를 약물에 가까이 대주세요",
                            fontSize = 12.sp, color = HnColors.TextSecondary,
                        )
                    }
                }
            }
        }

        val fluids = drugs.filter { it.kind == DrugKind.FLUID }
        val injections = drugs.filter { it.kind == DrugKind.INJ }
        val pills = drugs.filter { it.kind == DrugKind.PILL }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        ) {
            item {
                Text("태깅된 약물 (${drugs.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary)
            }
            if (fluids.isNotEmpty()) {
                item { SectionTitle("수액 (혼합 가능) · ${fluids.size}", HnColors.Primary) }
                items(fluids, key = { it.id }) { d -> DrugRow(d, "수액", HnColors.Purple, HnColors.TagFluidBg) { drugs.removeAll { it.id == d.id } } }
                item {
                    HnButton(
                        text = "모든 수액 타이머 설정",
                        variant = com.happynurse.presentation.components.HnButtonVariant.SECONDARY,
                        full = true, icon = Icons.Outlined.Timer, onClick = onTimer,
                    )
                }
            }
            if (injections.isNotEmpty()) {
                item { SectionTitle("주사 · ${injections.size}", HnColors.Warning) }
                items(injections, key = { it.id }) { d -> DrugRow(d, "주사", HnColors.Info, HnColors.TagInjBg) { drugs.removeAll { it.id == d.id } } }
            }
            if (pills.isNotEmpty()) {
                item { SectionTitle("알약 · ${pills.size}", HnColors.Success) }
                items(pills, key = { it.id }) { d -> DrugRow(d, "알약", HnColors.Success, HnColors.TagPillBg) { drugs.removeAll { it.id == d.id } } }
            }
        }
        Box(Modifier.fillMaxWidth().padding(20.dp)) {
            HnButton(
                text = "웹 전송 (${drugs.size})",
                icon = Icons.Outlined.Send,
                full = true,
                enabled = drugs.isNotEmpty(),
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun DrugRow(d: DrugItem, label: String, fg: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color, onRemove: () -> Unit) {
    HnCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                TagChip(label, fg = fg, bg = bg)
                Spacer(Modifier.height(6.dp))
                Text(d.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Text(d.presc, fontSize = 12.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .background(HnColors.Surface).clickable(onClick = onRemove),
            ) { Icon(Icons.Outlined.Close, "삭제", tint = HnColors.TextSecondary, modifier = Modifier.size(14.dp)) }
        }
    }
}
