// 간호 일지 타임라인 아이템 — 8개 카테고리(V/S, 투약, 처치, 식이 등) 아이콘+텍스트 행
package com.happynurse.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class JournalCategory(val label: String, val colors: Pair<Color, Color>) {
    VitalSign("V/S", TagColors.VitalSign),
    Medication("투약", TagColors.Medication),
    Treatment("처치", TagColors.Treatment),
    Diet("식이", TagColors.Diet),
    Observation("관찰", TagColors.Observation),
    Excretion("배설", TagColors.Excretion),
    Sleep("수면", TagColors.Sleep),
    Activity("활동", TagColors.Activity)
}

data class JournalEntry(
    val time: String,
    val category: JournalCategory,
    val title: String,
    val description: String
)

@Composable
fun JournalTimelineItem(entry: JournalEntry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = entry.time,
            fontSize = 13.sp,
            color = Color(0xFF666666),
            modifier = Modifier.width(52.dp).padding(top = 12.dp)
        )
        Spacer(Modifier.width(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FB))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagChip(
                        label = entry.category.label,
                        background = entry.category.colors.first,
                        foreground = entry.category.colors.second
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(entry.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(2.dp))
                Text(entry.description, fontSize = 13.sp, color = Color(0xFF444444))
            }
        }
    }
}
