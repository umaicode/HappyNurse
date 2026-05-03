// ============================================================================
// 환자 상세 화면 — NFC 진입 검증용 임시 화면
// API 연동 + 디자인 와이어프레임 반영 시 이 파일 전체를 교체
// (받는 인자: navController.currentBackStackEntry?.arguments?.getString("id"))
// ============================================================================
package com.happynurse.presentation.screens.patient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun PatientDetailScreen(navController: NavController) {
    val patientId = navController.currentBackStackEntry
        ?.arguments
        ?.getString("id")
        ?: "(인자 없음)"

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = "환자 상세 (테스트 화면)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(text = "🛠️ patientId = $patientId", fontSize = 18.sp)
            Text(
                text = "NFC 진입 + 토큰 검증 + navigate 가 정상 작동하면 이 화면이 표시됩니다.",
                fontSize = 14.sp,
            )
            Text(
                text = "API 연동 시 이 파일 전체를 실제 환자 상세 UI 로 교체하세요.",
                fontSize = 12.sp,
            )
        }
    }
}
