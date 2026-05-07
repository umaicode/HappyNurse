// HomeScreen — 워치 홈(명세 §1). 3개 탭(수액/타이머/환자알림) + 카드 리스트 + 페이지 인디케이터.
// 좌→우 스와이프로 record 페이지 전환은 상위 HomeRecordPager 가 담당.
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
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.data.model.PatientSelfReport
import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.presentation.components.HnPagerIndicator
import com.happynurse.wear.presentation.components.HnSegmentedTabs
import com.happynurse.wear.presentation.components.HnTab
import com.happynurse.wear.presentation.screens.home.tabs.IvListTab
import com.happynurse.wear.presentation.screens.home.tabs.PatientReportListTab
import com.happynurse.wear.presentation.screens.home.tabs.SttListTab

@Composable
fun HomeScreen(
    onIvClick: (IvInfusionTimer) -> Unit,
    onSttClick: (SttTimer) -> Unit,
    onReqClick: (PatientSelfReport) -> Unit,
    pagerCurrentPage: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tabs = listOf(
        HnTab(HomeTab.IV.label, state.ivList.size),
        HnTab(HomeTab.STT.label, state.sttList.size),
        HnTab(HomeTab.REQ.label, state.reqList.size),
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            HnSegmentedTabs(
                tabs = tabs,
                selectedIndex = state.selectedTab.ordinal,
                onSelected = { idx -> viewModel.selectTab(HomeTab.values()[idx]) },
            )
            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                when (state.selectedTab) {
                    HomeTab.IV -> IvListTab(
                        items = state.ivList,
                        onCardClick = onIvClick,
                        onDelete = viewModel::deleteIv,
                    )
                    HomeTab.STT -> SttListTab(
                        items = state.sttList,
                        onCardClick = onSttClick,
                        onDelete = viewModel::deleteStt,
                    )
                    HomeTab.REQ -> PatientReportListTab(
                        items = state.reqList,
                        onCardClick = onReqClick,
                        onDelete = viewModel::deleteReq,
                    )
                }
            }
            Text(
                text = "← 카드를 왼쪽으로 밀면 삭제",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
