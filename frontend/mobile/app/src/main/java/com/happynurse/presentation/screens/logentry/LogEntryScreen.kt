// 간호일지 등록 — 마이크 녹음 → STT 서버 (POST /api/stt/recognize) → 교정 결과 표시
package com.happynurse.presentation.screens.logentry

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.presentation.components.HnButton
import com.happynurse.presentation.components.HnButtonVariant
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors

@Composable
fun LogEntryScreen(
    patientId: Long,
    encounterId: Long,
    onClose: () -> Unit,
    viewModel: LogEntryViewModel = hiltViewModel(),
) {
    LaunchedEffect(patientId, encounterId) { viewModel.setContext(patientId, encounterId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // RECORD_AUDIO 권한 — manifest 에는 이미 선언됨. 런타임 권한 요청 launcher.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.startRecording() }

    val onStartClick: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startRecording()
        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "뒤로",
                modifier = Modifier.size(28.dp).clickable(onClick = onClose),
            )
            Spacer(Modifier.size(8.dp))
            Text("간호일지 등록", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        }

        when (val s = state) {
            LogEntryViewModel.LogState.Idle -> IdleBody(onStart = onStartClick)
            is LogEntryViewModel.LogState.Recording -> RecordingBody(
                seconds = s.seconds,
                onStop = viewModel::stopAndUpload,
            )
            LogEntryViewModel.LogState.Uploading -> UploadingBody()
            is LogEntryViewModel.LogState.Result -> ResultBody(
                state = s,
                onRetry = viewModel::reset,
                onClose = onClose,
            )
            is LogEntryViewModel.LogState.Error -> ErrorBody(
                message = s.message,
                onRetry = viewModel::reset,
            )
        }
    }
}

@Composable
private fun IdleBody(onStart: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(20.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp).clip(CircleShape)
                .background(HnColors.Primary)
                .clickable(onClick = onStart),
        ) {
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("버튼을 눌러 녹음을 시작하세요", fontSize = 15.sp, color = HnColors.TextSecondary)
    }
}

@Composable
private fun RecordingBody(seconds: Int, onStop: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(20.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp).clip(CircleShape)
                .background(HnColors.Danger)
                .clickable(onClick = onStop),
        ) {
            Icon(Icons.Outlined.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            formatSec(seconds),
            fontSize = 32.sp, fontWeight = FontWeight.Bold, color = HnColors.Danger,
        )
        Text("녹음 중 · 정지 버튼을 누르세요", fontSize = 13.sp, color = HnColors.TextSecondary)
    }
}

@Composable
private fun UploadingBody() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(20.dp),
    ) {
        CircularProgressIndicator(color = HnColors.Primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(14.dp))
        Text("STT 변환 중...", fontSize = 13.sp, color = HnColors.TextSecondary)
    }
}

@Composable
private fun ResultBody(
    state: LogEntryViewModel.LogState.Result,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    val resp = state.response
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            // status='draft' 로 백엔드 저장됨. 확정/수정은 데스크톱 차트에서.
            if (resp.nursingRecordId != null) "STT 변환 완료 — 데스크톱 차트에서 확정" else "STT 변환 결과",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary,
        )
        HnCard(modifier = Modifier.weight(1f)) {
            Column {
                resp.audioUrl?.let {
                    Text(it, fontSize = 11.sp, color = HnColors.TextTertiary)
                    Spacer(Modifier.height(6.dp))
                }
                resp.correctedText?.let {
                    Text("교정된 본문", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextTertiary)
                    Spacer(Modifier.height(2.dp))
                    Text(it, fontSize = 14.sp, color = HnColors.Text)
                    Spacer(Modifier.height(10.dp))
                }
                resp.originalText?.takeIf { it != resp.correctedText }?.let {
                    Text("원본", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextTertiary)
                    Spacer(Modifier.height(2.dp))
                    Text(it, fontSize = 12.sp, color = HnColors.TextSecondary)
                    Spacer(Modifier.height(10.dp))
                }
                if (resp.corrections.isNotEmpty()) {
                    Text("교정 내역 (${resp.corrections.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextTertiary)
                    Spacer(Modifier.height(2.dp))
                    resp.corrections.forEach { c ->
                        Text(
                            "${c.original} → ${c.corrected}" + (c.type?.let { " (${it})" } ?: ""),
                            fontSize = 12.sp, color = HnColors.Text,
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HnButton(
                text = "다시 녹음",
                variant = HnButtonVariant.SECONDARY,
                full = true,
                onClick = onRetry,
                modifier = Modifier.weight(1f),
            )
            HnButton(
                text = "닫기",
                icon = Icons.AutoMirrored.Outlined.Send,
                full = true,
                onClick = onClose,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(20.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = HnColors.Danger, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("STT 변환 실패", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 13.sp, color = HnColors.TextSecondary)
        Spacer(Modifier.height(20.dp))
        HnButton(text = "다시 시도", variant = HnButtonVariant.SECONDARY, onClick = onRetry)
    }
}

private fun formatSec(s: Int) = "%02d:%02d".format(s / 60, s % 60)
