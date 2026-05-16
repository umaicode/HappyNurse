// 간호일지 STT ViewModel — 녹음 → 업로드 → 결과 표시 state machine
package com.happynurse.presentation.screens.logentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.audio.AudioRecorder
import com.happynurse.data.remote.model.SttRecognizeResponse
import com.happynurse.data.repository.SttRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LogEntryViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val sttRepository: SttRepository,
) : ViewModel() {

    sealed interface LogState {
        data object Idle : LogState                                            // 녹음 시작 대기
        data class Recording(val seconds: Int) : LogState                      // 녹음 중 (1초 tick)
        data object Uploading : LogState                                       // STT 서버 변환 중
        data class Result(val response: SttRecognizeResponse) : LogState       // STT 결과 표시 (draft 상태 — 데스크톱 차트에서 수정/확정)
        data class Error(val message: String) : LogState
    }

    private val _state = MutableStateFlow<LogState>(LogState.Idle)
    val state: StateFlow<LogState> = _state.asStateFlow()

    // 마이크 입력 진폭 시간축 히스토리 (각 0f..1f, 길이 LEVELS_SIZE) —
    // 80ms 간격 폴링값을 앞에 push, 끝값 pop. UI 이퀄라이저는 인덱스별 막대로 표시 → 좌→우로 흐르는 파형.
    private val _levels = MutableStateFlow(List(LEVELS_SIZE) { 0f })
    val levels: StateFlow<List<Float>> = _levels.asStateFlow()

    private var patientId: Long = -1L
    private var encounterId: Long = -1L
    private var tickJob: Job? = null
    private var amplitudeJob: Job? = null
    private var recordedFile: File? = null

    fun setContext(patientId: Long, encounterId: Long) {
        this.patientId = patientId
        this.encounterId = encounterId
    }

    // 권한 (RECORD_AUDIO) 은 호출자(Screen) 가 사전 보장. 실패 시 catch 로 Error 전이.
    fun startRecording() {
        if (_state.value !is LogState.Idle && _state.value !is LogState.Result && _state.value !is LogState.Error) return
        try {
            recordedFile = audioRecorder.start()
            _state.value = LogState.Recording(0)
            tickJob?.cancel()
            tickJob = viewModelScope.launch {
                var sec = 0
                while (isActive) {
                    delay(1000)
                    sec++
                    val current = _state.value
                    if (current is LogState.Recording) _state.value = LogState.Recording(sec) else break
                }
            }
            amplitudeJob?.cancel()
            amplitudeJob = viewModelScope.launch {
                while (isActive && _state.value is LogState.Recording) {
                    val raw = audioRecorder.maxAmplitude()
                    val newLevel = (raw / 8000f).coerceIn(0f, 1f)
                    _levels.value = (listOf(newLevel) + _levels.value).take(LEVELS_SIZE)
                    delay(80)
                }
                _levels.value = List(LEVELS_SIZE) { 0f }
            }
        } catch (e: Exception) {
            _state.value = LogState.Error(e.message ?: "녹음 시작 실패")
        }
    }

    // 녹음 정지 → 즉시 업로드. 짧은 녹음 (<1초) 도 일단 시도하고 서버 결과로 판단.
    fun stopAndUpload() {
        if (_state.value !is LogState.Recording) return
        tickJob?.cancel()
        amplitudeJob?.cancel()
        _levels.value = List(LEVELS_SIZE) { 0f }
        val file = audioRecorder.stop()
        if (file == null || !file.exists() || file.length() == 0L) {
            _state.value = LogState.Error("녹음 파일이 비어있습니다")
            return
        }
        _state.value = LogState.Uploading
        viewModelScope.launch {
            sttRepository.recognize(
                audioFile = file,
                patientId = patientId.takeIf { it > 0 },
                encounterId = encounterId.takeIf { it > 0 },
            ).fold(
                onSuccess = { _state.value = LogState.Result(it) },
                onFailure = { _state.value = LogState.Error(it.message ?: "STT 변환 실패") },
            )
            // cache 정리는 다음 녹음 시 또는 ViewModel cleared 시
        }
    }

    // 다시 녹음 — 결과/에러 카드에서 마이크 버튼으로 돌아갈 때
    fun reset() {
        tickJob?.cancel()
        amplitudeJob?.cancel()
        _levels.value = List(LEVELS_SIZE) { 0f }
        audioRecorder.stop()
        recordedFile?.delete()
        recordedFile = null
        _state.value = LogState.Idle
    }

    // confirm 단계는 데스크톱 차트에서 처리. 모바일은 STT 즉시 저장 (status='draft') 으로 끝.

    override fun onCleared() {
        tickJob?.cancel()
        amplitudeJob?.cancel()
        audioRecorder.stop()
        recordedFile?.delete()
        super.onCleared()
    }

    companion object {
        const val LEVELS_SIZE = 13
    }
}
