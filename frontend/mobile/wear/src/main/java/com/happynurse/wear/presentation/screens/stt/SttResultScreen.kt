// SttResultScreen — STT 인식 결과 확인. 상단에 인식된 시간, 중앙에 녹음 내용, 하단에 확인 EdgeButton.
// 사용자가 EdgeButton 을 누르면 RecordViewModel.confirm() 으로 /reminders/stt 등록을 실행한다.
package com.happynurse.wear.presentation.screens.stt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.happynurse.wear.data.util.FireAtFormatter
import com.happynurse.wear.presentation.screens.record.RecordPhase
import com.happynurse.wear.presentation.screens.record.RecordViewModel
import com.happynurse.wear.presentation.theme.TabularNumStyle

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
            RecordPhase.DONE -> {
                viewModel.consumeDone()
                onSubmitted()
            }
            RecordPhase.IDLE -> onCancel()
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
                    contentPadding = PaddingValues(top = 28.dp, bottom = 60.dp, start = 14.dp, end = 14.dp),
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
                }

                if (state.phase == RecordPhase.SUBMITTING) {
                    SubmittingOverlay(modifier = Modifier.align(Alignment.Center))
                }

                EdgeButton(
                    onClick = viewModel::confirm,
                    enabled = state.fireAtEpochMillis != null && state.phase != RecordPhase.SUBMITTING,
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Text(
                        text = "확인",
                        fontWeight = FontWeight.Bold,
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
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = 14.dp, horizontal = 16.dp),
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
                style = MaterialTheme.typography.titleLarge.merge(TabularNumStyle),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
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
