// 타이머 STT 녹음 화면(워치) — 녹음 → 폰 송신 → 서버 STT → 폰 회신 → 타이머 시작
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text

@Composable
fun RecordingScreen(viewModel: RecordingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (uiState) {
            RecordingUiState.Idle -> {
                Text("녹음 대기 중")
                Button(onClick = { viewModel.startRecording() }) {
                    Text("녹음 시작")
                }
            }
            RecordingUiState.Recording -> {
                Text("녹음 중...")
                Button(onClick = { viewModel.stopRecording() }) {
                    Text("녹음 종료")
                }
            }
            RecordingUiState.Uploading -> {
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
