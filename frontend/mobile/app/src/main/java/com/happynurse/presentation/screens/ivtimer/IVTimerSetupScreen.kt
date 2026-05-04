// 수액 타이머 설정 — 용량/세트/주입속도 입력 후 종료시간 계산
package com.happynurse.presentation.screens.ivtimer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors

@Composable
fun IVTimerSetupScreen(
    drugName: String = "0.9% 생리식염수 1L",
    onClose: () -> Unit,
) {
    var volume by remember { mutableIntStateOf(1000) }
    var dropSet by remember { mutableIntStateOf(15) }
    var rate by remember { mutableIntStateOf(20) }
    var calculated by remember { mutableStateOf(false) }

    val totalMin = if (rate > 0) (volume.toFloat() / rate * 60f).toInt() else 0
    val h = totalMin / 60
    val m = totalMin % 60
    val endTime = run {
        // 09:00 기준
        val end = 9 * 60 + totalMin
        "%02d:%02d".format((end / 60) % 24, end % 60)
    }

    Column(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "뒤로",
                modifier = Modifier.size(28.dp).clickable(onClick = onClose),
            )
            Spacer(Modifier.size(8.dp))
            Text("수액 타이머 설정", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HnCard {
                Column {
                    Text("수액명", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextTertiary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = HnColors.Purple, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(drugName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    }
                }
            }

            HnCard {
                Column {
                    LabelRow("용량", "(mL)")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            volume.toString(),
                            fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            color = HnColors.Primary,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("mL", fontSize = 14.sp, color = HnColors.TextSecondary)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(500, 1000, 2000).forEach { v ->
                            PresetChip(v.toString(), volume == v) { volume = v }
                        }
                    }
                }
            }

            HnCard {
                Column {
                    LabelRow("수액세트", "(gtt/mL)")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10 to "10", 15 to "15", 20 to "20", 60 to "60 (소아)").forEach { (v, label) ->
                            PresetChip(label, dropSet == v) { dropSet = v }
                        }
                    }
                }
            }

            HnCard {
                Column {
                    LabelRow("주입 속도", "(gtt/min)")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepBtn(Icons.Outlined.Remove) { rate = (rate - 1).coerceAtLeast(1) }
                        Spacer(Modifier.weight(1f))
                        Text(rate.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
                        Spacer(Modifier.weight(1f))
                        StepBtn(Icons.Outlined.Add) { rate += 1 }
                    }
                    Spacer(Modifier.height(10.dp))
                    Slider(
                        value = rate.toFloat(),
                        onValueChange = { rate = it.toInt() },
                        valueRange = 1f..50f,
                        steps = 48,
                        colors = SliderDefaults.colors(
                            thumbColor = HnColors.Primary,
                            activeTrackColor = HnColors.Primary,
                            inactiveTrackColor = HnColors.Border,
                        ),
                    )
                }
            }

            HnButton(text = "계산", full = true, icon = Icons.Outlined.Timer, onClick = { calculated = true })

            if (calculated) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(HnColors.Primary, HnColors.PrimaryDark)))
                        .padding(16.dp),
                ) {
                    Column {
                        Text("예상 종료 시간", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(4.dp))
                        Text(endTime, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("오늘 종료 예정", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(14.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Row {
                                Stat("소요 시간", "${h}시간 ${m}분", Modifier.weight(1f))
                                Stat("총 용량", "${volume} mL", Modifier.weight(1f))
                                Stat("속도", "${rate} mL/hr", Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HnButton(text = "수액 타이머 설정", full = true, icon = Icons.Outlined.Timer, onClick = onClose)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LabelRow(title: String, suffix: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
        Text(" $suffix", fontSize = 13.sp, color = HnColors.TextTertiary)
    }
}

@Composable
private fun PresetChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) HnColors.PrimarySoft else HnColors.Surface)
            .border(1.dp, if (active) HnColors.Primary else HnColors.Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (active) HnColors.Primary else HnColors.TextSecondary)
    }
}

@Composable
private fun StepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(HnColors.Surface)
            .border(1.dp, HnColors.Primary, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
    ) { Icon(icon, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun Stat(label: String, value: String, mod: Modifier) {
    Column(mod) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
