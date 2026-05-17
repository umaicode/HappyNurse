// 인수인계 환자 카드 상단 — synthesis(종합) 슬롯 전용 체크리스트.
// 서버 영속(GET /handover/{id}, PATCH /handover/{id}/checks). by/at 메타 표시.
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.CheckMeta
import com.happynurse.domain.model.SlotItem
import com.happynurse.presentation.theme.HnColors

@Composable
fun SynthesisChecklist(
    items: List<SlotItem>,
    checkedMap: Map<Int, CheckMeta>?,
    inFlight: Set<Int>,
    onToggle: (index: Int, newValue: Boolean) -> Unit,
) {
    if (items.isEmpty()) return
    val accent = HnColors.Success
    val bg = accent.copy(alpha = 0.04f)
    val done = checkedMap?.count { (idx, _) -> idx in items.indices } ?: 0

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.TaskAlt,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "체크리스트",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$done / ${items.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = HnColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (checkedMap == null) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 1.6.dp,
                    color = accent,
                )
            }
        } else {
            items.forEachIndexed { idx, item ->
                if (idx > 0) Spacer(Modifier.height(6.dp))
                ChecklistRow(
                    label = item.value.orEmpty(),
                    checked = idx in checkedMap,
                    meta = checkedMap[idx],
                    loading = idx in inFlight,
                    accent = accent,
                    onToggle = { onToggle(idx, idx !in checkedMap) },
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    checked: Boolean,
    meta: CheckMeta?,
    loading: Boolean,
    accent: Color,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !loading) { onToggle() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 1.6.dp,
                    color = accent,
                )
            } else {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (checked) accent else HnColors.Surface)
                        .border(
                            BorderStroke(1.5.dp, if (checked) accent else HnColors.BorderStrong),
                            RoundedCornerShape(5.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (checked) {
                        Icon(
                            Icons.Outlined.Check,
                            null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (checked) HnColors.TextTertiary else HnColors.Text,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                lineHeight = 20.sp,
            )
            if (checked && meta != null) {
                val sub = listOfNotNull(
                    meta.by.takeIf { it.isNotBlank() },
                    formatShortIso(meta.at).takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(
                        sub,
                        fontSize = 12.sp,
                        color = HnColors.TextTertiary,
                    )
                }
            }
        }
    }
}

// "2026-05-13T09:15:00..." → "05/13 09:15"
private fun formatShortIso(iso: String): String {
    if (iso.isBlank()) return ""
    return try {
        val t = iso.substringBefore('+').substringBefore('Z')
        val datePart = t.substringBefore('T')
        val timePart = t.substringAfter('T', missingDelimiterValue = "")
        val md = datePart.substringAfter('-', missingDelimiterValue = datePart).replace('-', '/')
        val hm = timePart.take(5)
        if (hm.isBlank()) md else "$md $hm"
    } catch (_: Throwable) {
        iso
    }
}
