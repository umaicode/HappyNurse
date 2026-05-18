// HomeRecordPager — home(0) ↔ record(1) 두 페이지를 좌우 스와이프로 전환하는 HorizontalPager 호스트.
// autoStartRecord=true 면 record 페이지(1)로 초기 진입하고 RecordScreen 에 자동 시작 신호 전달.
package com.happynurse.wear.presentation.screens.pager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
import com.happynurse.wear.domain.model.IvInfusionTimer
import com.happynurse.wear.domain.model.SttTimer
import com.happynurse.wear.presentation.screens.home.HomeScreen
import com.happynurse.wear.presentation.screens.record.RecordPhase
import com.happynurse.wear.presentation.screens.record.RecordScreen
import com.happynurse.wear.presentation.screens.record.RecordViewModel

@Composable
fun HomeRecordPager(
    recordViewModel: RecordViewModel,
    onIvClick: (IvInfusionTimer) -> Unit,
    onSttClick: (SttTimer) -> Unit,
    onShowResult: () -> Unit,
    autoStartRecordTrigger: Long = 0L,
    onAutoStartConsumed: () -> Unit = {},
) {
    val hasTrigger = autoStartRecordTrigger > 0L
    val initialPage = if (hasTrigger) 1 else 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    // rememberPagerState 는 한 번 만들면 initialPage 를 다시 안 봄 — 저장된 page 가 우선이라
    // 제스처로 재진입했을 때 record(1) 로 못 가는 문제가 있음. trigger 가 들어오면 강제로 page 1.
    // lifecycle RESUMED 까지 대기해서 STOPPED→STARTED 전환 frame timing 에 scroll 이 무시되는 race 차단.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(autoStartRecordTrigger) {
        if (autoStartRecordTrigger <= 0L) return@LaunchedEffect
        lifecycleOwner.lifecycle.withResumed { }
        if (pagerState.pageCount > 0 && pagerState.currentPage != 1) {
            pagerState.scrollToPage(1)
        }
    }
    // 녹음 중에는 페이지 스와이프를 막아 좌→우 제스처를 RecordScreen 의 "녹음 취소"로만 쓰이게 한다.
    val recordState by recordViewModel.state.collectAsStateWithLifecycle()
    val pagerScrollEnabled = recordState.phase != RecordPhase.RECORDING
    // HorizontalPager 는 인접 페이지(page 0/1) 를 동시에 compose 하므로, autoStart 신호를 RecordScreen
    // 에 그대로 넘기면 page 0(Home) 머무는 동안에도 녹음이 시작될 수 있다. page 1 이 실제 활성 상태
    // (currentPage=1 + targetPage=1, 즉 스크롤 안정화) 일 때만 신호를 전달한다.
    val isRecordPageActive = pagerState.currentPage == 1 && pagerState.targetPage == 1
    val recordAutoStartTrigger = if (hasTrigger && isRecordPageActive) autoStartRecordTrigger else 0L
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = pagerScrollEnabled,
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                onIvClick = onIvClick,
                onSttClick = onSttClick,
                pagerCurrentPage = pagerState.currentPage,
            )
            1 -> RecordScreen(
                viewModel = recordViewModel,
                onShowResult = onShowResult,
                pagerCurrentPage = pagerState.currentPage,
                autoStartTrigger = recordAutoStartTrigger,
                onAutoStartConsumed = onAutoStartConsumed,
            )
        }
    }
}
