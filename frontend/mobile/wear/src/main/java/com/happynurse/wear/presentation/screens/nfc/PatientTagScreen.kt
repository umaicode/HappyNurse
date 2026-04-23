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

// NFC_025: 환자 전자팔찌 ↔ 워치 NFC 태깅
@Composable
fun PatientTagScreen(
    navController: NavHostController,
    viewModel: PatientTagViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = uiState) {
            is PatientTagUiState.Waiting -> Text("환자 팔찌에\n워치를 가까이 대세요")
            is PatientTagUiState.Success -> Text("환자 인식 완료\n${state.patientName}")
            is PatientTagUiState.Error -> Text("태깅 실패\n수동 입력을 이용하세요")
        }
    }
}
