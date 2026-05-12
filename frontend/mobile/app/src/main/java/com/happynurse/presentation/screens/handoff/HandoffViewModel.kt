// 인수인계 탭 ViewModel — roster-summary 초기 로드, 환자 expand 시 단건 상세 lazy fetch + 캐시.
package com.happynurse.presentation.screens.handoff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.repository.HandoverRepository
import com.happynurse.data.repository.PatientRepository
import com.happynurse.domain.model.HandoverDetail
import com.happynurse.domain.model.Patient
import com.happynurse.domain.model.RosterPatientItem
import com.happynurse.domain.model.RosterSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
)

@HiltViewModel
class HandoffViewModel @Inject constructor(
    private val repo: HandoverRepository,
    private val patientRepo: PatientRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HandoffUiState(loading = true))
    val state: StateFlow<HandoffUiState> = _state.asStateFlow()

    init { loadRoster(initial = true) }

    fun refresh() = loadRoster(initial = false)

    private fun loadRoster(initial: Boolean) {
        viewModelScope.launch {
            _state.update {
                if (initial) it.copy(loading = true, error = null)
                else it.copy(refreshing = true, error = null)
            }
            val (roster, ward) = coroutineScope {
                val r = async { repo.getRosterSummary() }
                val w = async { patientRepo.getMyWardPatients() }
                r.await() to w.await()
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
}
