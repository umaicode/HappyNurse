// RecordScreen — 워치 녹음 화면. 마이크 권한 → 녹음 → STT 인식/시간 파싱 진행 상태를 한 화면에서 처리.
// 결과(RESULT) 단계가 되면 onShowResult 콜백으로 SttResultScreen 으로 이동시킨다.
package com.happynurse.wear.presentation.screens.record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.happynurse.wear.presentation.components.HnAudioWaveform
import com.happynurse.wear.presentation.components.HnPagerIndicator
import com.happynurse.wear.presentation.components.HnPulsingDot
import com.happynurse.wear.presentation.theme.TabularNumStyle

private val ButtonSize = 80.dp
// 녹음 중 좌→우 스와이프로 취소되는 누적 픽셀 임계값. 워치 화면(약 240~450px) 기준 약 1/3 폭.
private const val SWIPE_CANCEL_THRESHOLD_PX = 80f

@Composable
fun RecordScreen(
    viewModel: RecordViewModel,
    onShowResult: () -> Unit,
    pagerCurrentPage: Int = 1,
    autoStartTrigger: Long = 0L,
    onAutoStartConsumed: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val amplitudes by viewModel.amplitudes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startRecording()
    }

    LaunchedEffect(state.phase) {
        if (state.phase == RecordPhase.RESULT) onShowResult()
    }

    // 녹음/처리/결과/등록 진행 중에는 화면이 꺼지지 않도록 FLAG_KEEP_SCREEN_ON 유지.
    // RecordScreen 이 dispose 되거나 phase 가 IDLE/ERROR 로 가면 flag 해제.
    val keepScreenOn = state.phase == RecordPhase.RECORDING ||
        state.phase == RecordPhase.PROCESSING ||
        state.phase == RecordPhase.RESULT ||
        state.phase == RecordPhase.SUBMITTING ||
        state.phase == RecordPhase.DONE
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 손목 제스처로 진입한 경우 — 현재 phase 가 IDLE/ERROR 일 때만 자동 녹음.
    // RECORDING/PROCESSING/RESULT/SUBMITTING/DONE 이면 진행 중인 흐름을 끊지 않도록 신호만 소비.
    // autoStartTrigger 는 매번 unique 한 timestamp 라 같은 신호가 반복돼도 LaunchedEffect 가 재실행됨.
    LaunchedEffect(autoStartTrigger) {
        if (autoStartTrigger <= 0L) return@LaunchedEffect
        // ViewModel 레벨에서 같은 trigger 중복 처리 방지 — stale state 부활 시에도 두 번 startRecording 안 함.
        if (!viewModel.tryConsumeAutoStartTrigger(autoStartTrigger)) {
            onAutoStartConsumed()
            return@LaunchedEffect
        }
        val current = viewModel.state.value.phase
        if (current != RecordPhase.IDLE && current != RecordPhase.ERROR) {
            onAutoStartConsumed()
            return@LaunchedEffect
        }
        if (hasRecordPermission(context)) {
            viewModel.reset()
            viewModel.startRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        onAutoStartConsumed()
    }

    AppScaffold(timeText = {}) {
        ScreenScaffold {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                AnimatedContent(
                    targetState = state.phase,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.92f)) togetherWith
                            (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f))
                    },
                    label = "recordPhaseAnim",
                ) { phase ->
                    when (phase) {
                        RecordPhase.IDLE -> IdleContent(
                            onMicTap = {
                                if (hasRecordPermission(context)) viewModel.startRecording()
                                else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                        )
                        RecordPhase.RECORDING -> RecordingContent(
                            elapsed = state.elapsedSec,
                            amplitudes = amplitudes,
                            onStop = viewModel::stopRecording,
                            onSwipeCancel = viewModel::cancelRecording,
                        )
                        RecordPhase.PROCESSING -> ProcessingContent("음성 인식 중…")
                        RecordPhase.SUBMITTING -> ProcessingContent("등록 중…")
                        RecordPhase.RESULT,
                        RecordPhase.DONE -> ProcessingContent("결과 준비 중…")
                        RecordPhase.ERROR -> ErrorContent(
                            message = state.errorMessage ?: "다시 시도해 주세요",
                            onRetry = viewModel::reset,
                        )
                    }
                }
                HnPagerIndicator(
                    pageCount = 2,
                    currentPage = pagerCurrentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onMicTap: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = "타이머 설정",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(ButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clickable { onMicTap() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "녹음 시작",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun RecordingContent(
    elapsed: Int,
    amplitudes: List<Float>,
    onStop: () -> Unit,
    onSwipeCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDx = 0f },
                    onDragEnd = { totalDx = 0f },
                    onDragCancel = { totalDx = 0f },
                ) { _, dragAmount ->
                    totalDx += dragAmount
                    if (totalDx > SWIPE_CANCEL_THRESHOLD_PX) {
                        totalDx = 0f
                        onSwipeCancel()
                    }
                }
            }
            .padding(top = 22.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 상단: ● REC
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HnPulsingDot()
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
        // 큰 흰 녹음 시간
        Text(
            text = formatMmSs(elapsed),
            style = MaterialTheme.typography.displayMedium.merge(TabularNumStyle),
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        // 파형 + 정지 버튼 겹침
        Box(contentAlignment = Alignment.Center) {
            HnAudioWaveform(amplitudes = amplitudes)
            Box(
                modifier = Modifier
                    .size(ButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { onStop() },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White),
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ProcessingContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.size(14.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onRetry() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                text = "다시 시도",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun hasRecordPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun formatMmSs(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
