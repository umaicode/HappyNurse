// 환자 탭 — 내 담당환자/전체환자 토글, 호실별 그룹, NFC FAB
package com.happynurse.presentation.screens.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happynurse.core.sample.SampleData
import com.happynurse.domain.model.Patient
import com.happynurse.presentation.components.NotifBell
import com.happynurse.presentation.components.PageHeader
import com.happynurse.presentation.components.PatientCard
import com.happynurse.presentation.components.PatientLayout
import com.happynurse.presentation.theme.HnColors

@Composable
fun PatientsScreen(
    onOpenPatient: (String) -> Unit,
    onOpenNFC: () -> Unit,
    onOpenNotifications: () -> Unit,
    upcomingCount: Int,
    layout: PatientLayout = PatientLayout.CARD,
) {
    var listTab by remember { mutableStateOf("mine") }
    val myNurse = "김소연"
    val mine = remember { SampleData.patients.filter { it.nurse == myNurse } }
    val all = SampleData.patients

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PageHeader(
                title = "환자",
                sub = "2026년 4월 30일 (목) · 데이 근무",
                right = { NotifBell(unreadCount = upcomingCount, onClick = onOpenNotifications) },
            )
            TabRow(listTab) { listTab = it }
            val showing: List<Patient> = if (listTab == "mine") mine else all

            if (showing.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp,
                    ),
                ) {
                    val rooms = showing.map { it.room }.distinct().sorted()
                    rooms.forEach { room ->
                        val inRoom = showing.filter { it.room == room }
                        item(key = "h-$room") {
                            RoomHeader(room = room, count = inRoom.size)
                        }
                        items(inRoom, key = { it.id }) { p ->
                            PatientCard(
                                patient = p,
                                onClick = { onOpenPatient(p.id) },
                                layout = layout,
                                myNurseName = myNurse,
                            )
                        }
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .size(56.dp)
                .clip(CircleShape)
                .background(HnColors.Primary)
                .clickable(onClick = onOpenNFC),
        ) {
            Icon(Icons.Outlined.Nfc, contentDescription = "NFC", tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun TabRow(active: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        listOf("mine" to "내 담당환자", "all" to "전체 환자").forEach { (id, label) ->
            val on = id == active
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clickable { onChange(id) },
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (on) HnColors.Primary else HnColors.TextSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (on) HnColors.Primary else Color.Transparent),
                )
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(HnColors.Border))
}

@Composable
private fun RoomHeader(room: String, count: Int) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            "${room}호실 · ${count}명",
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = HnColors.TextTertiary, letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(HnColors.Border))
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).clip(CircleShape).background(HnColors.PrimarySoft),
        ) {
            Icon(Icons.Outlined.Nfc, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("담당 환자가 없습니다", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        Spacer(Modifier.height(8.dp))
        Text(
            "담당 환자를 등록하면 여기에 표시됩니다.",
            fontSize = 14.sp, color = HnColors.TextSecondary,
        )
    }
}
