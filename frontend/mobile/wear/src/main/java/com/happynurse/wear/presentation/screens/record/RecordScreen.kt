// RecordScreen — 워치 녹음 화면(명세 §2). IDLE/RECORDING 두 상태를 한 화면에서 처리.
// 마이크 탭 시 새 화면으로 이동하지 않고 인라인으로 녹음 상태 전환. 60초 또는 Stop 탭 시 onRecordingComplete 호출.
package com.happynurse.wear.presentation.screens.record

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.presentation.components.HnPagerIndicator
import com.happynurse.wear.presentation.components.HnPulseRing
import com.happynurse.wear.presentation.components.HnPulsingDot
import com.happynurse.wear.presentation.theme.TabularNumStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class RecordState { IDLE, RECORDING }
private const val MAX_RECORD_SEC = 60

@Composable
fun RecordScreen(
    onRecordingComplete: () -> Unit,
    pagerCurrentPage: Int = 1,
) {
    val now by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.KOREA).format(Date())) }
    var state by remember { mutableStateOf(RecordState.IDLE) }
    var elapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        if (state == RecordState.RECORDING) {
            elapsed = 0
            while (elapsed < MAX_RECORD_SEC) {
                delay(1000)
                elapsed += 1
            }
            state = RecordState.IDLE
            onRecordingComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "recordStateAnim",
        ) { currentState ->
            when (currentState) {
                RecordState.IDLE -> IdleContent(
                    now = now,
                    onMicTap = { state = RecordState.RECORDING },
                )
                RecordState.RECORDING -> RecordingContent(
                    elapsed = elapsed,
                    onStop = {
                        state = RecordState.IDLE
                        onRecordingComplete()
                    },
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

@Composable
private fun IdleContent(now: String, onMicTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 18.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        // 상단 시각 칩 (Material 3 스타일)
        TimeChip(text = now, isRecording = false)
        Text(
            text = "탭하여 녹음 시작",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onMicTap() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "녹음 시작",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(44.dp),
            )
        }
        Text(
            text = "→ 스와이프 → 알람 리스트",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecordingContent(elapsed: Int, onStop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 18.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 상단 시간 칩 — 점멸 도트 + 경과 시간
        TimeChip(text = formatMmSs(elapsed), isRecording = true)

        Spacer(Modifier.weight(1f))

        // 중앙 펄스 링 + 빨간 Stop 버튼
        Box(contentAlignment = Alignment.Center) {
            HnPulseRing(
                diameterDp = 110,
                color = MaterialTheme.colorScheme.error,
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { onStop() },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White),
                )
            }
        }
        Text(
            text = "탭하여 중지",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun TimeChip(text: String, isRecording: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isRecording) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainer,
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isRecording) {
            HnPulsingDot()
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.merge(
                if (isRecording) TabularNumStyle else TabularNumStyle
            ),
            color = if (isRecording) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun formatMmSs(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
