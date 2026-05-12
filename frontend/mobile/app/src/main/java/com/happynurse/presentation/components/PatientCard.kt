// 환자 리스트 카드 — card / compact 두 가지 변형. 기본값은 card
// TagChip 공용 컴포넌트도 이 파일에서 정의 (같은 패키지이므로 별도 import 불필요)
package com.happynurse.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
import com.happynurse.domain.model.Patient
import com.happynurse.presentation.theme.HnColors

// ── TagChip ──────────────────────────────────────────────────────────────────
@Composable
fun TagChip(
    text: String,
    fg: Color = Color(0xFF2D2D2D),
    bg: Color = Color(0xFFF5F5F5),
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}
// ─────────────────────────────────────────────────────────────────────────────

enum class PatientLayout { CARD, COMPACT }

@Composable
fun PatientCard(
    patient: Patient,
    onClick: () -> Unit,
    layout: PatientLayout = PatientLayout.CARD,
    myNurseName: String = "",
) {
    when (layout) {
        PatientLayout.COMPACT -> CompactRow(patient, onClick)
        else                  -> CardRow(patient, onClick, myNurseName)
    }
}

@Composable
private fun CardRow(p: Patient, onClick: () -> Unit, myNurseName: String) {
    HnCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    Spacer(Modifier.size(6.dp))
                    TagChip("${p.sex}/${p.age}")
                }
                Text(
                    p.surgery.ifBlank { "-" },
                    fontSize = 16.sp,
                    color = HnColors.TextSecondary,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Row(modifier = Modifier.padding(top = 3.dp)) {
                    Text("담당 ", fontSize = 14.sp, color = HnColors.TextTertiary)
                    Text(
                        p.nurse,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (p.nurse == myNurseName) HnColors.Primary else HnColors.TextSecondary,
                    )
                    Text(" 간호사", fontSize = 14.sp, color = HnColors.TextTertiary)
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = HnColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CompactRow(p: Patient, onClick: () -> Unit) {
    HnCard(onClick = onClick, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            Spacer(Modifier.size(8.dp))
            Text("D+${p.daysSince}", fontSize = 12.sp, color = HnColors.TextSecondary)
            Spacer(Modifier.weight(1f))
            Text(p.surgery.ifBlank { "-" }, fontSize = 14.sp, color = HnColors.TextSecondary)
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = HnColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
