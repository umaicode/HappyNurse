// RecordViewModel — 녹음 → STT 인식 → 시간 파싱(preview) → 결과 표시 → 등록 흐름을 관리한다.
// RecordScreen 과 SttResultScreen 이 같은 인스턴스를 공유하도록 NavGraph 에서 단일 ViewModel 로 주입한다.
package com.happynurse.wear.presentation.screens.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.data.audio.AudioRecorder
import com.happynurse.wear.data.repository.SttRecognitionRepository
import com.happynurse.wear.data.repository.SttReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class RecordPhase {
    IDLE,
    RECORDING,
    PROCESSING, // 음성 인식 + 시간 파싱
    RESULT,
    SUBMITTING,
    DONE,
    ERROR,
}

data class RecordUiState(
    val phase: RecordPhase = RecordPhase.IDLE,
    val elapsedSec: Int = 0,
    val recognizedText: String = "",
    val fireAtEpochMillis: Long? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val sttRecognitionRepository: SttRecognitionRepository,
    private val sttReminderRepository: SttReminderRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RecordUiState())
    val state: StateFlow<RecordUiState> = _state.asStateFlow()

    private var elapsedJob: Job? = null
    private var currentAudio: File? = null

    fun startRecording() {
        runCatching { audioRecorder.start() }
            .onFailure { fail("녹음을 시작할 수 없어요"); return }
        _state.update { RecordUiState(phase = RecordPhase.RECORDING, elapsedSec = 0) }
        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            var sec = 0
            while (sec < MAX_RECORD_SEC) {
                delay(1_000)
                sec += 1
                _state.update { it.copy(elapsedSec = sec) }
            }
            stopRecording()
        }
    }

    fun stopRecording() {
        elapsedJob?.cancel()
        elapsedJob = null
        val file = runCatching { audioRecorder.stop() }.getOrNull()
        currentAudio = file
        if (file == null || !file.exists() || file.length() == 0L) {
            fail("녹음 파일이 비어 있어요")
            return
        }
        viewModelScope.launch { processRecording(file) }
    }

    fun cancelRecording() {
        elapsedJob?.cancel()
        elapsedJob = null
        runCatching { audioRecorder.cancel() }
        currentAudio = null
        _state.value = RecordUiState()
    }

    private suspend fun processRecording(file: File) {
        _state.update { it.copy(phase = RecordPhase.PROCESSING, errorMessage = null) }
        val recognizeResult = sttRecognitionRepository.recognize(file)
        val recognized = recognizeResult.getOrElse {
            fail(it.message ?: "음성 인식에 실패했어요")
            return
        }
        val text = recognized.correctedText.takeUnless { it.isNullOrBlank() }
            ?: recognized.originalText.orEmpty()
        if (text.isBlank()) {
            fail("음성에서 텍스트를 추출하지 못했어요")
            return
        }
        val previewResult = sttReminderRepository.previewFireAt(text)
        val fireAt = previewResult.getOrElse {
            fail(it.message ?: "시간 표현을 인식하지 못했어요")
            return
        }
        _state.update {
            it.copy(
                phase = RecordPhase.RESULT,
                recognizedText = text,
                fireAtEpochMillis = fireAt,
            )
        }
    }

    fun confirm() {
        val current = _state.value
        val fireAt = current.fireAtEpochMillis ?: return
        if (current.phase == RecordPhase.SUBMITTING) return
        viewModelScope.launch {
            _state.update { it.copy(phase = RecordPhase.SUBMITTING, errorMessage = null) }
            val result = sttReminderRepository.create(
                sttText = current.recognizedText,
                fireAtEpochMillis = fireAt,
            )
            result.fold(
                onSuccess = { _state.update { it.copy(phase = RecordPhase.DONE) } },
                onFailure = { fail(it.message ?: "알람 등록에 실패했어요") },
            )
        }
    }

    fun reset() {
        cancelRecording()
    }

    fun consumeDone() {
        // 등록 완료 후 화면 전환을 마치고 호출 — 다음 녹음을 위해 초기화
        _state.value = RecordUiState()
    }

    private fun fail(message: String) {
        _state.update { it.copy(phase = RecordPhase.ERROR, errorMessage = message) }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { audioRecorder.cancel() }
    }

    private companion object {
        const val MAX_RECORD_SEC = 60
    }
}
