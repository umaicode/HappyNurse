// 간호일지 등록 — 마이크 녹음 → STT 서버 (POST /api/stt/recognize) → 교정 결과 표시
package com.happynurse.presentation.screens.logentry

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
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

// 녹음 강조색 — 메인 네이비(#1428A0)와 어울리는 차분한 brick red
private val RecordingRed = Color(0xFFC23B36)

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
                modifier = Modifier.size(46.dp).clickable(onClick = onClose),
            )
        }

        val levels by viewModel.levels.collectAsStateWithLifecycle()
        when (val s = state) {
            LogEntryViewModel.LogState.Idle -> IdleBody(onStart = onStartClick)
            is LogEntryViewModel.LogState.Recording -> RecordingBody(
                seconds = s.seconds,
                levels = levels,
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
        // RecordingBody 와 동일한 Y 위치 — 텍스트는 안내 카피, 아래 placeholder 들은 자리 유지용.
        Text(
            "간호일지 녹음",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HnColors.Text,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(40.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp).clip(CircleShape)
                .background(HnColors.Primary)
                .clickable(onClick = onStart),
        ) {
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(80.dp))
        }
        Spacer(Modifier.height(30.dp))
        Text(
            "00:00",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Transparent,
        )
        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun RecordingBody(seconds: Int, levels: List<Float>, onStop: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(20.dp),
    ) {
        Text(
            "녹음 중",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RecordingRed,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(40.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp).clip(CircleShape)
                .background(RecordingRed)
                .clickable(onClick = onStop),
        ) {
            Icon(Icons.Outlined.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(110.dp))
        }
        Spacer(Modifier.height(30.dp))
        Text(
            formatSec(seconds),
            fontSize = 40.sp, fontWeight = FontWeight.Bold, color = RecordingRed,
        )
        Spacer(Modifier.height(28.dp))
        RecordingEqualizer(levels = levels)
    }
}

@Composable
private fun RecordingEqualizer(levels: List<Float>) {
    // 13개 막대 — 인덱스별로 amplitude 히스토리 한 칸씩 매핑.
    // 0번(가장 왼쪽) = 가장 최근 입력, 12번(오른쪽) = 가장 오래된 입력. tick 마다 좌→우로 흐르는 파형.
    val maxHeights = listOf(18f, 26f, 36f, 46f, 56f, 64f, 70f, 64f, 56f, 46f, 36f, 26f, 18f)
    Box(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            maxHeights.forEachIndexed { i, maxH ->
                val amp = levels.getOrElse(i) { 0f }.coerceIn(0f, 1f).coerceAtLeast(0.05f)
                val height by animateFloatAsState(
                    targetValue = maxH * amp,
                    animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
                    label = "bar$i",
                )
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(height.dp)
                        .clip(RoundedCornerShape(50))
                        .background(RecordingRed),
                )
            }
        }
    }
}

@Composable
private fun UploadingBody() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(20.dp),
    ) {
        CircularProgressIndicator(color = HnColors.Primary, modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(40.dp))
        Text("STT 변환 및 정제중...", fontSize = 24.sp, color = HnColors.Text, fontWeight = FontWeight.SemiBold)
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
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (resp.nursingRecordId != null) {
            // status='draft' 로 백엔드 저장됨. 확정/수정은 데스크톱 차트에서.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(HnColors.Success),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    "간호일지 임시 등록 완료",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HnColors.Text,
                )
            }
        }
        HnCard(modifier = Modifier.fillMaxWidth().weight(1f), padding = 22.dp) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                val corrected = resp.correctedText?.takeIf { it.isNotBlank() }
                if (corrected != null) {
                    Text(corrected, fontSize = 22.sp, lineHeight = 34.sp, color = HnColors.Text)
                } else {
                    Text(
                        "교정된 본문이 없습니다",
                        fontSize = 18.sp,
                        color = HnColors.TextTertiary,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            HnButton(
                text = "다시 녹음",
                variant = HnButtonVariant.SECONDARY,
                full = true,
                onClick = onRetry,
                modifier = Modifier.weight(1f),
            )
            HnButton(
                text = "확인",
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
