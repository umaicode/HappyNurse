// 워치 메인 화면 — 좌측 페이지: 수액/타이머/환자 알림 탭, 우측 페이지: 타이머 STT 녹음
package com.happynurse.wear.presentation.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.happynurse.wear.presentation.screens.recording.RecordingScreen

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> NotificationListPage(viewModel)
            else -> RecordingScreen()
        }
    }
}
