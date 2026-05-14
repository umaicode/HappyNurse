// 인수인계 탭 ViewModel — roster-summary + ward-events 초기 동시 로드,
// 환자 카드 진입 시 단건 상세 + 체크리스트 lazy fetch + 캐시.
// 체크리스트 토글은 낙관적 업데이트 + PATCH delta, 실패 시 롤백.
package com.happynurse.presentation.screens.handoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.repository.AuthRepository
import com.happynurse.data.repository.HandoverRepository
import com.happynurse.data.repository.PatientRepository
import com.happynurse.domain.model.CheckMeta
import com.happynurse.domain.model.HandoverDetail
import com.happynurse.domain.model.Patient
import com.happynurse.domain.model.RosterPatientItem
import com.happynurse.domain.model.RosterSummary
import com.happynurse.domain.model.WardEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HandoffUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val rosterSummary: RosterSummary? = null,
    val patients: List<RosterPatientItem> = emptyList(),
    val patientByEncounterId: Map<String, Patient> = emptyMap(),
    val detailByHandoverId: Map<String, HandoverDetail> = emptyMap(),
    val loadingDetailIds: Set<String> = emptySet(),
    val expandedHandoverId: String? = null,
    val error: String? = null,

    // 입퇴원 환자 strip
    val wardEvents: WardEvents? = null,
    val wardEventsLoading: Boolean = false,
    val wardEventsError: String? = null,

    // 체크리스트 영속 — handoverId → (synthesis index → CheckMeta)
    // 해당 키가 없으면 = 아직 GET 안됨. 빈 Map = 로드 완료(전부 미체크).
    val checksByHandoverId: Map<String, Map<Int, CheckMeta>> = emptyMap(),
    val checksLoadingIds: Set<String> = emptySet(),
    // 진행 중 토글
    val checksInFlight: Map<String, Set<Int>> = emptyMap(),
    val checksError: Map<String, String> = emptyMap(),
)

@HiltViewModel
class HandoffViewModel @Inject constructor(
    private val repo: HandoverRepository,
    private val patientRepo: PatientRepository,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HandoffUiState(loading = true))
    val state: StateFlow<HandoffUiState> = _state.asStateFlow()

    init { loadRoster(initial = true) }

    fun refresh() = loadRoster(initial = false)

    private fun loadRoster(initial: Boolean) {
        viewModelScope.launch {
            _state.update {
                if (initial) it.copy(loading = true, error = null, wardEventsLoading = true, wardEventsError = null)
                else it.copy(refreshing = true, error = null, wardEventsLoading = true, wardEventsError = null)
            }
            val (roster, ward, wardEvents) = coroutineScope {
                val r = async { repo.getRosterSummary() }
                val w = async { patientRepo.getMyWardPatients() }
                val e = async { repo.getWardEvents() }
                Triple(r.await(), w.await(), e.await())
            }

            roster
                .onSuccess { summary ->
                    val map = ward.getOrNull().orEmpty().associateBy { it.encounterId.toString() }
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            rosterSummary = summary,
                            patients = summary.patients,
                            patientByEncounterId = map,
                            error = null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = e.message ?: "불러오기 실패",
                        )
                    }
                }

            wardEvents
                .onSuccess { events ->
                    _state.update {
                        it.copy(
                            wardEvents = events,
                            wardEventsLoading = false,
                            wardEventsError = null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            wardEventsLoading = false,
                            wardEventsError = e.message ?: "입퇴원 환자 조회 실패",
                        )
                    }
                }
        }
    }

    fun toggleExpand(handoverId: String) {
        if (handoverId.isBlank()) return
        val cur = _state.value
        if (cur.expandedHandoverId == handoverId) {
            _state.update { it.copy(expandedHandoverId = null) }
            return
        }
        _state.update { it.copy(expandedHandoverId = handoverId) }
        if (cur.detailByHandoverId.containsKey(handoverId)) return
        viewModelScope.launch {
            _state.update { it.copy(loadingDetailIds = it.loadingDetailIds + handoverId) }
            repo.getHandoverDetail(handoverId)
                .onSuccess { detail ->
                    _state.update {
                        it.copy(
                            detailByHandoverId = it.detailByHandoverId + (handoverId to detail),
                            loadingDetailIds = it.loadingDetailIds - handoverId,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loadingDetailIds = it.loadingDetailIds - handoverId,
                            error = e.message,
                        )
                    }
                }
        }
    }

    fun collapse() {
        _state.update { it.copy(expandedHandoverId = null) }
    }

    // 페이저로 환자 카드 진입 시 자동 호출 — 이미 캐시되어 있으면 noop.
    fun ensureDetailLoaded(handoverId: String) {
        if (handoverId.isBlank()) return
        val cur = _state.value
        if (cur.detailByHandoverId.containsKey(handoverId)) return
        if (cur.loadingDetailIds.contains(handoverId)) return
        viewModelScope.launch {
            _state.update { it.copy(loadingDetailIds = it.loadingDetailIds + handoverId) }
            repo.getHandoverDetail(handoverId)
                .onSuccess { detail ->
                    _state.update {
                        it.copy(
                            detailByHandoverId = it.detailByHandoverId + (handoverId to detail),
                            loadingDetailIds = it.loadingDetailIds - handoverId,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loadingDetailIds = it.loadingDetailIds - handoverId,
                            error = e.message,
                        )
                    }
                }
        }
    }

    // 환자 카드 진입 시 체크리스트 GET — 이미 로드/진행 중이면 noop.
    fun ensureChecksLoaded(handoverId: String) {
        if (handoverId.isBlank()) return
        val cur = _state.value
        if (cur.checksByHandoverId.containsKey(handoverId)) return
        if (cur.checksLoadingIds.contains(handoverId)) return
        viewModelScope.launch {
            _state.update { it.copy(checksLoadingIds = it.checksLoadingIds + handoverId) }
            repo.getHandoverChecks(handoverId)
                .onSuccess { checks ->
                    _state.update {
                        it.copy(
                            checksByHandoverId = it.checksByHandoverId + (handoverId to checks.checkedSynthesisIndex),
                            checksLoadingIds = it.checksLoadingIds - handoverId,
                            checksError = it.checksError - handoverId,
                        )
                    }
                }
                .onFailure { e ->
                    // 실패해도 빈 map으로 가정해 UI 동작 — 에러 메시지는 기록
                    _state.update {
                        it.copy(
                            checksByHandoverId = it.checksByHandoverId + (handoverId to emptyMap()),
                            checksLoadingIds = it.checksLoadingIds - handoverId,
                            checksError = it.checksError + (handoverId to (e.message ?: "체크리스트 조회 실패")),
                        )
                    }
                }
        }
    }

    // synthesis index 체크 토글 — 낙관적 업데이트 + PATCH(delta), 실패 시 롤백.
    fun toggleSynthesisCheck(handoverId: String, index: Int, newValue: Boolean) {
        if (handoverId.isBlank()) return
        val cur = _state.value
        val inFlightSet = cur.checksInFlight[handoverId].orEmpty()
        if (index in inFlightSet) return  // 연타 방지

        val previousMap = cur.checksByHandoverId[handoverId].orEmpty()
        val previousMeta = previousMap[index]

        // 낙관적 업데이트
        val nowIso = runCatching {
            OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }.getOrElse { "" }
        viewModelScope.launch {
            val myName = authRepo.displayName.firstOrNull().orEmpty()

            val optimisticMap = if (newValue) {
                previousMap + (index to CheckMeta(by = myName, at = nowIso))
            } else {
                previousMap - index
            }
            _state.update {
                it.copy(
                    checksByHandoverId = it.checksByHandoverId + (handoverId to optimisticMap),
                    checksInFlight = it.checksInFlight + (handoverId to (inFlightSet + index)),
                )
            }

            repo.patchHandoverChecks(handoverId, mapOf(index to newValue))
                .onSuccess {
                    _state.update {
                        val flight = (it.checksInFlight[handoverId].orEmpty() - index)
                        it.copy(
                            checksInFlight = if (flight.isEmpty()) it.checksInFlight - handoverId
                            else it.checksInFlight + (handoverId to flight),
                            checksError = it.checksError - handoverId,
                        )
                    }
                }
                .onFailure { e ->
                    // 롤백
                    _state.update {
                        val rolled = if (previousMeta != null) previousMap + (index to previousMeta)
                        else previousMap - index
                        val flight = (it.checksInFlight[handoverId].orEmpty() - index)
                        it.copy(
                            checksByHandoverId = it.checksByHandoverId + (handoverId to rolled),
                            checksInFlight = if (flight.isEmpty()) it.checksInFlight - handoverId
                            else it.checksInFlight + (handoverId to flight),
                            checksError = it.checksError + (handoverId to (e.message ?: "체크 저장 실패")),
                        )
                    }
                }
        }
    }
}
