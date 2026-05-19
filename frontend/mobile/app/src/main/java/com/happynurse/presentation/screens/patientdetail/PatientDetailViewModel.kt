// 환자 상세 ViewModel — 환자 단건/간호일지(날짜별)/의사오더 + 담당 환자 드롭다운 목록
package com.happynurse.presentation.screens.patientdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.happynurse.data.remote.sse.NotificationStream
import com.happynurse.data.remote.sse.NursingNoteSseEnvelope
import com.happynurse.data.repository.EncounterRepository
import com.happynurse.data.repository.PatientRepository
import com.happynurse.domain.model.Note
import com.happynurse.domain.model.Order
import com.happynurse.domain.model.highlightKey
import com.happynurse.domain.model.Patient
import com.happynurse.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val encounterRepository: EncounterRepository,
    private val notificationStream: NotificationStream,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel() {

    private val initialId: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: 0L

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _myPatients = MutableStateFlow<List<Patient>>(emptyList())
    val myPatients: StateFlow<List<Patient>> = _myPatients.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    private val _monthCounts = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val monthCounts: StateFlow<Map<LocalDate, Int>> = _monthCounts.asStateFlow()

    // SSE 도착으로 신규 추가된 간호일지 행의 식별자 모음 — 화면에서 2.5초간 카드 border 강조 표시 후 자동 해제.
    // 사용자 액션(날짜/환자 변경, 첫 진입) 의 loadNotes 는 영향 없음 — reloadNotesAndHighlightNew 가 호출될 때만 동작.
    private val _recentlyAddedKeys = MutableStateFlow<Set<String>>(emptySet())
    val recentlyAddedKeys: StateFlow<Set<String>> = _recentlyAddedKeys.asStateFlow()

    init {
        if (initialId > 0L) loadPatient(initialId)
        loadMyPatients()
        // SSE 구독 시작 (이미 시작됐으면 중복 가드로 무시됨) — 알림 화면과 동일한 NotificationStream singleton.
        notificationStream.start(viewModelScope)
        // ward 채널의 nursing_record/medication_admin 이벤트만 처리 → 현재 보고 있는 날짜면 화면 즉시 갱신.
        viewModelScope.launch {
            notificationStream.events.collect { ev ->
                if (ev.sourceType != "nursing_record" && ev.sourceType != "medication_admin") return@collect
                handleNursingNoteEvent(ev.data)
            }
        }
    }

    // envelope.occurredAt → LocalDate 추출 후 현재 선택 날짜와 비교. 같으면 reload+highlight, 같은 월이면 loadMonthNotes.
    private fun handleNursingNoteEvent(json: String) {
        val eid = _patient.value?.encounterId ?: return
        if (eid <= 0L) return
        val envelope = runCatching { Gson().fromJson(json, NursingNoteSseEnvelope::class.java) }.getOrNull() ?: return
        val occurredLocalDate = parseOccurredDate(envelope.occurredAt) ?: return
        val selected = _selectedDate.value
        if (occurredLocalDate == selected) {
            reloadNotesAndHighlightNew(eid, selected)
        }
        val occurredMonth = YearMonth.from(occurredLocalDate)
        if (occurredMonth == _visibleMonth.value) {
            loadMonthNotes(eid, occurredMonth)
        }
    }

    // SSE 트리거 reload — 이전 노트 목록과 비교해 신규 식별자를 _recentlyAddedKeys 에 2.5초 동안 등록.
    // 사용자 액션 reload (loadNotes) 와 분리되어 있어 날짜/환자 변경 시 강조가 발사되지 않는다.
    private fun reloadNotesAndHighlightNew(encounterId: Long, date: LocalDate) {
        val previousKeys = _notes.value.mapNotNull { it.highlightKey() }.toSet()
        launchWithResult(block = { encounterRepository.getNursingNotes(encounterId, date.toString()) }) { list ->
            val sorted = list.sortedBy { it.time }
            _notes.value = sorted
            val newKeys = sorted.mapNotNull { it.highlightKey() }.filter { it !in previousKeys }.toSet()
            if (newKeys.isNotEmpty()) {
                _recentlyAddedKeys.update { it + newKeys }
                viewModelScope.launch {
                    delay(HIGHLIGHT_DURATION_MS)
                    _recentlyAddedKeys.update { it - newKeys }
                }
            }
        }
    }

    private companion object {
        const val HIGHLIGHT_DURATION_MS = 2500L
    }

    private fun parseOccurredDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        // BE 는 Instant (ISO_INSTANT) 로 직렬화하는 게 기본. 혹시 OffsetDateTime/LocalDateTime 도 들어올 수 있어 fallback.
        return runCatching { Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(raw).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(raw).toLocalDate() }.getOrNull()
    }

    fun loadPatient(patientId: Long) {
        launchWithResult(block = { patientRepository.getPatient(patientId) }) { p ->
            _patient.value = p
            if (p.encounterId > 0L) {
                loadNotes(p.encounterId, _selectedDate.value)
                loadOrders(p.encounterId)
                loadMonthNotes(p.encounterId, _visibleMonth.value)
            }
        }
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
        val eid = _patient.value?.encounterId ?: return
        if (eid > 0L) loadNotes(eid, date)
        val ym = YearMonth.from(date)
        if (ym != _visibleMonth.value) setMonth(ym)
    }

    fun setMonth(ym: YearMonth) {
        _visibleMonth.value = ym
        val eid = _patient.value?.encounterId ?: return
        if (eid > 0L) loadMonthNotes(eid, ym)
    }

    private fun loadMonthNotes(encounterId: Long, ym: YearMonth) {
        viewModelScope.launch {
            val days = (1..ym.lengthOfMonth()).map { ym.atDay(it) }
            val results = days.map { d ->
                async {
                    val r = encounterRepository.getNursingNotes(encounterId, d.toString())
                    d to (r.getOrNull()?.size ?: 0)
                }
            }.awaitAll()
            _monthCounts.value = results.toMap().filterValues { it > 0 }
        }
    }

    private fun loadNotes(encounterId: Long, date: LocalDate) {
        launchWithResult(block = { encounterRepository.getNursingNotes(encounterId, date.toString()) }) {
            _notes.value = it.sortedBy { n -> n.time }
        }
    }

    private fun loadOrders(encounterId: Long) {
        launchWithResult(block = { encounterRepository.getOrders(encounterId) }) { _orders.value = it }
    }

    private fun loadMyPatients() {
        launchWithResult(block = { patientRepository.getMyWardPatients() }) { list ->
            // 본인 담당 환자 우선, 없으면 같은 병동 환자 전체 표시
            val mine = list.filter { it.isMyPatient }
            _myPatients.value = if (mine.isNotEmpty()) mine else list
        }
    }
}
