// 약물 등록 — NFC 태깅으로 처방 검증(/drug/verify) 누적 → 일괄 저장(/drug/record)
package com.happynurse.presentation.screens.drugentry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.res.painterResource
import com.happynurse.R
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.happynurse.BuildConfig
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnButtonVariant
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.components.TagChip
import com.happynurse.presentation.components.findActivity
import com.happynurse.presentation.theme.HnColors

@Composable
fun DrugEntryScreen(
    patientId: Long,
    encounterId: Long,
    onClose: () -> Unit,
    onTimer: (encounterId: Long, medicationOrderIds: List<Long>) -> Unit,
    viewModel: DrugEntryViewModel = hiltViewModel(),
) {
    val activity = LocalContext.current.findActivity()
    // setContext 가 startNfc 전에 실행되도록 한 DisposableEffect 안에서 순서 보장.
    DisposableEffect(activity, patientId, encounterId) {
        viewModel.setContext(patientId, encounterId)
        android.util.Log.d("DrugEntryScreen", "DisposableEffect fired, activity=${activity?.javaClass?.simpleName ?: "NULL"} patientId=$patientId encounterId=$encounterId")
        if (activity != null) viewModel.startNfc(activity)
        onDispose {
            android.util.Log.d("DrugEntryScreen", "DisposableEffect disposed")
            if (activity != null) viewModel.stopNfc(activity)
        }
    }

    val drugs by viewModel.verifiedDrugs.collectAsStateWithLifecycle()
    val verifyError by viewModel.verifyError.collectAsStateWithLifecycle()
    val submit by viewModel.submitState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                "뒤로",
                modifier = Modifier.size(46.dp).clickable(onClick = onClose),
            )
        }

        when (val s = submit) {
            DrugEntryViewModel.SubmitState.Idle,
            DrugEntryViewModel.SubmitState.Submitting -> EntryBody(
                drugs = drugs,
                verifyError = verifyError,
                submitting = s is DrugEntryViewModel.SubmitState.Submitting,
                onConsumeError = viewModel::consumeVerifyError,
                onRemove = viewModel::removeDrug,
                onSubmit = viewModel::submit,
                onTimer = { onTimer(encounterId, drugs.map { it.medicationOrderId }) },
                onDebugInject = viewModel::onTagScanned,
            )
            is DrugEntryViewModel.SubmitState.Success -> SuccessBody(
                drugs = s.drugs,
                patientName = s.patientName,
                savedCount = s.savedCount,
                savedAt = s.savedAt,
                onClose = onClose,
            )
            is DrugEntryViewModel.SubmitState.Error -> ErrorBody(
                message = s.message,
                onDismiss = viewModel::consumeSubmitState,
            )
        }
    }
}

@Composable
private fun ColumnScope.EntryBody(
    drugs: List<DrugEntryViewModel.VerifiedDrug>,
    verifyError: String?,
    submitting: Boolean,
    onConsumeError: () -> Unit,
    onRemove: (String) -> Unit,
    onSubmit: () -> Unit,
    onTimer: () -> Unit,
    onDebugInject: (String) -> Unit = {},
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        HnCard(padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HnColors.PrimarySoft),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nfc_label),
                        contentDescription = null,
                        tint = HnColors.Primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("약물 NFC 태그", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
                    Text(
                        "디바이스를 약물에 가까이 대주세요",
                        fontSize = 16.sp, color = HnColors.TextSecondary,
                    )
                }
            }
        }
    }

    if (BuildConfig.DEBUG) {
        var debugUid by remember { mutableStateOf("04:57:C1:48:C6:2A:81") }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.padding(horizontal = 20.dp)) {
            HnCard(padding = 12.dp) {
                Column {
                    Text("디버그 — NFC UID 직접 입력", fontSize = 11.sp, color = HnColors.TextTertiary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = debugUid,
                            onValueChange = { debugUid = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            placeholder = { Text("04:57:C1:48:C6:2A:81", fontSize = 13.sp) },
                        )
                        Spacer(Modifier.size(8.dp))
                        HnButton(
                            text = "주입",
                            onClick = { onDebugInject(debugUid.trim()) },
                            enabled = debugUid.isNotBlank(),
                        )
                    }
                }
            }
        }
    }

    if (verifyError != null) {
        Spacer(Modifier.height(10.dp))
        Box(Modifier.padding(horizontal = 20.dp)) {
            HnCard(padding = 12.dp, onClick = onConsumeError) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = HnColors.Danger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(verifyError, fontSize = 16.sp, color = HnColors.Text, modifier = Modifier.weight(1f))
                    Text("닫기", fontSize = 14.sp, color = HnColors.TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("검증된 처방", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary)
                Text("${drugs.size}개", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
            }
        }
        if (drugs.isEmpty()) {
            item {
                HnCard(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
                    Text(
                        "약물 NFC 칩을 태깅하면 여기에 표시됩니다",
                        fontSize = 18.sp, color = HnColors.TextSecondary, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else {
            items(drugs, key = { it.tagUid }) { d -> VerifiedDrugRow(d, onRemove) }
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        HnButton(
            text = "수액타이머 설정",
            variant = HnButtonVariant.SECONDARY,
            full = true,
            icon = Icons.Outlined.Timer,
            enabled = drugs.isNotEmpty() && !submitting,
            onClick = onTimer,
        )
        Spacer(Modifier.height(8.dp))
        HnButton(
            text = "투약 완료",
            full = true,
            enabled = drugs.isNotEmpty() && !submitting,
            loading = submitting,
            onClick = onSubmit,
        )
    }
}

@Composable
private fun VerifiedDrugRow(
    d: DrugEntryViewModel.VerifiedDrug,
    onRemove: (String) -> Unit,
) {
    HnCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                TagChip("약물", fg = HnColors.Primary, bg = HnColors.PrimarySoft)
                Spacer(Modifier.height(10.dp))
                Text(d.medicationName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Text("처방코드 ${d.orderCode ?: "—"}", fontSize = 16.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                    .background(HnColors.Surface).clickable { onRemove(d.tagUid) },
            ) { Icon(Icons.Outlined.Close, "삭제", tint = HnColors.TextSecondary, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun SuccessBody(
    drugs: List<DrugEntryViewModel.VerifiedDrug>,
    patientName: String?,
    savedCount: Int,
    savedAt: java.time.LocalDateTime,
    onClose: () -> Unit,
) {
    val timeStr = savedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
        HnCard(padding = 20.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = HnColors.Success, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text("투약 기록 저장 완료", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                        Text(timeStr, fontSize = 12.sp, color = HnColors.TextSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                androidx.compose.material3.HorizontalDivider(color = HnColors.Border)
                Spacer(Modifier.height(12.dp))
                Text("환자", fontSize = 11.sp, color = HnColors.TextTertiary)
                Spacer(Modifier.height(2.dp))
                Text(patientName ?: "환자", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HnColors.Text)
                Spacer(Modifier.height(14.dp))
                Text("투약된 약물 ($savedCount)", fontSize = 11.sp, color = HnColors.TextTertiary)
                Spacer(Modifier.height(6.dp))
                drugs.forEachIndexed { idx, d ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Text(
                            "${idx + 1}.",
                            fontSize = 13.sp,
                            color = HnColors.TextSecondary,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(
                            d.medicationName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HnColors.Text,
                            modifier = Modifier.weight(1f),
                        )
                        Text(d.orderCode ?: "—", fontSize = 11.sp, color = HnColors.TextTertiary)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        HnButton(text = "확인", full = true, onClick = onClose)
    }
}

@Composable
private fun ErrorBody(message: String, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
        HnCard(padding = 20.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = HnColors.Danger, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("저장 실패", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Spacer(Modifier.height(6.dp))
                Text(message, fontSize = 13.sp, color = HnColors.TextSecondary)
            }
        }
        Spacer(Modifier.weight(1f))
        HnButton(text = "다시 시도", full = true, variant = HnButtonVariant.SECONDARY, onClick = onDismiss)
    }
}
