// 알람 탭 — 워치타이머(간호 알람)/수액타이머 두 서브탭
package com.happynurse.presentation.screens.alarms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.core.sample.SampleData
import com.happynurse.domain.model.NurseAlarm
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.components.IVTimerCard
import com.happynurse.presentation.components.IVTimerLayout
import com.happynurse.presentation.components.NotifBell
import com.happynurse.presentation.components.PageHeader
import com.happynurse.presentation.theme.HnColors

@Composable
fun AlarmsScreen(
    onOpenNotifications: () -> Unit,
    upcomingCount: Int,
    ivLayout: IVTimerLayout = IVTimerLayout.BAR,
) {
    var tab by remember { mutableStateOf("nurse") }
    val alarms = remember { SampleData.nurseAlarms.sortedBy { it.date + it.time } }
    val timers = remember { SampleData.ivTimers.sortedBy { it.endsAt.replace(":", "").toIntOrNull() ?: 0 } }

    Column(Modifier.fillMaxWidth()) {
        PageHeader(title = "알람", right = { NotifBell(unreadCount = upcomingCount, onClick = onOpenNotifications) })
        SubTabBar(tab) { tab = it }
        if (tab == "nurse") {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            ) {
                items(alarms, key = { it.id }) { NurseAlarmCard(it) }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            ) {
                items(timers, key = { it.id }) { IVTimerCard(it, layout = ivLayout) }
            }
        }
    }
}

@Composable
private fun SubTabBar(active: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            listOf("nurse" to "전체 알림", "iv" to "수액타이머").forEach { (id, label) ->
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
                            fontSize = 14.sp,
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

@Composable
private fun NurseAlarmCard(a: NurseAlarm) {
    HnCard(padding = 14.dp) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.patient, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(a.room, fontSize = 12.sp, color = HnColors.Text)
                Spacer(Modifier.weight(1f))
                Text(a.time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HnColors.Primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(a.text, fontSize = 14.sp, color = HnColors.Text)
            Spacer(Modifier.height(8.dp))
            Text("생성됨: ${a.createdTime}", fontSize = 12.sp, color = HnColors.TextSecondary)
        }
    }
}
