// 타이머 STT 녹음 ViewModel(Hilt)
// 워치는 raw audio 만 캡처해 폰으로 송신, STT 처리는 서버. 폰이 서버 응답을 받아 /timer/start 로 회신해 타이머 시작
package com.happynurse.wear.presentation.screens.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.data.audio.AudioRecorder
import com.happynurse.wear.data.remote.WearDataClient
import com.happynurse.wear.data.remote.WearableMessagePaths
import com.happynurse.wear.data.sensor.GestureDetector
import java.io.File
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RecordingUiState {
    object Idle : RecordingUiState()
    object Recording : RecordingUiState()
    object Uploading : RecordingUiState() // raw audio 폰 전송 중 (폰이 서버 STT 후 /timer/start 로 회신 대기)
    data class Done(val filePath: String) : RecordingUiState()
    data class Error(val message: String) : RecordingUiState()
}

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val gestureDetector: GestureDetector,
    private val wearDataClient: WearDataClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecordingUiState>(RecordingUiState.Idle)
    val uiState: StateFlow<RecordingUiState> = _uiState

    init {
        viewModelScope.launch {
            gestureDetector.gestureEvents.collect { event ->
                if (event == GestureDetector.GestureEvent.PINCH_DOUBLE) {
                    toggleRecording()
                }
            }
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            audioRecorder.startRecording()
            _uiState.value = RecordingUiState.Recording
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            _uiState.value = RecordingUiState.Uploading
            val audioFile = audioRecorder.stopRecording()
            val filteredAudio = audioRecorder.applyNoiseCancellation(audioFile)
            wearDataClient.send(WearableMessagePaths.AUDIO_TIMER, File(filteredAudio).readBytes())
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
