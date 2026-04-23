package com.happynurse.wear.presentation.screens.recording

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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text

// 음성STT_020~023: 제스처/버튼 녹음 시작/종료, 노이즈 캔슬링 후 폰 전송
@Composable
fun RecordingScreen(
    navController: NavHostController,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (uiState) {
            RecordingUiState.Idle -> {
                Text("녹음 대기 중")
                Button(onClick = { viewModel.startRecording() }) {
                    Text("녹음 시작")
                }
            }
            RecordingUiState.Recording -> {
                Text("● 녹음 중...")
                Button(onClick = { viewModel.stopRecording() }) {
                    Text("녹음 종료")
                }
            }
            RecordingUiState.Processing -> {
                Text("음성 전송 중...")
            }
            is RecordingUiState.Done -> {
                Text("전송 완료")
            }
            is RecordingUiState.Error -> {
                Text("오류 발생")
            }
        }
    }
}
