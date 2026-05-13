// 타이머 페이지 — 수액타이머 / 워치알람 2탭
package com.happynurse.presentation.screens.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.presentation.components.EmptyState
import com.happynurse.presentation.components.IvTimerCard
import com.happynurse.presentation.components.IvTimerLayout
import com.happynurse.presentation.components.NotificationBell
import com.happynurse.presentation.components.PageHeader
import com.happynurse.presentation.theme.HnColors

private const val TAB_IV = "iv"
private const val TAB_WATCH = "watch"
private val TAB_ORDER = listOf(TAB_IV, TAB_WATCH)

@Composable
fun TasksScreen(
    onOpenNotifications: () -> Unit,
    upcomingCount: Int,
    ivLayout: IvTimerLayout = IvTimerLayout.BAR,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    var tab by remember { mutableStateOf(TAB_IV) }
    var direction by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        viewModel.refreshIvBoard()
        viewModel.refreshWatchAlarms()
    }
    val ivTimers by viewModel.ivTimers.collectAsStateWithLifecycle()
    val watchAlarms by viewModel.watchAlarms.collectAsStateWithLifecycle()

    val swipeModifier = Modifier.pointerInput(tab) {
        var dragX = 0f
        val threshold = 80f
        detectHorizontalDragGestures(
            onDragStart = { dragX = 0f },
            onDragEnd = {
                if (kotlin.math.abs(dragX) >= threshold) {
                    val idx = TAB_ORDER.indexOf(tab)
                    val forward = dragX < 0
                    val nextIdx = if (forward) (idx + 1).coerceAtMost(TAB_ORDER.lastIndex)
                                  else (idx - 1).coerceAtLeast(0)
                    if (nextIdx != idx) {
                        direction = if (forward) 1 else -1
                        tab = TAB_ORDER[nextIdx]
                    }
                }
                dragX = 0f
            },
            onDragCancel = { dragX = 0f },
            onHorizontalDrag = { _, amount -> dragX += amount },
        )
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(title = "타이머", right = { NotificationBell(unreadCount = upcomingCount, onClick = onOpenNotifications) })
        TasksTabBar(tab) { newTab ->
            val curIdx = TAB_ORDER.indexOf(tab)
            val newIdx = TAB_ORDER.indexOf(newTab)
            direction = if (newIdx > curIdx) 1 else -1
            tab = newTab
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val forward = direction >= 0
                (slideInHorizontally(animationSpec = tween(280)) { w -> if (forward) w else -w } +
                    fadeIn(tween(220)))
                    .togetherWith(
                        slideOutHorizontally(animationSpec = tween(280)) { w -> if (forward) -w else w } +
                            fadeOut(tween(220)),
                    )
            },
            modifier = Modifier.fillMaxSize().then(swipeModifier),
            label = "tasks-tab-swipe",
        ) { currentTab ->
            when (currentTab) {
                TAB_IV -> {
                    val timers = ivTimers.sortedBy { it.endsAt.replace(":", "").toIntOrNull() ?: 0 }
                    if (timers.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.Inbox,
                            title = "진행 중인 수액이 없습니다",
                            subtitle = "담당 환자에게 시작된 수액이 표시됩니다",
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp,
                            ),
                        ) {
                            items(timers, key = { it.id }) { IvTimerCard(it, layout = ivLayout) }
                        }
                    }
                }
                TAB_WATCH -> {
                    if (watchAlarms.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.Inbox,
                            title = "예정된 워치 알람이 없습니다",
                            subtitle = "워치에서 음성으로 등록한 알람이 표시됩니다",
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp,
                            ),
                        ) {
                            items(watchAlarms, key = { it.sttReminderId }) { WatchAlarmCard(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksTabBar(active: String, onChange: (String) -> Unit) {
    val tabs = listOf(
        TAB_IV to "수액타이머",
        TAB_WATCH to "워치알람",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            tabs.forEach { (id, label) ->
                val on = id == active
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onChange(id) }
                        .height(46.dp),
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            label,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (on) HnColors.Primary else HnColors.TextSecondary,
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (on) HnColors.Primary else Color.Transparent),
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(HnColors.Border))
    }
}

