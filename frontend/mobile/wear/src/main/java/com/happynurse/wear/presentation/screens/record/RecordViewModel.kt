// RecordViewModel — 녹음 → STT 인식 → 시간 파싱(preview) → 자동 등록 흐름을 관리한다.
// RecordScreen 과 SttResultScreen 이 같은 인스턴스를 공유하도록 NavGraph 에서 단일 ViewModel 로 주입한다.
// GestureService 와는 RecordingControlBus 로 통신: 녹음 중 = isRecording true, 작업 중 = isBusy true.
package com.happynurse.wear.presentation.screens.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.alarm.AlarmScheduler
import com.happynurse.wear.data.audio.AudioRecorder
import com.happynurse.wear.data.repository.SttRecognitionRepository
import com.happynurse.wear.data.repository.SttReminderRepository
import com.happynurse.wear.gesture.RecordingControlBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private val alarmScheduler: AlarmScheduler,
    private val recordingBus: RecordingControlBus,
) : ViewModel() {

    private val _state = MutableStateFlow(RecordUiState())
    val state: StateFlow<RecordUiState> = _state.asStateFlow()

    private var elapsedJob: Job? = null
    private var currentAudio: File? = null

    init {
        // 외부(GestureService 등) 의 stop 요청 → 녹음 중일 때만 stop. 다른 phase 면 무시.
        viewModelScope.launch {
            recordingBus.stopRequests.collect {
                if (_state.value.phase == RecordPhase.RECORDING) {
                    stopRecording()
                }
            }
        }
        // 결과 화면에서 싱글 스냅 → confirm. RESULT phase 에서만 유효.
        viewModelScope.launch {
            recordingBus.confirmRequests.collect {
                if (_state.value.phase == RecordPhase.RESULT) {
                    confirm()
                }
            }
        }
        // 현재 phase → bus 의 isRecording / isBusy / awaitingConfirm 상태 동기화
        viewModelScope.launch {
            _state
                .map { it.phase }
                .distinctUntilChanged()
                .collect { phase ->
                    recordingBus.setRecording(phase == RecordPhase.RECORDING)
                    // DONE 도 busy 로 유지 — 등록 직후 토스트 보는 동안 사용자의 우연한 손목 동작이
                    // DOUBLE_SNAP 으로 인식되어 새 녹음이 자동 시작되는 것을 방지.
                    recordingBus.setBusy(
                        phase == RecordPhase.RECORDING ||
                            phase == RecordPhase.PROCESSING ||
                            phase == RecordPhase.RESULT ||
                            phase == RecordPhase.SUBMITTING ||
                            phase == RecordPhase.DONE,
                    )
                    recordingBus.setAwaitingConfirm(phase == RecordPhase.RESULT)
                }
        }
    }

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
        // 자동 등록 안 함 — 사용자가 결과 화면에서 [등록] 버튼 또는 손목 싱글 스냅으로 confirm 트리거.
    }

    fun confirm() {
        val current = _state.value
        val fireAt = current.fireAtEpochMillis ?: return
        if (current.phase == RecordPhase.SUBMITTING) return
        viewModelScope.launch {
            _state.update { it.copy(phase = RecordPhase.SUBMITTING, errorMessage = null) }
            // 백엔드 등록 + 로컬 알람 스케줄링 어디서든 예외가 터져도 ViewModel scope 가 죽지 않도록
            // 전체를 try-catch 로 감싼다. 알람 스케줄링 실패는 등록 자체 성공으로 간주(베스트에포트).
            try {
                val result = sttReminderRepository.create(
                    sttText = current.recognizedText,
                    fireAtEpochMillis = fireAt,
                )
                result.fold(
                    onSuccess = { resp ->
                        val content = resp.contentSummary
                            ?.takeIf { it.isNotBlank() }
                            ?: current.recognizedText
                        runCatching {
                            alarmScheduler.scheduleSttAlarm(
                                triggerAtMillis = resp.fireAtEpochMillis ?: fireAt,
                                sttId = resp.sttReminderId.toString(),
                                patient = "",
                                content = content,
                                roomBedTime = "",
                            )
                        }
                        _state.update { it.copy(phase = RecordPhase.DONE) }
                    },
                    onFailure = { fail(it.message ?: "알람 등록에 실패했어요") },
                )
            } catch (t: Throwable) {
                fail(t.message ?: "알람 등록 중 오류가 발생했어요")
            }
        }
    }

    fun reset() {
        cancelRecording()
    }

    /** 결과 화면 [재녹음] — 현재 결과 폐기 후 즉시 새 녹음 시작. */
    fun restartRecording() {
        cancelRecording()
        startRecording()
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
        recordingBus.setRecording(false)
        recordingBus.setBusy(false)
        recordingBus.setAwaitingConfirm(false)
    }

    private companion object {
        const val MAX_RECORD_SEC = 60
    }
}
