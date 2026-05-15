// SttResultScreen — STT 인식 결과 표시 화면. 상단에 인식된 시간, 중앙에 녹음 내용, 하단에 [재녹음][전송] 버튼.
// 사용자가 [전송] 터치 또는 손목 싱글 스냅으로 RecordViewModel.confirm() 트리거 → 등록.
// [재녹음] 누르면 결과 폐기하고 즉시 새 녹음 시작 → RecordScreen 으로 복귀.
// swipe-from-left 또는 phase=IDLE 이면 등록 없이 홈 복귀.
// DONE 상태가 되면 "등록 완료" 배지를 잠시 보여준 뒤 자동으로 홈으로 복귀한다.
package com.happynurse.wear.presentation.screens.stt

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.happynurse.wear.data.util.FireAtFormatter
import com.happynurse.wear.presentation.screens.record.RecordPhase
import com.happynurse.wear.presentation.screens.record.RecordViewModel
import com.happynurse.wear.presentation.theme.TabularNumStyle
import kotlinx.coroutines.delay

private const val DONE_AUTO_DISMISS_MS = 1500L

@Composable
fun SttResultScreen(
    viewModel: RecordViewModel,
    onSubmitted: () -> Unit,
    onCancel: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fireAtDisplay = remember(state.fireAtEpochMillis) {
        state.fireAtEpochMillis?.let { FireAtFormatter.format(it) }.orEmpty()
    }

    LaunchedEffect(state.phase) {
        when (state.phase) {
            // DONE 진입 즉시 화면을 닫고 홈으로 복귀. IDLE 로의 자동 복귀는 ViewModel 이 책임진다.
            RecordPhase.DONE -> {
                delay(DONE_AUTO_DISMISS_MS)
                onSubmitted()
            }
            // 재녹음 트리거로 phase 가 RECORDING 으로 바뀌면 RecordScreen 으로 복귀.
            // IDLE 은 등록 완료 후의 자동 reset 신호이므로 onCancel 트리거에서 제외한다.
            RecordPhase.RECORDING -> onCancel()
            else -> Unit
        }
    }

    AppScaffold(timeText = {}) {
        ScreenScaffold {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                ScalingLazyColumn(
                    state = rememberScalingLazyListState(),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 28.dp, bottom = 24.dp, start = 14.dp, end = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { TimeBadge(text = fireAtDisplay.ifBlank { "—" }) }
                    item { RecognizedTextCard(text = state.recognizedText) }
                    if (state.errorMessage != null && state.phase == RecordPhase.ERROR) {
                        item {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (state.phase != RecordPhase.DONE) {
                        item {
                            RestartSubmitRow(
                                enabled = state.phase == RecordPhase.RESULT,
                                onSubmit = viewModel::confirm,
                                onRestart = viewModel::restartRecording,
                            )
                        }
                    }
                }

                if (state.phase == RecordPhase.SUBMITTING) {
                    SubmittingOverlay(modifier = Modifier.align(Alignment.Center))
                }
                // DONE 은 상단에 체크 아이콘으로 표시 — 결과 카드 위로 떠 있다가 잠시 후 자동 dismiss.
                // top padding 은 워치 상단 시계 reserve 영역(약 28dp) 과 겹치지 않도록 넉넉히.
                if (state.phase == RecordPhase.DONE) {
                    DoneToast(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeBadge(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "인식된 시간",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.merge(TabularNumStyle),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RecognizedTextCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "녹음 내용",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RestartSubmitRow(
    enabled: Boolean,
    onSubmit: () -> Unit,
    onRestart: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionPill(
            label = "재녹음",
            background = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            enabled = enabled,
            onClick = onRestart,
        )
        ActionPill(
            label = "등록",
            background = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            enabled = enabled,
            onClick = onSubmit,
        )
    }
}

@Composable
private fun ActionPill(
    label: String,
    background: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background.copy(alpha = alpha))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.copy(alpha = alpha),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Material 3 Expressive 스타일 체크 아이콘 — 부드러운 spring scale-in 으로 등장.
 * primary 원 + 둥근 체크 아이콘 단일 레이어.
 */
@Composable
private fun DoneToast(modifier: Modifier = Modifier) {
    var entered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "doneToastScale",
    )
    LaunchedEffect(Unit) { entered = true }

    Box(
        modifier = modifier
            .scale(scale)
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "등록 완료",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun SubmittingOverlay(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            text = "등록 중…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
