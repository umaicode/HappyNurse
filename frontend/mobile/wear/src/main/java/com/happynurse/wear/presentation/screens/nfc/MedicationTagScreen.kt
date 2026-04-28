// 약물 NFC 태그 화면(워치) — 약물 태깅 후 일치/불일치/오류 상태 표시(5 Rights)
package com.happynurse.wear.presentation.screens.nfc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Text

// NFC_026: 약물 ↔ 워치 NFC 태깅 (5 Rights 투약 원칙 연계)
@Composable
fun MedicationTagScreen(
    navController: NavHostController,
    viewModel: MedicationTagViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = uiState) {
            is MedicationTagUiState.Waiting -> Text("약물 태그에\n워치를 가까이 대세요")
            is MedicationTagUiState.Matched -> Text("투약 일치\n${state.medicationName}")
            is MedicationTagUiState.Mismatched -> Text("⚠ 투약 불일치!\n처방을 확인하세요")
            is MedicationTagUiState.Error -> Text("태깅 실패\n수동 입력을 이용하세요")
        }
    }
}
