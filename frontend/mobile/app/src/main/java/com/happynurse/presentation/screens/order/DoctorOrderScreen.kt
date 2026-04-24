package com.happynurse.presentation.screens.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
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
import com.happynurse.presentation.components.DoctorOrder
import com.happynurse.presentation.components.OrderCard
import com.happynurse.presentation.components.OrderCategory
import com.happynurse.presentation.components.OrderStatus
import com.happynurse.presentation.components.PatientTab
import com.happynurse.presentation.components.PatientTabBar
import com.happynurse.presentation.components.TagChip
import com.happynurse.presentation.components.TagColors
import com.happynurse.presentation.ui.theme.HappyNurseTheme

private val patientTabs = listOf(
    PatientTab("1", "김OO"),
    PatientTab("2", "이OO"),
    PatientTab("3", "박OO")
)

private val sampleOrders = listOf(
    DoctorOrder(
        category = OrderCategory.Rx,
        status = OrderStatus.InProgress,
        code = "CEF001",
        name = "세파졸린 1g",
        dose = "1g",
        count = "3회",
        unit = "vial",
        days = "3일",
        route = "IV",
        usage = "8시간마다",
        note = "알레르기 확인 필수. 생리식염수 100ml에 희석 후 30분간 점적."
    ),
    DoctorOrder(
        category = OrderCategory.Rx,
        status = OrderStatus.Prescribed,
        code = "ACE500",
        name = "아세트아미노펜 500mg",
        dose = "500mg",
        count = "PRN",
        unit = "tab",
        days = "필요시",
        route = "PO",
        usage = "6시간마다",
        note = "통증 호소 시 투약. 24시간 내 4g 초과 금지."
    ),
    DoctorOrder(
        category = OrderCategory.Fluid,
        status = OrderStatus.InProgress,
        code = "NS0901",
        name = "생리식염수 1L",
        dose = "1000ml",
        count = "1회",
        unit = "bag",
        days = "1일",
        route = "IV",
        usage = "80ml/hr",
        note = "수액 잔량 확인 2시간마다."
    ),
    DoctorOrder(
        category = OrderCategory.Procedure,
        status = OrderStatus.Prescribed,
        code = "DRS14",
        name = "상처 드레싱",
        dose = "-",
        count = "1회",
        unit = "-",
        days = "매일",
        route = "Local",
        usage = "14:00",
        note = "소독 후 거즈 교환. 삼출물 관찰."
    ),
    DoctorOrder(
        category = OrderCategory.LIS,
        status = OrderStatus.Prescribed,
        code = "LAB-CBC",
        name = "혈액검사 (CBC, CMP)",
        dose = "-",
        count = "1회",
        unit = "-",
        days = "당일",
        route = "채혈",
        usage = "오전 10:00",
        note = "공복 채혈 불필요."
    ),
    DoctorOrder(
        category = OrderCategory.Imaging,
        status = OrderStatus.Hold,
        code = "CXR-PA",
        name = "흉부 X-ray (PA view)",
        dose = "-",
        count = "1회",
        unit = "-",
        days = "-",
        route = "영상의학과",
        usage = "오후 예약",
        note = "이동식 X-ray 요청 시 간호사실 연락."
    )
)

@Composable
fun DoctorOrderScreen(navController: NavController) {
    val inProgressCount = sampleOrders.count { it.status == OrderStatus.InProgress }
    val prescribedCount = sampleOrders.count { it.status == OrderStatus.Prescribed }

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
            Spacer(Modifier.height(8.dp))
            PatientTabBar(
                tabs = patientTabs,
                selectedId = "1",
                onSelect = {}
            )
            Spacer(Modifier.height(12.dp))
            SummaryRow(total = sampleOrders.size, inProgressCount, prescribedCount)
            HorizontalDivider(color = Color(0xFFEAEAF0))

            LazyColumn(
                Modifier.fillMaxSize().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(sampleOrders) { OrderCard(it) }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("의사 오더", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("김OO · 301호 · 폐렴 (J18.9)", fontSize = 13.sp, color = Color(0xFF888888))
    }
}

@Composable
private fun SummaryRow(total: Int, inProgress: Int, prescribed: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "총 ${total}건  2026.04.14 기준",
            fontSize = 12.sp,
            color = Color(0xFF888888),
            modifier = Modifier.weight(1f)
        )
        TagChip(
            label = "진행중 $inProgress",
            background = TagColors.StatusInProgress.first,
            foreground = TagColors.StatusInProgress.second
        )
        Spacer(Modifier.width(6.dp))
        TagChip(
            label = "처방 $prescribed",
            background = TagColors.StatusPrescribed.first,
            foreground = TagColors.StatusPrescribed.second
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun DoctorOrderPreview() {
    HappyNurseTheme { DoctorOrderScreen(rememberNavController()) }
}
