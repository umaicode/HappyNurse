// 환자 리스트 카드 — card/compact/rich 3가지 변형. 기본값은 card
package com.happynurse.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.domain.model.Patient
import com.happynurse.presentation.theme.HnColors

enum class PatientLayout { CARD, COMPACT, RICH }

@Composable
fun PatientCard(
    patient: Patient,
    onClick: () -> Unit,
    layout: PatientLayout = PatientLayout.CARD,
    myNurseName: String = "김소연",
) {
    when (layout) {
        PatientLayout.COMPACT -> CompactRow(patient, onClick)
        PatientLayout.RICH    -> RichRow(patient, onClick, myNurseName)
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
                Text(p.chief, fontSize = 13.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 3.dp))
                Row(modifier = Modifier.padding(top = 3.dp)) {
                    Text("담당 ", fontSize = 12.sp, color = HnColors.TextTertiary)
                    Text(
                        p.nurse,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (p.nurse == myNurseName) HnColors.Primary else HnColors.TextSecondary,
                    )
                    Text(" 간호사", fontSize = 12.sp, color = HnColors.TextTertiary)
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
            Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            Spacer(Modifier.size(8.dp))
            Text("D+${p.daysSince}", fontSize = 12.sp, color = HnColors.TextSecondary)
            Spacer(Modifier.weight(1f))
            Text(p.chief, fontSize = 12.sp, color = HnColors.TextSecondary)
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = HnColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RichRow(p: Patient, onClick: () -> Unit, myNurseName: String) {
    HnCard(onClick = onClick) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                        Spacer(Modifier.size(6.dp))
                        TagChip("${p.sex}/${p.age}")
                    }
                    Text("${p.chief} · D+${p.daysSince}", fontSize = 12.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
                    Row(modifier = Modifier.padding(top = 2.dp)) {
                        Text("담당 ", fontSize = 12.sp, color = HnColors.TextTertiary)
                        Text(
                            p.nurse,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (p.nurse == myNurseName) HnColors.Primary else HnColors.TextSecondary,
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = HnColors.TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                VitalCell("BP", p.vitals.bp)
                VitalCell("HR", p.vitals.hr.toString())
                VitalCell("T°", p.vitals.temp)
                VitalCell("SpO₂", "${p.vitals.spo2}%")
            }
        }
    }
}

@Composable
private fun VitalCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = HnColors.TextTertiary, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
    }
}
