// HomeRecordPager — home(0) ↔ record(1) 두 페이지를 좌우 스와이프로 전환하는 HorizontalPager 호스트.
package com.happynurse.wear.presentation.screens.pager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.data.model.PatientSelfReport
import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.presentation.screens.home.HomeScreen
import com.happynurse.wear.presentation.screens.record.RecordScreen

@Composable
fun HomeRecordPager(
    onIvClick: (IvInfusionTimer) -> Unit,
    onSttClick: (SttTimer) -> Unit,
    onReqClick: (PatientSelfReport) -> Unit,
    onRecordingComplete: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                onIvClick = onIvClick,
                onSttClick = onSttClick,
                onReqClick = onReqClick,
                pagerCurrentPage = pagerState.currentPage,
            )
            1 -> RecordScreen(
                onRecordingComplete = onRecordingComplete,
                pagerCurrentPage = pagerState.currentPage,
            )
        }
    }
}
