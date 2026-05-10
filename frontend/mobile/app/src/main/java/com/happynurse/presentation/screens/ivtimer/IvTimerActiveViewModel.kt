// 진행 중 IV 화면 ViewModel — 캐시/재태깅 양쪽 진입 + 1초 카운트다운 + 속도 변경 / 종료
package com.happynurse.presentation.screens.ivtimer

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.nfc.NfcReaderManager
import com.happynurse.data.remote.model.IvInfusionResponse
import com.happynurse.data.repository.IvRepository
import com.happynurse.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class IvTimerActiveViewModel @Inject constructor(
    private val ivRepository: IvRepository,
    private val readerManager: NfcReaderManager,
    private val patientRepository: PatientRepository,
) : ViewModel() {

    sealed interface ActiveState {
        data object NeedsTag : ActiveState                                          // 진입 정보 없음 — NFC 재태깅 대기
        data object Loading : ActiveState                                           // by-tag 호출 중
        data class Loaded(val infusion: IvInfusionResponse, val tagUid: String?) : ActiveState
        data class Error(val message: String) : ActiveState
    }

    sealed interface ActionState {
        data object Idle : ActionState
        data object Submitting : ActionState
        data class Error(val message: String) : ActionState
        data object Completed : ActionState                                         // 종료 성공 — 화면 자동 dismiss
    }

    private val _state = MutableStateFlow<ActiveState>(ActiveState.NeedsTag)
    val state: StateFlow<ActiveState> = _state.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    private val _remainingSec = MutableStateFlow<Long?>(null)
    val remainingSec: StateFlow<Long?> = _remainingSec.asStateFlow()

    // 환자 호실/침대 — IvInfusionResponse 에 없으므로 환자 리스트에서 patientId 로 룩업
    private val _patientLocation = MutableStateFlow<Pair<String, String>?>(null)
    val patientLocation: StateFlow<Pair<String, String>?> = _patientLocation.asStateFlow()

    private var tickJob: Job? = null
    private var initialized = false
    @Volatile private var resolving = false

    /** 진입 시 1회. ivInfusionId 가 있으면 IvRepository 캐시(=직전 start 응답)에서 hit 시도. */
    fun init(ivInfusionId: Long) {
        if (initialized) return
        initialized = true
        if (ivInfusionId > 0L) {
            ivRepository.cachedById(ivInfusionId)?.let {
                applyLoaded(it, tagUid = null)
                return
            }
        }
        _state.value = ActiveState.NeedsTag
    }

    fun onTagScanned(tagUid: String) {
        if (resolving) return
        resolving = true
        viewModelScope.launch {
            _state.value = ActiveState.Loading
            ivRepository.resolveByTag(tagUid).fold(
                onSuccess = { applyLoaded(it, tagUid) },
                onFailure = { _state.value = ActiveState.Error(it.message ?: "수액 조회 실패") },
            )
            resolving = false
        }
    }

    fun submitChangeRate(rateGttPerMin: Int, patientType: PatientType) {
        val current = _state.value as? ActiveState.Loaded ?: return
        val tagUid = current.tagUid ?: return
        if (_actionState.value is ActionState.Submitting) return
        viewModelScope.launch {
            _actionState.value = ActionState.Submitting
            ivRepository.changeRate(tagUid, rateGttPerMin, patientType.raw).fold(
                onSuccess = {
                    applyLoaded(it, tagUid)
                    _actionState.value = ActionState.Idle
                },
                onFailure = { _actionState.value = ActionState.Error(it.message ?: "속도 변경 실패") },
            )
        }
    }

    fun submitComplete() {
        val current = _state.value as? ActiveState.Loaded ?: return
        val tagUid = current.tagUid ?: return
        if (_actionState.value is ActionState.Submitting) return
        viewModelScope.launch {
            _actionState.value = ActionState.Submitting
            ivRepository.complete(tagUid).fold(
                onSuccess = {
                    tickJob?.cancel()
                    _remainingSec.value = 0L
                    _state.value = ActiveState.Loaded(it, tagUid)
                    _actionState.value = ActionState.Completed
                },
                onFailure = { _actionState.value = ActionState.Error(it.message ?: "수액 종료 실패") },
            )
        }
    }

    fun consumeActionError() {
        if (_actionState.value is ActionState.Error) _actionState.value = ActionState.Idle
    }

    fun startNfc(activity: Activity) {
        readerManager.enable(activity, this) { tag ->
            val uid = tag.id.toColonHex()
            if (uid.isNotEmpty()) onTagScanned(uid)
        }
    }

    fun stopNfc(activity: Activity) {
        readerManager.disable(activity, this)
    }

    override fun onCleared() {
        tickJob?.cancel()
        super.onCleared()
    }

    private fun applyLoaded(infusion: IvInfusionResponse, tagUid: String?) {
        _state.value = ActiveState.Loaded(infusion, tagUid)
        startTicker(infusion)
        loadPatientLocation(infusion.patientId)
    }

    private fun loadPatientLocation(patientId: Long) {
        if (patientId <= 0L) return
        viewModelScope.launch {
            patientRepository.getMyWardPatients().getOrNull()
                ?.firstOrNull { it.patientId == patientId }
                ?.let { p -> _patientLocation.value = p.room to p.bed }
        }
    }

    private fun startTicker(infusion: IvInfusionResponse) {
        tickJob?.cancel()
        val end = parseInstant(infusion.expectedEndAt)
        if (end == null) {
            _remainingSec.value = infusion.remainingSeconds
            return
        }
        tickJob = viewModelScope.launch {
            while (true) {
                val remaining = (end.epochSecond - Instant.now().epochSecond).coerceAtLeast(0L)
                _remainingSec.value = remaining
                if (remaining <= 0L) break
                delay(1000)
            }
        }
    }

    private fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
            }.getOrNull()
    }

    private fun ByteArray.toColonHex(): String =
        joinToString(separator = ":") { "%02X".format(it) }
}
