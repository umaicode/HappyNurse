// 수액 타이머 설정 — verify 통과 처방 + 용량 + 속도 + 환자 타입 → POST /iv/start → active 화면 전환
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.runtime.remember
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors
import kotlinx.coroutines.delay

@Composable
fun IVTimerSetupScreen(
    encounterId: Long,
    medicationOrderIds: List<Long>,
    onClose: () -> Unit,
    onActive: (Long) -> Unit,
    viewModel: IvTimerSetupViewModel = hiltViewModel(),
) {
    var volume by remember { mutableIntStateOf(1000) }
    var rate by remember { mutableIntStateOf(20) }
    var patientType by remember { mutableStateOf(PatientType.ADULT) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val patientName by viewModel.patientName.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()

    LaunchedEffect(encounterId) { viewModel.loadContext(encounterId) }

    // 처방의 dose 합계가 max. 모든 선택된 처방의 doseMl 정보가 있을 때만 검증.
    val maxVolumeMl: Int? = remember(medicationOrderIds, orders) {
        if (medicationOrderIds.isEmpty()) return@remember null
        val doses = medicationOrderIds.map { orders[it]?.doseMl }
        if (doses.any { it == null }) null
        else doses.filterNotNull().sum()
    }
    val volumeExceeded = maxVolumeMl != null && volume > maxVolumeMl

    val gttPerMl = patientType.gttPerMl
    val mlPerHr = if (gttPerMl > 0) rate * 60.0 / gttPerMl else 0.0
    val totalMin = if (mlPerHr > 0) (volume * 60.0 / mlPerHr).toInt() else 0
    val h = totalMin / 60
    val m = totalMin % 60

    val submitting = state is IvTimerSetupViewModel.SetupState.Submitting
    val success = state as? IvTimerSetupViewModel.SetupState.Success
    val error = state as? IvTimerSetupViewModel.SetupState.Error

    // 1.5s delay → active 화면 replace
    LaunchedEffect(success) {
        val infusion = success?.infusion ?: return@LaunchedEffect
        delay(1500)
        onActive(infusion.ivInfusionId)
    }

    Column(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "뒤로",
                modifier = Modifier.size(28.dp).clickable(enabled = !submitting && success == null, onClick = onClose),
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
                    Text("환자", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextTertiary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        patientName ?: "—",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HnColors.Text,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("선택된 처방 (${medicationOrderIds.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextTertiary)
                    Spacer(Modifier.height(4.dp))
                    if (medicationOrderIds.isEmpty()) {
                        Text("—", fontSize = 13.sp, color = HnColors.TextSecondary)
                    } else {
                        medicationOrderIds.forEachIndexed { idx, id ->
                            val info = orders[id]
                            val name = info?.orderName ?: "처방 #$id"
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("${idx + 1}.", fontSize = 12.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(end = 6.dp))
                                Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text, modifier = Modifier.weight(1f))
                                if (info?.doseMl != null) {
                                    Text("${info.doseMl}mL", fontSize = 11.sp, color = HnColors.TextTertiary)
                                } else {
                                    Text("#$id", fontSize = 11.sp, color = HnColors.TextTertiary)
                                }
                            }
                        }
                        if (maxVolumeMl != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "처방 총량: ${maxVolumeMl} mL",
                                fontSize = 11.sp,
                                color = HnColors.TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            HnCard {
                Column {
                    LabelRow("환자 타입", "(점적 계수)")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PatientType.entries.forEach { p ->
                            PresetChip(
                                label = "${p.label} (${p.gttPerMl}gtt/mL)",
                                active = patientType == p,
                                onClick = { patientType = p },
                            )
                        }
                    }
                }
            }

            HnCard {
                Column {
                    LabelRow("용량", "(mL)")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (volume == 0) "" else volume.toString(),
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(5)
                            volume = digits.toIntOrNull() ?: 0
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text("직접 입력", color = HnColors.TextTertiary, fontSize = 14.sp) },
                        suffix = { Text("mL", fontSize = 14.sp, color = HnColors.TextSecondary) },
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = HnColors.Primary,
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = HnColors.Surface,
                            unfocusedContainerColor = HnColors.Surface,
                            focusedIndicatorColor = HnColors.Primary,
                            unfocusedIndicatorColor = HnColors.Border,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(100, 500, 1000, 2000).forEach { v ->
                            PresetChip(v.toString(), volume == v) { volume = v }
                        }
                    }
                    if (volumeExceeded && maxVolumeMl != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = HnColors.Danger,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                "처방 총량 ${maxVolumeMl} mL 를 초과합니다",
                                fontSize = 12.sp,
                                color = HnColors.Danger,
                                fontWeight = FontWeight.SemiBold,
                            )
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
                        valueRange = 1f..100f,
                        steps = 98,
                        colors = SliderDefaults.colors(
                            thumbColor = HnColors.Primary,
                            activeTrackColor = HnColors.Primary,
                            inactiveTrackColor = HnColors.Border,
                        ),
                    )
                }
            }

            // 로컬 예상 시간 미리보기 (백엔드 응답으로 대체될 예정)
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF5C6BC0), // Primary 의 라이트 인디고 변형
                                Color(0xFF3F51B5), // PrimaryDark 의 라이트 인디고 변형
                            ),
                        ),
                    )
                    .padding(16.dp),
            ) {
                Column {
                    Text("예상 소요 시간", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(4.dp))
                    Text("${h}시간 ${m}분", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row {
                            Stat("총 용량", "${volume} mL", Modifier.weight(1f))
                            Stat("속도", "%.1f mL/hr".format(mlPerHr), Modifier.weight(1f))
                            Stat("타입", patientType.label, Modifier.weight(1f))
                        }
                    }
                }
            }

            if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HnColors.Surface)
                        .border(1.dp, HnColors.Danger, RoundedCornerShape(10.dp))
                        .clickable(onClick = viewModel::resetError)
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = HnColors.Danger, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(error.message, fontSize = 13.sp, color = HnColors.Text, modifier = Modifier.weight(1f))
                        Text("닫기", fontSize = 12.sp, color = HnColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            GradientPrimaryButton(
                text = if (success != null) "설정 완료" else "수액 타이머 설정",
                icon = if (success != null) Icons.Outlined.CheckCircle else Icons.Outlined.Timer,
                enabled = !submitting && success == null && medicationOrderIds.isNotEmpty()
                    && volume > 0 && !volumeExceeded,
                loading = submitting,
                onClick = {
                    viewModel.start(
                        encounterId = encounterId,
                        medicationOrderIds = medicationOrderIds,
                        totalVolumeMl = volume.toDouble(),
                        rateGttPerMin = rate,
                        patientType = patientType,
                    )
                },
            )
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

@Composable
private fun StatLight(label: String, value: String, mod: Modifier) {
    Column(mod) {
        Text(label, fontSize = 11.sp, color = HnColors.TextTertiary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
    }
}

@Composable
private fun GradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF5C6BC0), Color(0xFF3F51B5)),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (enabled) gradient else Brush.linearGradient(listOf(Color(0xFFD1D5DB), Color(0xFFD1D5DB))))
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(" ", color = Color.White)
            }
            Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
