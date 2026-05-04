// NFC token 진입 화면 — /nfc/patients/entry 호출 후 patient_detail 로 이동, 실패 시 에러 표시
package com.happynurse.presentation.screens.nfc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun NfcEntryScreen(
    navController: NavController,
    token: String,
    viewModel: NfcEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(token) {
        viewModel.resolve(token)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val current = state) {
            is NfcEntryUiState.Loading -> CircularProgressIndicator()
            is NfcEntryUiState.Success -> {
                LaunchedEffect(current.patientId) {
                    navController.navigate("patient_detail/${current.patientId}") {
                        popUpTo("nfc_entry/{token}") { inclusive = true }
                    }
                }
            }
            is NfcEntryUiState.InvalidToken -> ErrorView(
                title = "유효하지 않은 NFC 태그입니다",
                detail = current.message,
                onBack = { navController.popBackStack() },
            )
            is NfcEntryUiState.NetworkError -> ErrorView(
                title = "서버에 연결할 수 없습니다",
                detail = current.message,
                onBack = { navController.popBackStack() },
            )
            is NfcEntryUiState.UnknownError -> ErrorView(
                title = "알 수 없는 오류가 발생했습니다",
                detail = current.message,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun ErrorView(title: String, detail: String, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title)
        Text(detail)
        Button(onClick = onBack) { Text("돌아가기") }
    }
}
