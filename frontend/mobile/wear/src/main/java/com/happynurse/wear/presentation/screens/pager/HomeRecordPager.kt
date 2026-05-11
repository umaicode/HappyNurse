// HomeRecordPager — home(0) ↔ record(1) 두 페이지를 좌우 스와이프로 전환하는 HorizontalPager 호스트.
// autoStartRecord=true 면 record 페이지(1)로 초기 진입하고 RecordScreen 에 자동 시작 신호 전달.
package com.happynurse.wear.presentation.screens.pager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.presentation.screens.home.HomeScreen
import com.happynurse.wear.presentation.screens.record.RecordScreen
import com.happynurse.wear.presentation.screens.record.RecordViewModel

@Composable
fun HomeRecordPager(
    recordViewModel: RecordViewModel,
    onIvClick: (IvInfusionTimer) -> Unit,
    onSttClick: (SttTimer) -> Unit,
    onShowResult: () -> Unit,
    autoStartRecord: Boolean = false,
    onAutoStartConsumed: () -> Unit = {},
) {
    val initialPage = if (autoStartRecord) 1 else 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
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
                autoStart = autoStartRecord,
                onAutoStartConsumed = onAutoStartConsumed,
            )
        }
    }
}
