// HomeScreen — 워치 홈. 두 개의 알약형 탭(수액/타이머) + 카드 리스트 + 페이지 인디케이터.
// 시계(TimeText) 는 표시하지 않으며, 원형 베젤에 탭이 잘리지 않도록 상단에 안전 마진을 둔다.
package com.happynurse.wear.presentation.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import com.happynurse.wear.R
import com.happynurse.wear.domain.model.IvInfusionTimer
import com.happynurse.wear.domain.model.SttTimer
import com.happynurse.wear.presentation.components.HnPagerIndicator
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
    AppScaffold(timeText = {}) {
        ScreenScaffold {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircleTabRow(
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

@Composable
private fun CircleTabRow(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    val icons: List<@Composable () -> Unit> = listOf(
        {
            Icon(
                painter = painterResource(R.drawable.ic_infusion),
                contentDescription = HomeTab.IV.label,
                modifier = Modifier.size(30.dp),
            )
        },
        {
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = HomeTab.STT.label,
                modifier = Modifier.size(30.dp),
            )
        },
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icons.forEachIndexed { index, iconContent ->
            val isSelected = index == selectedIndex
            val targetBg = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceContainer
            val targetFg = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
            val bg by animateColorAsState(targetBg, label = "circleTabBg-$index")
            val fg by animateColorAsState(targetFg, label = "circleTabFg-$index")
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(
                        onClick = { onSelected(index) },
                        indication = null,
                        interactionSource = interactionSource,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                androidx.wear.compose.material3.LocalContentColor
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.wear.compose.material3.LocalContentColor provides fg,
                ) {
                    iconContent()
                }
            }
        }
    }
}
