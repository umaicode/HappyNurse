// 인계 화면 — 교대 인수인계 환자 카드 리스트
package com.happynurse.presentation.screens.handover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private data class SummaryBullet(val color: Color, val text: String)
private data class PatientSummary(
    val id: String,
    val name: String,
    val room: String,
    val time: String,
    val summary: String
)

private val bullets = listOf(
    SummaryBullet(Color(0xFFE53935), "[김OO 301호]: Vital Sign 알럿 — BP 158/95 이상"),
    SummaryBullet(Color(0xFFF9A825), "[이OO 302호]: 처방 변경 — 세파졸린 → 반코마이신"),
    SummaryBullet(Color(0xFF1E88E5), "신규 오더 3건 (약물 2, 처치 1)"),
    SummaryBullet(Color(0xFFBDBDBD), "[박OO 303호]: 수액 교체 예정 17:00")
)

private val patientSummaries = listOf(
    PatientSummary("1", "김OO", "301호", "예정 10:00", "혈액검사 (CBC) 채혈 미완료"),
    PatientSummary("2", "이OO", "302호", "예정 14:00", "드레싱 교환 미완료"),
    PatientSummary("3", "박OO", "303호", "예정 16:00", "수액 잔량 재확인")
)

@Composable
fun HandoverScreen(navController: NavController) {
    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            Header()
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { AiSummaryCard() }
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    Text(
                        "환자별 AI 요약",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
                items(patientSummaries) { summary ->
                    PatientSummaryCard(summary) {
                        navController.navigate("journal/${summary.id}")
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("교대 인계 자료", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("오전 → 오후 근무 · 인계 예정 15:00", fontSize = 13.sp, color = Color(0xFF888888))
    }
}

@Composable
private fun AiSummaryCard() {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F4FA))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("AI 요약", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            bullets.forEach { b ->
                Row(
                    Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        Modifier
                            .padding(top = 5.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(b.color)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        b.text,
                        fontSize = 13.sp,
                        color = if (b.color == Color(0xFFBDBDBD)) Color(0xFF888888) else b.color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PatientSummaryCard(summary: PatientSummary, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E8F0))
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(summary.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(summary.room, fontSize = 12.sp, color = Color(0xFF888888))
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(summary.time, fontSize = 12.sp, color = Color(0xFF888888))
                }
                Spacer(Modifier.height(4.dp))
                Text(summary.summary, fontSize = 13.sp, color = Color(0xFF333333))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color(0xFF888888))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HandoverPreview() {
    HappyNurseTheme { HandoverScreen(rememberNavController()) }
}
