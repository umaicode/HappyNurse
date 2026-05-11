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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.data.remote.model.IvInfusionResponse
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnButtonVariant
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.screens.nfc.findActivity
import com.happynurse.presentation.theme.HnColors
import kotlinx.coroutines.delay

@Composable
fun IVTimerActiveScreen(
    ivInfusionId: Long,
    onClose: () -> Unit,
    viewModel: IvTimerActiveViewModel = hiltViewModel(),
) {
    LaunchedEffect(ivInfusionId) { viewModel.init(ivInfusionId) }

    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        if (activity != null) viewModel.startNfc(activity)
        onDispose { if (activity != null) viewModel.stopNfc(activity) }
    }

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

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.Outlined.Close, "닫기",
                modifier = Modifier.size(28.dp).clickable(onClick = onClose),
            )
            Spacer(Modifier.size(8.dp))
            Text("진행 중 수액", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
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
            title = { Text("수액 종료", fontWeight = FontWeight.Bold) },
            text = { Text("진행 중인 수액을 종료하시겠습니까?", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.submitComplete()
                    showCompleteDialog = false
                }) { Text("종료", color = HnColors.Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) { Text("취소", color = HnColors.TextSecondary) }
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
                ) { Icon(Icons.Outlined.Nfc, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(36.dp)) }
                Spacer(Modifier.height(14.dp))
                Text("진행 중 수액에 NFC 태깅", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Spacer(Modifier.height(4.dp))
                Text("수액 백의 NFC 태그를 디바이스에 가까이 대주세요", fontSize = 13.sp, color = HnColors.TextSecondary)
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
        // 카운트다운 카드
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        when {
                            isCompleted -> listOf(HnColors.Success, HnColors.Success)
                            critical || expired -> listOf(HnColors.Danger, Color(0xFFB91C1C))
                            // setup 카드 / 버튼과 동일한 라이트 인디고 톤
                            else -> listOf(Color(0xFF5C6BC0), Color(0xFF3F51B5))
                        },
                    ),
                )
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
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    formatRemaining(remainingSec, isCompleted),
                    fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White,
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Stat("총량", "${infusion.totalVolumeMl.toInt()} mL", Modifier.weight(1f))
                    Stat(
                        "잔여",
                        "${infusion.remainingVolumeMl?.toInt() ?: 0} mL",
                        Modifier.weight(1f),
                    )
                    Stat("속도", "%.1f mL/hr".format(infusion.currentRateMlPerHr), Modifier.weight(1f))
                }
            }
        }

        // 약물명 (mix IV 면 N개 join)
        HnCard {
            Column {
                Text("약물", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextTertiary)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = HnColors.Purple, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        infusion.medications
                            .sortedBy { it.sequence }
                            .joinToString(" + ") { it.medicationName },
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text,
                    )
                }
                infusion.patientName?.let {
                    Spacer(Modifier.height(8.dp))
                    // 호실/침대: IvInfusionResponse 에는 없으므로 WardPatientListResponse 룩업값 사용
                    val room = patientLocation?.first?.takeIf { s -> s.isNotBlank() }
                    val bed = patientLocation?.second?.takeIf { s -> s.isNotBlank() }
                    val roomBed = listOfNotNull(room, bed).joinToString("-")
                    val patientLine = if (roomBed.isNotBlank()) "환자: $it · $roomBed" else "환자: $it"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F3F5))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(patientLine, fontSize = 12.sp, color = HnColors.TextSecondary)
                    }
                }
                infusion.note?.let {
                    Spacer(Modifier.height(2.dp))
                    Text("메모: $it", fontSize = 12.sp, color = HnColors.TextSecondary)
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
                    Text("닫기", fontSize = 12.sp, color = HnColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

      } // end inner scrollable Column

        Spacer(Modifier.height(12.dp))

        // tagUid 없을 때 안내
        if (tagUid == null && !isCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(HnColors.PrimarySoft)
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Nfc, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "속도 변경 / 종료 전, 수액 백을 다시 태깅해 주세요",
                        fontSize = 12.sp, color = HnColors.Primary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        val submitting = actionState is IvTimerActiveViewModel.ActionState.Submitting
        val canAct = tagUid != null && !isCompleted && !submitting
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
        title = { Text("주입 속도 변경", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("수액 세트", fontSize = 12.sp, color = HnColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PatientType.entries.forEach { p ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (patientType == p) HnColors.PrimarySoft else HnColors.Surface)
                                .border(
                                    1.dp,
                                    if (patientType == p) HnColors.Primary else HnColors.Border,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { patientType = p }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                p.gttPerMl.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (patientType == p) HnColors.Primary else HnColors.TextSecondary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("주입 속도 (gtt/min)", fontSize = 12.sp, color = HnColors.TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text("$rate", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
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
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(rate, patientType) }) {
                Text("확인", color = HnColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = HnColors.TextSecondary) }
        },
    )
}

@Composable
private fun Stat(label: String, value: String, mod: Modifier) {
    Column(mod) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
