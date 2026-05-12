// 메인 스캐폴드 — 4탭 BottomNav + 알림 시트 호스팅
package com.happynurse.presentation.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.presentation.components.BottomNav
import com.happynurse.presentation.components.HnTab
import com.happynurse.presentation.components.IvTimerLayout
import com.happynurse.presentation.components.NotificationsSheet
import com.happynurse.presentation.components.PatientLayout
import com.happynurse.presentation.screens.handoff.HandoffScreen
import com.happynurse.presentation.screens.mypage.MyPageScreen
import com.happynurse.presentation.screens.patients.PatientsScreen
import com.happynurse.presentation.screens.tasks.TasksScreen
import com.happynurse.presentation.screens.tasks.TasksViewModel
import com.happynurse.presentation.theme.HnColors

@Composable
fun MainScaffold(
    onOpenPatient: (String) -> Unit,
    onLogout: () -> Unit,
) {
    var tab by remember { mutableStateOf(HnTab.PATIENTS) }
    var notificationOpen by remember { mutableStateOf(false) }
    // TasksViewModel 은 같은 NavBackStackEntry scope — TasksScreen 과 instance 공유
    val tasksViewModel: TasksViewModel = hiltViewModel()
    val notifications by tasksViewModel.notifications.collectAsStateWithLifecycle()
    // 종 아이콘 배지 = 과거/미래 24시간 이내 알림 개수 (워치알람은 minutesAgo 음수)
    val upcoming = notifications.count { it.minutesAgo in -1440..1440 }
    // 탭 전환 시마다 refresh (담당환자 변경 후 다른 탭 → 벨 카운트 / 시트 자동 갱신)
    LaunchedEffect(tab) { tasksViewModel.refreshBellNotifications() }
    val openNotifications: () -> Unit = {
        tasksViewModel.refreshBellNotifications()
        notificationOpen = true
    }

    Box(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    HnTab.PATIENTS -> PatientsScreen(
                        onOpenPatient = onOpenPatient,
                        onOpenNotifications = openNotifications,
                        upcomingCount = upcoming,
                        layout = PatientLayout.CARD,
                    )
                    HnTab.TASKS -> TasksScreen(
                        onOpenNotifications = openNotifications,
                        upcomingCount = upcoming,
                        ivLayout = IvTimerLayout.BAR,
                    )
                    HnTab.HANDOFF -> HandoffScreen()
                    HnTab.ME -> MyPageScreen(
                        onLogout = onLogout,
                        onOpenPatient = onOpenPatient,
                        onOpenNotifications = openNotifications,
                        upcomingCount = upcoming,
                    )
                }
            }
            BottomNav(active = tab, onChange = { tab = it })
        }
        NotificationsSheet(
            visible = notificationOpen,
            notifications = notifications,
            onClose = { notificationOpen = false },
            onDelete = tasksViewModel::dismissNotification,
            onDeleteAll = tasksViewModel::dismissAllNotifications,
        )
    }
}
