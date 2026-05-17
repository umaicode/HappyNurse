// 진행 중 IV 화면 — 카운트다운 + 약물명 표시 + 속도 변경 모달 + 종료 모달 + NFC 재태깅 복원
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.R
import com.happynurse.data.remote.model.IvInfusionResponse
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnButtonVariant
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.components.IvDripAnimation
import com.happynurse.presentation.components.NfcLifecycleEffect
import com.happynurse.presentation.theme.HnColors
import kotlinx.coroutines.delay

@Composable
fun IvTimerActiveScreen(
    ivInfusionId: Long,
    onClose: () -> Unit,
    viewModel: IvTimerActiveViewModel = hiltViewModel(),
) {
    LaunchedEffect(ivInfusionId) { viewModel.init(ivInfusionId) }

    NfcLifecycleEffect(
        onStart = viewModel::startNfc,
        onStop = viewModel::stopNfc,
    )

    val state by viewModel.state.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val remainingSec by viewModel.remainingSec.collectAsStateWithLifecycle()
    val patientLocation by viewModel.patientLocation.collectAsStateWithLifecycle()

    // 종료 성공 → 1.5s 후 자동 닫기
    LaunchedEffect(actionState) {
        if (actionState is IvTimerActiveViewModel.ActionState.Completed) {
            delay(1500)
            onClose()
        }
    }

    var showRateDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.Outlined.Close, "닫기",
                tint = HnColors.Text,
                modifier = Modifier.size(28.dp).clickable(onClick = onClose),
            )
        }

        when (val s = state) {
            IvTimerActiveViewModel.ActiveState.NeedsTag -> NeedsTagBody()
            IvTimerActiveViewModel.ActiveState.Loading -> LoadingBody()
            is IvTimerActiveViewModel.ActiveState.Error -> ErrorBody(s.message)
            is IvTimerActiveViewModel.ActiveState.Loaded -> LoadedBody(
                infusion = s.infusion,
                tagUid = s.tagUid,
                remainingSec = remainingSec,
                actionState = actionState,
                patientLocation = patientLocation,
                onRequestRateChange = { showRateDialog = true },
                onRequestComplete = { showCompleteDialog = true },
                onConsumeActionError = viewModel::consumeActionError,
            )
        }
    }

    val loaded = state as? IvTimerActiveViewModel.ActiveState.Loaded
    if (showRateDialog && loaded != null && loaded.tagUid != null) {
        RateChangeDialog(
            initialRate = (loaded.infusion.currentRateMlPerHr).coerceAtLeast(1.0).toInt(),
            onDismiss = { showRateDialog = false },
            onConfirm = { rate, patientType ->
                viewModel.submitChangeRate(rate, patientType)
                showRateDialog = false
            },
        )
    }

    if (showCompleteDialog && loaded != null && loaded.tagUid != null) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            containerColor = HnColors.Surface,
            shape = RoundedCornerShape(12.dp),
            title = { Text("수액 종료", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text) },
            text = { Text("진행 중인 수액을 종료하시겠습니까?", fontSize = 18.sp, color = HnColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.submitComplete()
                    showCompleteDialog = false
                }) { Text("종료", fontSize = 16.sp, color = HnColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text("취소", fontSize = 16.sp, color = HnColors.TextSecondary, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun NeedsTagBody() {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HnCard(padding = 24.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(HnColors.PrimarySoft),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nfc_label),
                        contentDescription = null,
                        tint = HnColors.Primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text("진행 중인 수액 NFC 태깅", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            }
        }

    }
}

@Composable
private fun LoadingBody() {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = HnColors.Primary, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(14.dp))
        Text("불러오는 중...", fontSize = 13.sp, color = HnColors.TextSecondary)
    }
}

@Composable
private fun ErrorBody(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HnCard(padding = 20.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = HnColors.Danger, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(10.dp))
                Text("불러오기 실패", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Spacer(Modifier.height(6.dp))
                Text(message, fontSize = 13.sp, color = HnColors.TextSecondary)
                Spacer(Modifier.height(10.dp))
                Text("다시 NFC 태깅하여 시도하세요", fontSize = 12.sp, color = HnColors.TextTertiary)
            }
        }
    }
}

@Composable
private fun LoadedBody(
    infusion: IvInfusionResponse,
    tagUid: String?,
    remainingSec: Long?,
    actionState: IvTimerActiveViewModel.ActionState,
    patientLocation: Pair<String, String>?,
    onRequestRateChange: () -> Unit,
    onRequestComplete: () -> Unit,
    onConsumeActionError: () -> Unit,
) {
    val isCompleted = infusion.status == "COMPLETED" ||
        actionState is IvTimerActiveViewModel.ActionState.Completed
    val critical = remainingSec != null && remainingSec in 1..299  // 5분 미만
    val expired = remainingSec != null && remainingSec <= 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
      Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 색상 분기 — 애니메이션 + 카운트다운 공통
        val accentColor = when {
            isCompleted -> HnColors.Success
            critical || expired -> HnColors.Danger
            else -> HnColors.Primary
        }
        val countdownBg = when {
            isCompleted -> HnColors.Success.copy(alpha = 0.12f)
            critical || expired -> HnColors.Danger.copy(alpha = 0.10f)
            else -> Color(0xFFE3F2FD)
        }

        // Hero IV 백 애니메이션 + 약물명
        val total = infusion.totalVolumeMl.takeIf { it > 0.0 } ?: 1.0
        val remaining = (infusion.remainingVolumeMl ?: 0.0).coerceAtLeast(0.0)
        val fillRatio = (remaining / total).toFloat().coerceIn(0f, 1f)
        val pct = (1f - fillRatio).coerceIn(0f, 1f)

        // IvTimerCard 와 동일한 3단계 색상 — 수액 백 액체 색
        val ivColor: Color = when {
            pct < 0.5f -> Color(0xFF5BAD8A) // IvSafeColor    (여유)
            pct < 0.8f -> Color(0xFFE8BE57) // IvCautionColor (주의)
            else       -> Color(0xFFD25757) // IvUrgentColor  (임박)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IvDripAnimation(
                fillRatio = fillRatio,
                color = ivColor,
                animate = !isCompleted,
                gttPerMin = infusion.rateGttPerMin?.takeIf { it > 0 },
                modifier = Modifier.size(width = 180.dp, height = 240.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                infusion.medications
                    .sortedBy { it.sequence }
                    .joinToString(" + ") { it.medicationName },
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HnColors.Text,
            )
        }
          Spacer(Modifier.height(20.dp))
        // 카운트다운 카드 — 옅은 톤
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(color = 0xFFF0F2F8))
                .padding(20.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    when {
                        isCompleted -> "수액 종료 완료"
                        expired -> "종료 임박"
                        critical -> "곧 종료됩니다"
                        else -> "남은 시간"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    formatRemaining(remainingSec, isCompleted),
                    fontSize = 28.sp, fontWeight = FontWeight.Black, color = accentColor,
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Stat("총량", "${infusion.totalVolumeMl.toInt()} mL", accentColor, Modifier.weight(1f))
                    Stat(
                        "잔여",
                        "${infusion.remainingVolumeMl?.toInt() ?: 0} mL",
                        accentColor,
                        Modifier.weight(1f),
                    )
                    Stat("속도", "%.1f mL/hr".format(infusion.currentRateMlPerHr), accentColor, Modifier.weight(1f))
                }
            }
        }

        // 액션 에러 표시
        (actionState as? IvTimerActiveViewModel.ActionState.Error)?.let { e ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HnColors.Surface)
                    .border(1.dp, HnColors.Danger, RoundedCornerShape(10.dp))
                    .clickable(onClick = onConsumeActionError)
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = HnColors.Danger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(e.message, fontSize = 13.sp, color = HnColors.Text, modifier = Modifier.weight(1f))
                    Text("닫기", fontSize = 13.sp, color = HnColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

      } // end inner scrollable Column

        val submitting = actionState is IvTimerActiveViewModel.ActionState.Submitting
        val canAct = tagUid != null && !isCompleted && !submitting

        // tagUid 가 있거나 종료된 경우에만 액션 버튼을 노출.
        // Setup 화면에서 막 진입한 직후(tagUid == null)에는 안내/버튼을 모두 숨김.
        if (tagUid != null || isCompleted) {
            Spacer(Modifier.height(12.dp))
            HnButton(
                text = "속도 변경",
                full = true,
                icon = Icons.Outlined.Speed,
                variant = HnButtonVariant.SECONDARY,
                enabled = canAct,
                onClick = onRequestRateChange,
            )
            Spacer(Modifier.height(8.dp))
            HnButton(
                text = if (isCompleted) "닫기" else "수액 종료",
                full = true,
                icon = Icons.Outlined.Stop,
                variant = HnButtonVariant.DANGER,
                enabled = isCompleted || canAct,
                loading = submitting,
                onClick = if (isCompleted) ({}) else onRequestComplete,
            )
        }
    }
}

@Composable
private fun RateChangeDialog(
    initialRate: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, PatientType) -> Unit,
) {
    var rate by remember { mutableIntStateOf(initialRate.coerceIn(1, 100)) }
    var patientType by remember { mutableStateOf(PatientType.SET_20) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HnColors.Surface,
        shape = RoundedCornerShape(12.dp),
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("수액 세트", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
                    Text(" (gtt/mL)", fontSize = 16.sp, color = HnColors.TextTertiary)
                }
                Spacer(Modifier.height(20.dp))
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
                Spacer(Modifier.height(30.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("주입 속도", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
                    Text(" (gtt/min)", fontSize = 16.sp, color = HnColors.TextTertiary)
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    StepBtn(Icons.Outlined.Remove) { rate = (rate - 1).coerceAtLeast(1) }
                    Spacer(Modifier.size(20.dp))
                    Box(
                        modifier = Modifier.widthIn(min = 70.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$rate", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
                    }
                    Spacer(Modifier.size(20.dp))
                    StepBtn(Icons.Outlined.Add) { rate = (rate + 1).coerceAtMost(100) }
                }
                Spacer(Modifier.height(15.dp))
                SlimSlider(
                    value = rate,
                    onValueChange = { rate = it },
                    valueRange = 1..100,
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("1", fontSize = 16.sp, color = HnColors.TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("100", fontSize = 16.sp, color = HnColors.TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(rate, patientType) }) {
                Text("확인", fontSize = 18.sp, color = HnColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", fontSize = 18.sp, color = HnColors.TextSecondary, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun Stat(label: String, value: String, accent: Color, mod: Modifier) {
    Column(mod) {
        Text(label, fontSize = 16.sp, color = HnColors.Text, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
    }
}

@Composable
private fun StepBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFFF2F2F3))
            .clickable(onClick = onClick),
    ) { Icon(icon, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(26.dp)) }
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

private fun formatRemaining(sec: Long?, completed: Boolean): String {
    if (completed) return "종료됨"
    val s = sec ?: return "--:--:--"
    val h = s / 3600
    val m = (s % 3600) / 60
    val ss = s % 60
    return "%02d:%02d:%02d".format(h, m, ss)
}
