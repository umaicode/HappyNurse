// 간호 일지 화면 — 좌우 화살표로 날짜 이동, 카테고리별 타임라인 표시
package com.happynurse.presentation.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.happynurse.presentation.components.BottomNavBar
import com.happynurse.presentation.components.JournalCategory
import com.happynurse.presentation.components.JournalEntry
import com.happynurse.presentation.components.JournalTimelineItem
import com.happynurse.presentation.components.PatientTab
import com.happynurse.presentation.components.PatientTabBar
import com.happynurse.presentation.ui.theme.HappyNurseTheme

private val sampleTabs = listOf(
    PatientTab("1", "김OO (301호)"),
    PatientTab("2", "이OO (302호)"),
    PatientTab("3", "박OO (303호)")
)

private val sampleEntries = listOf(
    JournalEntry("08:00", JournalCategory.VitalSign, "V/S 체크 완료", "BP 120/80, HR 76, RR 18, T 36.5℃"),
    JournalEntry("09:05", JournalCategory.Medication, "약물 태깅: 세파졸린 1g", "IV 경로 확인, 부작용 없음"),
    JournalEntry("10:30", JournalCategory.Treatment, "드레싱 교환 기록", "우측 하퇴부 상처 소독 및 거즈 교환"),
    JournalEntry("12:00", JournalCategory.Diet, "점심 식사 섭취량 확인", "전량 섭취, 구역감 없음"),
    JournalEntry("13:00", JournalCategory.Medication, "아세트아미노펜 500mg 투약", "통증 호소로 PRN 투약"),
    JournalEntry("14:30", JournalCategory.Excretion, "배설 기록", "소변량 250ml, 양호"),
    JournalEntry("15:20", JournalCategory.VitalSign, "수액 잔량 확인", "잔량 250ml, 속도 유지"),
    JournalEntry("16:00", JournalCategory.Activity, "병동 내 보행", "복도 1회 보행, 통증 호소 없음"),
    JournalEntry("17:00", JournalCategory.Observation, "환자 상태 관찰", "의식 명료, 통증 NRS 2점"),
    JournalEntry("22:00", JournalCategory.Sleep, "수면 상태 확인", "수면 유도제 없이 입면")
)

@Composable
fun JournalScreen(navController: NavController) {
    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            JournalHeader()
            Spacer(Modifier.height(8.dp))
            PatientTabBar(
                tabs = sampleTabs,
                selectedId = "1",
                onSelect = {}
            )
            Spacer(Modifier.height(8.dp))
            DateNavigator("2026.04.14 (오전 근무)")
            HorizontalDivider(color = Color(0xFFEAEAF0))

            LazyColumn(
                Modifier.fillMaxSize().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sampleEntries) { JournalTimelineItem(it) }
            }
        }
    }
}

@Composable
private fun JournalHeader() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("김OO", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("담당 박지수 · A병동", fontSize = 13.sp, color = Color(0xFF888888))
    }
}

@Composable
private fun DateNavigator(label: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {}) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "이전 날짜")
        }
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = {}) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "다음 날짜")
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun JournalScreenPreview() {
    HappyNurseTheme { JournalScreen(rememberNavController()) }
}
