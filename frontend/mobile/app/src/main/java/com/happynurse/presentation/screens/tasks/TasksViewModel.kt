// 업무 페이지 ViewModel — 수액타이머 / 의사오더변경(placeholder) / 워치알람 3탭 상태 관리
package com.happynurse.presentation.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.remote.mapper.toDomain
import com.happynurse.data.remote.model.IvInfusionListItemResponse
import com.happynurse.data.remote.model.NotificationListItemResponse
import com.happynurse.data.repository.AuthRepository
import com.happynurse.data.repository.IvRepository
import com.happynurse.data.repository.NotificationRepository
import com.happynurse.data.repository.PatientRepository
import com.happynurse.data.repository.SttReminderRepository
import com.happynurse.domain.model.DoctorOrderChange
import com.happynurse.domain.model.IVTimer
import com.happynurse.domain.model.Notif
import com.happynurse.domain.model.NotifCategory
import com.happynurse.domain.model.WatchAlarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ivRepository: IvRepository,
    private val notificationRepository: NotificationRepository,
    private val patientRepository: PatientRepository,
    private val sttReminderRepository: SttReminderRepository,
) : ViewModel() {

    private val _ivTimers = MutableStateFlow<List<IVTimer>>(emptyList())
    val ivTimers: StateFlow<List<IVTimer>> = _ivTimers.asStateFlow()

    // 의사오더변경 — 백엔드 변경 API 추가 전까지 항상 빈 리스트 (placeholder)
    private val _orderChanges = MutableStateFlow<List<DoctorOrderChange>>(emptyList())
    val orderChanges: StateFlow<List<DoctorOrderChange>> = _orderChanges.asStateFlow()

    private val _watchAlarms = MutableStateFlow<List<WatchAlarm>>(emptyList())
    val watchAlarms: StateFlow<List<WatchAlarm>> = _watchAlarms.asStateFlow()

    // 상단 알림 벨 시트(MainScaffold) 호환용 — 기존 AlarmsViewModel.notifs 와 동일 역할
    private val _notifs = MutableStateFlow<List<Notif>>(emptyList())
    val notifs: StateFlow<List<Notif>> = _notifs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refreshIvBoard() {
        viewModelScope.launch {
            val wardId = authRepository.wardId.firstOrNull() ?: return@launch
            _loading.value = true
            val wardPatients = patientRepository.getMyWardPatients().getOrNull() ?: emptyList()
            val myPatientIds = wardPatients.filter { it.isMyPatient }.map { it.patientId }.toSet()
            val locationMap = wardPatients.associate { it.patientId to (it.room to it.bed) }
            ivRepository.getByWard(wardId, status = "IN_PROGRESS").fold(
                onSuccess = { list ->
                    val filtered = list.filter { it.patientId in myPatientIds }
                    _ivTimers.value = filtered.mapNotNull { it.toIvTimerOrNull(locationMap) }
                    _error.value = null
                },
                onFailure = { _error.value = it.message ?: "수액 보드 조회 실패" },
            )
            _loading.value = false
        }
    }

    fun refreshWatchAlarms() {
        viewModelScope.launch {
            sttReminderRepository.listMine().fold(
                onSuccess = { list ->
                    _watchAlarms.value = list
                        .map { it.toDomain() }
                        .sortedBy { it.fireAtEpochMillis ?: Long.MAX_VALUE }
                    _error.value = null
                },
                onFailure = { _error.value = it.message ?: "워치 알람 조회 실패" },
            )
        }
    }

    // 벨 시트 데이터 — 병동 알림함을 본인 담당 환자만 client-side filter
    fun refreshBellNotifs() {
        viewModelScope.launch {
            val wardId = authRepository.wardId.firstOrNull() ?: return@launch
            val myPatientIds = loadMyPatientIds()
            notificationRepository.getWard(wardId, limit = 50).fold(
                onSuccess = { res ->
                    val mine = res.items.filter { it.patientId != null && it.patientId in myPatientIds }
                    _notifs.value = mine.map { it.toNotif() }
                    _error.value = null
                },
                onFailure = { _error.value = it.message ?: "알림 조회 실패" },
            )
        }
    }

    private suspend fun loadMyPatientIds(): Set<Long> =
        patientRepository.getMyWardPatients().getOrNull()
            ?.filter { it.isMyPatient }
            ?.map { it.patientId }
            ?.toSet()
            ?: emptySet()
}

private fun NotificationListItemResponse.toNotif(): Notif {
    val instant = parseInstantOrNull(createdAt) ?: Instant.now()
    val now = Instant.now()
    val minutesAgo = ((now.epochSecond - instant.epochSecond) / 60).toInt().coerceAtLeast(0)
    val zoned = instant.atZone(ZoneId.systemDefault())
    val time = zoned.format(DateTimeFormatter.ofPattern("HH:mm"))
    val cat = when (sourceType) {
        "iv_alert" -> NotifCategory.FLUID
        "self_report", "order_change" -> NotifCategory.REQUEST
        "vital_alert", "timer" -> NotifCategory.WATCH
        else -> NotifCategory.REQUEST
    }
    return Notif(
        id = notificationId.toString(),
        category = cat,
        patient = patientName ?: "",
        room = "",
        text = listOfNotNull(title, body).joinToString(" — ").ifBlank { sourceType ?: "" },
        time = time,
        minutesAgo = minutesAgo,
        unread = false,
        upcoming = false,
    )
}

private fun IvInfusionListItemResponse.toIvTimerOrNull(
    locationMap: Map<Long, Pair<String, String>> = emptyMap(),
): IVTimer? {
    val started = parseInstantOrNull(startedAt) ?: return null
    val expected = parseInstantOrNull(expectedEndAt) ?: return null
    val totalSec = Duration.between(started, expected).seconds.coerceAtLeast(0L)
    val now = Instant.now()
    val elapsedSec = Duration.between(started, now).seconds.coerceIn(0L, totalSec)
    val totalMin = (totalSec / 60).toInt()
    val elapsedMin = (elapsedSec / 60).toInt()
    val endsAt = expected.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    val startedAtStr = started.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    val drug = if (medicationNames.isNotEmpty()) medicationNames.joinToString(" + ") else "—"
    val (room, bed) = locationMap[patientId] ?: ("" to "")
    return IVTimer(
        id = ivInfusionId.toString(),
        patientId = patientId,
        patient = patientName ?: "환자 #$patientId",
        room = room,
        bed = bed,
        drug = drug,
        totalMin = totalMin,
        elapsedMin = elapsedMin,
        endsAt = endsAt,
        startedAt = startedAtStr,
        currentRateMlPerHr = currentRateMlPerHr,
    )
}

private fun parseInstantOrNull(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()
}
