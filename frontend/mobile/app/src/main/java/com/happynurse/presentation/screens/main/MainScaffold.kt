// 메인 스캐폴드 — 4탭 BottomNav + 알림 시트 호스팅
package com.happynurse.presentation.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.happynurse.presentation.components.IVTimerLayout
import com.happynurse.presentation.components.NotificationsSheet
import com.happynurse.presentation.components.PatientLayout
import com.happynurse.presentation.screens.alarms.AlarmsScreen
import com.happynurse.presentation.screens.alarms.AlarmsViewModel
import com.happynurse.presentation.screens.handoff.HandoffScreen
import com.happynurse.presentation.screens.mypage.MyPageScreen
import com.happynurse.presentation.screens.patients.PatientsScreen
import com.happynurse.presentation.theme.HnColors

@Composable
fun MainScaffold(
    onOpenPatient: (String) -> Unit,
    onLogout: () -> Unit,
) {
    var tab by remember { mutableStateOf(HnTab.PATIENTS) }
    var notifOpen by remember { mutableStateOf(false) }
    // AlarmsViewModel 은 같은 NavBackStackEntry scope — AlarmsScreen 과 instance 공유
    val alarmsViewModel: AlarmsViewModel = hiltViewModel()
    val notifs by alarmsViewModel.notifs.collectAsStateWithLifecycle()
    val upcoming = notifs.count { it.upcoming }
    // 탭 전환 시마다 refresh (담당환자 변경 후 다른 탭 → 벨 카운트 / 시트 자동 갱신)
    LaunchedEffect(tab) { alarmsViewModel.refreshAlarms() }
    // 벨 클릭 시 한 번 더 refresh — 같은 탭에 머물러 있는 동안 변경된 경우 대비
    val openNotifications: () -> Unit = {
        alarmsViewModel.refreshAlarms()
        notifOpen = true
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
                    HnTab.ALARMS -> AlarmsScreen(
                        onOpenNotifications = openNotifications,
                        upcomingCount = upcoming,
                        ivLayout = IVTimerLayout.BAR,
                    )
                    HnTab.HANDOFF -> HandoffScreen(
                        onOpenNotifications = openNotifications,
                        upcomingCount = upcoming,
                    )
                    HnTab.ME -> MyPageScreen(
                        onLogout = onLogout,
                        onOpenPatient = onOpenPatient,
                    )
                }
            }
            BottomNav(active = tab, onChange = { tab = it })
        }
        NotificationsSheet(
            visible = notifOpen,
            notifications = notifs,
            onClose = { notifOpen = false },
        )
    }
}
