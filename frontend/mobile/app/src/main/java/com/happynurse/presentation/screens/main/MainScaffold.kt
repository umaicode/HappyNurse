// 메인 스캐폴드 — 4탭 BottomNav + 알림 시트 호스팅
package com.happynurse.presentation.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.happynurse.core.sample.SampleData
import com.happynurse.presentation.components.BottomNav
import com.happynurse.presentation.components.HnTab
import com.happynurse.presentation.components.IVTimerLayout
import com.happynurse.presentation.components.NotificationsSheet
import com.happynurse.presentation.components.PatientLayout
import com.happynurse.presentation.screens.alarms.AlarmsScreen
import com.happynurse.presentation.screens.handoff.HandoffScreen
import com.happynurse.presentation.screens.mypage.MyPageScreen
import com.happynurse.presentation.screens.patients.PatientsScreen
import com.happynurse.presentation.theme.HnColors

@Composable
fun MainScaffold(
    onOpenPatient: (String) -> Unit,
    onOpenNFC: () -> Unit,
    onLogout: () -> Unit,
) {
    var tab by remember { mutableStateOf(HnTab.PATIENTS) }
    var notifOpen by remember { mutableStateOf(false) }
    val upcoming = SampleData.notifications.count { it.upcoming }

    Box(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    HnTab.PATIENTS -> PatientsScreen(
                        onOpenPatient = onOpenPatient,
                        onOpenNFC = onOpenNFC,
                        onOpenNotifications = { notifOpen = true },
                        upcomingCount = upcoming,
                        layout = PatientLayout.CARD,
                    )
                    HnTab.ALARMS -> AlarmsScreen(
                        onOpenNotifications = { notifOpen = true },
                        upcomingCount = upcoming,
                        ivLayout = IVTimerLayout.BAR,
                    )
                    HnTab.HANDOFF -> HandoffScreen(
                        onOpenNotifications = { notifOpen = true },
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
            notifications = SampleData.notifications,
            onClose = { notifOpen = false },
        )
    }
}
