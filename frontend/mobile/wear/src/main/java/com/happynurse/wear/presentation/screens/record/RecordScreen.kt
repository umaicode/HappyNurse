// RecordScreen — 워치 녹음 화면. 마이크 권한 → 녹음 → STT 인식/시간 파싱 진행 상태를 한 화면에서 처리.
// 결과(RESULT) 단계가 되면 onShowResult 콜백으로 SttResultScreen 으로 이동시킨다.
package com.happynurse.wear.presentation.screens.record

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import com.happynurse.wear.presentation.components.HnPagerIndicator
import com.happynurse.wear.presentation.components.HnPulseRing
import com.happynurse.wear.presentation.components.HnPulsingDot
import com.happynurse.wear.presentation.theme.TabularNumStyle

private val ButtonSize = 80.dp

@Composable
fun RecordScreen(
    viewModel: RecordViewModel,
    onShowResult: () -> Unit,
    pagerCurrentPage: Int = 1,
    autoStart: Boolean = false,
    onAutoStartConsumed: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.startRecording()
    }

    LaunchedEffect(state.phase) {
        if (state.phase == RecordPhase.RESULT) onShowResult()
    }

    // 손목 제스처로 진입한 경우 — 이전 phase 잔여를 reset 한 뒤 즉시 녹음 시작.
    LaunchedEffect(autoStart) {
        if (!autoStart) return@LaunchedEffect
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
                            onStop = viewModel::stopRecording,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 22.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "알람 설정",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(ButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clickable { onMicTap() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "녹음 시작",
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RecordingContent(elapsed: Int, onStop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 22.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TimeChip(text = formatMmSs(elapsed))
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            HnPulseRing(diameterDp = 110, color = MaterialTheme.colorScheme.error)
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
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

@Composable
private fun TimeChip(text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HnPulsingDot()
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.merge(TabularNumStyle),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Bold,
        )
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
