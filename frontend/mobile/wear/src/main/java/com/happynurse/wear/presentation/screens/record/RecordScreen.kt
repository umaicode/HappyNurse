// RecordScreen — 워치 녹음 화면(명세 §2). IDLE/RECORDING 두 상태를 한 화면에서 처리.
// IDLE/RECORDING의 중앙 버튼(80dp, error 컬러)이 동일 위치/크기로 배치되어
// AnimatedContent fade+scale 전환 시 자연스럽게 mic↔stop 이 모핑되는 듯한 인상을 준다.
package com.happynurse.wear.presentation.screens.record

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

private enum class RecordState { IDLE, RECORDING }
private const val MAX_RECORD_SEC = 60
private val ButtonSize = 80.dp

@Composable
fun RecordScreen(
    onRecordingComplete: () -> Unit,
    pagerCurrentPage: Int = 1,
) {
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
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.92f)) togetherWith
                    (fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.96f))
            },
            label = "recordStateAnim",
        ) { currentState ->
            when (currentState) {
                RecordState.IDLE -> IdleContent(onMicTap = { state = RecordState.RECORDING })
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
private fun IdleContent(onMicTap: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 22.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 상단: "환자 타이머" 라벨 (시간 칩 대체)
        TitleLabel(text = "환자 타이머")

        Spacer(Modifier.weight(1f))

        // 중앙 — 정지 버튼과 동일 위치/사이즈의 빨간 마이크 버튼
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
        // 상단 시간 칩 — 점멸 도트 + 경과 시간
        TimeChip(text = formatMmSs(elapsed))

        Spacer(Modifier.weight(1f))

        // 중앙 펄스 링 + 빨간 Stop 버튼 (IDLE의 mic 버튼과 동일 사이즈/위치)
        Box(contentAlignment = Alignment.Center) {
            HnPulseRing(
                diameterDp = 110,
                color = MaterialTheme.colorScheme.error,
            )
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
private fun TitleLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
    )
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

private fun formatMmSs(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}
