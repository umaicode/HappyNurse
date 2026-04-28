// 음성 녹음 ViewModel(Hilt) — AudioRecorder/GestureDetector/WearDataClient 통합으로 녹음→폰 전송
package com.happynurse.wear.presentation.screens.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.data.audio.AudioRecorder
import com.happynurse.wear.data.remote.WearDataClient
import com.happynurse.wear.data.sensor.GestureDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RecordingUiState {
    object Idle : RecordingUiState()
    object Recording : RecordingUiState()
    object Processing : RecordingUiState()
    data class Done(val filePath: String) : RecordingUiState()
    data class Error(val message: String) : RecordingUiState()
}

// 음성STT_020~023
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val gestureDetector: GestureDetector,
    private val wearDataClient: WearDataClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val uiState: StateFlow<RecordingUiState> = _uiState

    init {
        // 음성STT_020/021: 제스처 트리거 감지 (엄지+검지 두 번 맞대기)
        viewModelScope.launch {
            gestureDetector.gestureEvents.collect { event ->
                if (event == GestureDetector.GestureEvent.PINCH_DOUBLE) {
                    toggleRecording()
                }
            }
        }
    }

    // 음성STT_022: 수동 버튼 녹음 시작
    fun startRecording() {
        viewModelScope.launch {
            audioRecorder.startRecording()
            _uiState.value = RecordingUiState.Recording
        }
    }

    // 음성STT_022: 수동 버튼 녹음 종료 + 음성STT_023: 노이즈 캔슬링 후 폰 전송
    fun stopRecording() {
        viewModelScope.launch {
            _uiState.value = RecordingUiState.Processing
            val audioFile = audioRecorder.stopRecording()
            val filteredAudio = audioRecorder.applyNoiseCancellation(audioFile)
            wearDataClient.sendAudioToPhone(filteredAudio)
            _uiState.value = RecordingUiState.Done(filteredAudio)
        }
    }

    private fun toggleRecording() {
        when (_uiState.value) {
            RecordingUiState.Idle -> startRecording()
            RecordingUiState.Recording -> stopRecording()
            else -> {}
        }
    }
}
