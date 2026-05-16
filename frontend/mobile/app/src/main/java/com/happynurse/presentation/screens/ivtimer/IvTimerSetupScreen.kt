// 수액 타이머 설정 — verify 통과 처방 + 용량 + 속도 + 환자 타입 → POST /iv/start → active 화면 전환
package com.happynurse.presentation.screens.ivtimer

import android.R
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors
import kotlinx.coroutines.delay

@Composable
fun IvTimerSetupScreen(
    encounterId: Long,
    medicationOrderIds: List<Long>,
    onClose: () -> Unit,
    onActive: (Long) -> Unit,
    viewModel: IvTimerSetupViewModel = hiltViewModel(),
) {
    var volume by remember { mutableIntStateOf(1000) }
    var rate by remember { mutableIntStateOf(20) }
    var patientType by remember { mutableStateOf(PatientType.SET_20) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val patientName by viewModel.patientName.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()

    LaunchedEffect(encounterId) { viewModel.loadContext(encounterId) }

    // 처방의 dose 합계가 max. mL 단위 처방만 합산해 부분 검증.
    val maxVolumeMl: Int? = remember(medicationOrderIds, orders) {
        if (medicationOrderIds.isEmpty()) null
        else medicationOrderIds.mapNotNull { orders[it]?.doseMl }
            .takeIf { it.isNotEmpty() }
            ?.sum()
    }
    val volumeExceeded = maxVolumeMl != null && volume > maxVolumeMl

    val gttPerMl = patientType.gttPerMl
    val mlPerHr = if (gttPerMl > 0) rate * 60.0 / gttPerMl else 0.0
    val totalMin = if (mlPerHr > 0) (volume * 60.0 / mlPerHr).toInt() else 0
    val h = totalMin / 60
    val m = totalMin % 60
    // 예상 종료시각 — 현재 시각 + totalMin. 백엔드 expectedEndAt 은 start 호출 후에야 오므로 setup 미리보기는 클라이언트 계산.
    val expectedEndStr: String = remember(totalMin) {
        if (totalMin > 0) LocalTime.now().plusMinutes(totalMin.toLong()).format(DateTimeFormatter.ofPattern("HH:mm"))
        else "—"
    }

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
                modifier = Modifier.size(46.dp).clickable(enabled = !submitting && success == null, onClick = onClose),
            )
        }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            HnCard {
                Column {
                    if (medicationOrderIds.isEmpty()) {
                        Text("—", fontSize = 13.sp, color = HnColors.TextSecondary)
                    } else {
                        medicationOrderIds.forEachIndexed { idx, id ->
                            val info = orders[id]
                            val name = info?.orderName ?: "처방코드 $id"
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text, modifier = Modifier.weight(1f))
                                if (info?.doseMl != null) {
                                    Text("${info.doseMl}mL", fontSize = 12.sp, color = HnColors.TextTertiary)
                                } else {
                                    Text(info?.orderCode ?: "—", fontSize = 14.sp, color = HnColors.TextSecondary)
                                }
                            }
                        }
                        if (maxVolumeMl != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "처방 총량: ${maxVolumeMl} mL",
                                fontSize = 12.sp,
                                color = HnColors.Primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            HnCard {
                Column {
                    LabelRow("수액 세트", "(gtt/mL)")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PatientType.entries.forEach { p ->
                            PresetChip(
                                label = p.gttPerMl.toString(),
                                active = patientType == p,
                                icon = if (p == PatientType.SET_60) Icons.Outlined.ChildCare else null,
                                modifier = Modifier.weight(1f),
                                onClick = { patientType = p },
                            )
                        }
                    }
                }
            }

            HnCard {
                Column {
                    LabelRow("용량", "(mL)")
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = if (volume == 0) "" else volume.toString(),
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(5)
                            volume = digits.toIntOrNull() ?: 0
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        placeholder = { Text("직접 입력", color = HnColors.TextTertiary, fontSize = 16.sp) },
                        suffix = { Text("mL", fontSize = 18.sp, color = HnColors.TextSecondary) },
                        textStyle = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HnColors.Primary,
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = HnColors.Surface,
                            unfocusedContainerColor = HnColors.Surface,
                            focusedIndicatorColor = HnColors.Primary,
                            unfocusedIndicatorColor = HnColors.Border,
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(100, 500, 1000, 2000).forEach { v ->
                            PresetChip(
                                label = v.toString(),
                                active = volume == v,
                                modifier = Modifier.weight(1f),
                                onClick = { volume = v },
                            )
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
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        StepBtn(Icons.Outlined.Remove) { rate = (rate - 1).coerceAtLeast(1) }
                        Spacer(Modifier.size(28.dp))
                        Box(
                            modifier = Modifier.widthIn(min = 80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                rate.toString(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = HnColors.Primary,
                            )
                        }
                        Spacer(Modifier.size(28.dp))
                        StepBtn(Icons.Outlined.Add) { rate += 1 }
                    }
                    Spacer(Modifier.height(14.dp))
                    SlimSlider(
                        value = rate,
                        onValueChange = { rate = it },
                        valueRange = 1..100,
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, color = Color(0xFFAEC0DC), RoundedCornerShape(12.dp))
                    .background(HnColors.PrimaryLight)
                    .padding(16.dp),
            ) {
                Column {
                    Text("예상 소요 시간", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${h}시간 ${m}분", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
                        Spacer(Modifier.size(15.dp))
                        Text("· 종료 $expectedEndStr", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
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

            Spacer(Modifier.height(4.dp))
        }

        HnButton(
            text = "설정 완료",
            icon = if (success != null) Icons.Outlined.CheckCircle else Icons.Outlined.Timer,
            enabled = !submitting && success == null && medicationOrderIds.isNotEmpty()
                && volume > 0 && !volumeExceeded,
            loading = submitting,
            full = true,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
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
    }
}

@Composable
private fun LabelRow(title: String, suffix: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
        Text(" $suffix", fontSize = 14.sp, color = HnColors.TextTertiary)
    }
}

@Composable
private fun PresetChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val fg = if (active) HnColors.Primary else HnColors.TextSecondary
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) HnColors.PrimarySoft else HnColors.Surface)
            .border(1.dp, if (active) HnColors.Primary else HnColors.Border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = fg)
            if (icon != null) {
                Spacer(Modifier.size(4.dp))
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlimSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
) {
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.toInt()) },
        valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
        modifier = Modifier.fillMaxWidth().height(20.dp),
        thumb = {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(HnColors.Primary),
            )
        },
        track = { sliderState ->
            val start = sliderState.valueRange.start
            val end = sliderState.valueRange.endInclusive
            val fraction = if (end > start) ((sliderState.value - start) / (end - start)).coerceIn(0f, 1f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HnColors.Border),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(HnColors.Primary),
                )
            }
        },
    )
}

@Composable
private fun StepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFFF2F2F3))
            .clickable(onClick = onClick),
    ) { Icon(icon, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(26.dp)) }
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
        Text(label, fontSize = 14.sp, color = HnColors.TextSecondary)
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
