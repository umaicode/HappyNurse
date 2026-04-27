// 환자 목록 화면 — LazyColumn으로 환자 카드 리스트 + 상단 알림 아이콘
package com.happynurse.presentation.screens.patient

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.happynurse.presentation.components.BottomNavBar
import com.happynurse.presentation.ui.theme.HappyNurseTheme

private data class Patient(
    val id: String,
    val room: String,
    val name: String,
    val admittedAt: String,
    val nurse: String,
    val tag: String?
)

private val wards = listOf("A병동", "B병동", "C병동")
private val rooms = listOf("전체", "301호", "302호", "303호", "304호", "305호")

private val samplePatients = listOf(
    Patient("1", "301호", "김OO", "2026.04.01", "박지수", "의사 오더"),
    Patient("2", "302호", "이OO", "2026.04.02", "박지수", "의사 오더"),
    Patient("3", "303호", "박OO", "2026.04.03", "김민지", "의사 오더"),
    Patient("4", "304호", "최OO", "2026.04.05", "이수아", "안정"),
    Patient("5", "305호", "정OO", "2026.04.08", "박지수", "의사 오더")
)

@Composable
fun PatientListScreen(navController: NavController) {
    var selectedWard by remember { mutableStateOf("A병동") }
    var selectedRoom by remember { mutableStateOf("전체") }

    val filtered = samplePatients.filter {
        selectedRoom == "전체" || it.room == selectedRoom
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            Header(selectedWard, filtered.size)
            Spacer(Modifier.height(8.dp))

            SectionLabel("병동 선택")
            ChipRow(wards, selectedWard) { selectedWard = it }
            Spacer(Modifier.height(12.dp))

            SectionLabel("호수 선택")
            ChipRow(rooms, selectedRoom) { selectedRoom = it }
            Spacer(Modifier.height(12.dp))

            HorizontalDivider(color = Color(0xFFEAEAF0))
            ListHeader()
            HorizontalDivider(color = Color(0xFFEAEAF0))

            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered) { patient ->
                    PatientRow(patient) {
                        navController.navigate("journal/${patient.id}")
                    }
                    HorizontalDivider(color = Color(0xFFF0F0F5))
                }
            }
        }
    }
}

@Composable
private fun Header(ward: String, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("환자 목록", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("$ward · 총 ${count}명", fontSize = 13.sp, color = Color(0xFF888888))
        }
        IconButton(onClick = {}) {
            Icon(Icons.Filled.Notifications, contentDescription = "알림")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF333333),
        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun ChipRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            OutlinedButton(
                onClick = { onSelect(item) },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFDDDDDD)
                )
            ) {
                Text(
                    item,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF666666),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ListHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        HeaderCell("호실", 0.22f)
        HeaderCell("환자명", 0.22f)
        HeaderCell("입원일", 0.33f)
        HeaderCell("담당자", 0.23f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF444444),
        modifier = Modifier.weight(weight)
    )
}

@Composable
private fun PatientRow(patient: Patient, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(patient.room, fontSize = 14.sp, modifier = Modifier.weight(0.22f))
            Text(patient.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.22f))
            Text(patient.admittedAt, fontSize = 13.sp, color = Color(0xFF666666), modifier = Modifier.weight(0.33f))
            Text(patient.nurse, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.23f))
        }
        patient.tag?.let {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFEAEAF0))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(it, fontSize = 11.sp, color = Color(0xFF444444))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PatientListPreview() {
    HappyNurseTheme { PatientListScreen(rememberNavController()) }
}
