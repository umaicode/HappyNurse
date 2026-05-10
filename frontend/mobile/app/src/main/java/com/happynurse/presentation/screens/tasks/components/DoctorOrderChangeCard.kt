// 의사오더변경 카드 — 백엔드 변경 API 추가 시 데이터로 채워짐. 클릭 시 환자상세 의사오더 탭으로 이동.
package com.happynurse.presentation.screens.tasks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.DoctorOrderChange
import com.happynurse.domain.model.OrderKind
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DoctorOrderChangeCard(
    change: DoctorOrderChange,
    onClick: () -> Unit,
) {
    HnCard(padding = 14.dp, onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(change.patientName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    "${change.room}호 ${change.bed}",
                    fontSize = 12.sp,
                    color = HnColors.TextSecondary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatChangedAt(change.changedAtIso),
                    fontSize = 12.sp,
                    color = HnColors.TextSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OrderKindChip(change.orderKind)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    change.orderName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HnColors.Text,
                )
            }
        }
    }
}

@Composable
private fun OrderKindChip(kind: OrderKind) {
    val (bg, fg, label) = when (kind) {
        OrderKind.INJ -> Triple(HnColors.TagInjBg, HnColors.TagInjFg, "투약")
        OrderKind.FLUID -> Triple(HnColors.TagFluidBg, HnColors.TagFluidFg, "수액")
        OrderKind.ORDER -> Triple(HnColors.TagOrderBg, HnColors.TagOrderFg, "지시")
        OrderKind.LIS -> Triple(HnColors.TagLisBg, HnColors.TagLisFg, "LIS")
        OrderKind.IMG -> Triple(HnColors.TagImgBg, HnColors.TagImgFg, "영상")
        OrderKind.PILL -> Triple(HnColors.TagPillBg, HnColors.TagPillFg, "알약")
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

private val DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("M.d HH:mm")

private fun formatChangedAt(iso: String): String {
    if (iso.isBlank()) return "-"
    val ldt = runCatching { OffsetDateTime.parse(iso).toLocalDateTime() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(iso) }.getOrNull()
    return ldt?.format(DISPLAY_FORMATTER) ?: "-"
}
