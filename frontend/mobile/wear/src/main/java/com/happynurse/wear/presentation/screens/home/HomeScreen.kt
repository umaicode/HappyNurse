// HomeScreen — 워치 홈. 두 개의 알약형 탭(수액/타이머) + 카드 리스트 + 페이지 인디케이터.
// 시계(TimeText) 는 표시하지 않으며, 원형 베젤에 탭이 잘리지 않도록 상단에 안전 마진을 둔다.
package com.happynurse.wear.presentation.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.presentation.components.HnPagerIndicator
import com.happynurse.wear.presentation.components.HnSegmentedTabs
import com.happynurse.wear.presentation.components.HnTab
import com.happynurse.wear.presentation.screens.home.tabs.IvListTab
import com.happynurse.wear.presentation.screens.home.tabs.SttListTab

@Composable
fun HomeScreen(
    onIvClick: (IvInfusionTimer) -> Unit,
    onSttClick: (SttTimer) -> Unit,
    pagerCurrentPage: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tabs = listOf(
        HnTab(HomeTab.IV.label),
        HnTab(HomeTab.STT.label),
    )
    AppScaffold(timeText = {}) {
        ScreenScaffold {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 34.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HnSegmentedTabs(
                        tabs = tabs,
                        selectedIndex = state.selectedTab.ordinal,
                        onSelected = { idx -> viewModel.selectTab(HomeTab.entries[idx]) },
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        when (state.selectedTab) {
                            HomeTab.IV -> IvListTab(
                                items = state.ivList,
                                isLoading = state.isLoading,
                                errorMessage = state.errorMessage,
                                onCardClick = onIvClick,
                            )
                            HomeTab.STT -> SttListTab(
                                items = state.sttList,
                                isLoading = state.isLoading,
                                errorMessage = state.errorMessage,
                                onCardClick = onSttClick,
                            )
                        }
                    }
                }
                HnPagerIndicator(
                    pageCount = 2,
                    currentPage = pagerCurrentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                )
            }
        }
    }
}
