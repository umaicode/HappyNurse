// 알람 탭 ViewModel — 본인 wardId 로 IV 보드 polling, 응답을 UI 모델 IVTimer 로 변환
package com.happynurse.presentation.screens.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.remote.model.IvInfusionListItemResponse
import com.happynurse.data.remote.model.NotificationListItemResponse
import com.happynurse.data.repository.AuthRepository
import com.happynurse.data.repository.IvRepository
import com.happynurse.data.repository.NotificationRepository
import com.happynurse.data.repository.PatientRepository
import com.happynurse.domain.model.IVTimer
import com.happynurse.domain.model.Notif
import com.happynurse.domain.model.NotifCategory
import com.happynurse.domain.model.NurseAlarm
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
class AlarmsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val ivRepository: IvRepository,
    private val notificationRepository: NotificationRepository,
    private val patientRepository: PatientRepository,
) : ViewModel() {

    private val _ivTimers = MutableStateFlow<List<IVTimer>>(emptyList())
    val ivTimers: StateFlow<List<IVTimer>> = _ivTimers.asStateFlow()

    private val _alarms = MutableStateFlow<List<NurseAlarm>>(emptyList())
    val alarms: StateFlow<List<NurseAlarm>> = _alarms.asStateFlow()

    // 벨 아이콘 시트용 — 같은 응답을 Notif 모델로 한 번 더 변환해 노출
    private val _notifs = MutableStateFlow<List<Notif>>(emptyList())
    val notifs: StateFlow<List<Notif>> = _notifs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // init refresh 제거 — 화면 진입 시마다 LaunchedEffect 가 호출 (담당환자 변경 후 stale 방지)

    // 본인 담당 환자 patientId 집합 — IV 보드 / 알림 둘 다 client-side filter 에 사용
    private suspend fun loadMyPatientIds(): Set<Long> =
        patientRepository.getMyWardPatients().getOrNull()
            ?.filter { it.isMyPatient }
            ?.map { it.patientId }
            ?.toSet()
            ?: emptySet()

    // IN_PROGRESS 만 노출 + 본인 담당 환자 의 IV 만 client-side filter.
    fun refreshIvBoard() {
        viewModelScope.launch {
            val wardId = authRepository.wardId.firstOrNull() ?: return@launch
            _loading.value = true
            val myPatientIds = loadMyPatientIds()
            ivRepository.getByWard(wardId, status = "IN_PROGRESS").fold(
                onSuccess = { list ->
                    val filtered = list.filter { it.patientId in myPatientIds }
                    _ivTimers.value = filtered.mapNotNull { it.toIvTimerOrNull() }
                    _error.value = null
                },
                onFailure = { _error.value = it.message ?: "수액 보드 조회 실패" },
            )
            _loading.value = false
        }
    }

    // 병동 알림함 — wardId 기반 + 본인 담당 환자 patientId 만 client-side filter.
    // 같은 응답을 NurseAlarm (전체알람 탭) + Notif (벨 시트) 두 모델로 변환.
    fun refreshAlarms() {
        viewModelScope.launch {
            val wardId = authRepository.wardId.firstOrNull() ?: return@launch
            val myPatientIds = loadMyPatientIds()
            notificationRepository.getWard(wardId, limit = 50).fold(
                onSuccess = { res ->
                    val mine = res.items.filter { it.patientId != null && it.patientId in myPatientIds }
                    _alarms.value = mine.map { it.toNurseAlarm() }
                    _notifs.value = mine.map { it.toNotif() }
                    _error.value = null
                },
                onFailure = { _error.value = it.message ?: "알림 조회 실패" },
            )
        }
    }
}

// 서버 알림 → 벨 시트용 Notif 변환. sourceType 으로 category 매핑.
private fun NotificationListItemResponse.toNotif(): Notif {
    val instant = parseInstantOrNull(createdAt) ?: java.time.Instant.now()
    val now = java.time.Instant.now()
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
        room = "",  // 알림 응답에 roomName 없음
        text = listOfNotNull(title, body).joinToString(" — ").ifBlank { sourceType ?: "" },
        time = time,
        minutesAgo = minutesAgo,
        unread = false,    // 백엔드 read 상태 endpoint 부재
        upcoming = false,  // 모두 발생한 이벤트
    )
}

// 서버 알림 → 화면 모델 변환. 시각은 createdAt 기준 HH:mm / yyyy-MM-dd 분리.
private fun NotificationListItemResponse.toNurseAlarm(): NurseAlarm {
    val instant = parseInstantOrNull(createdAt)
    val zoned = instant?.atZone(ZoneId.systemDefault())
    val date = zoned?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: ""
    val time = zoned?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
    val createdTime = zoned?.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) ?: ""
    return NurseAlarm(
        id = notificationId.toString(),
        patient = patientName ?: "",
        room = "",  // 알림 응답에 roomName 없음 — 화면에서 patient 만 표시
        date = date,
        time = time,
        text = listOfNotNull(title, body).joinToString(" — ").ifBlank { sourceType ?: "" },
        createdTime = createdTime,
    )
}

// 서버 slim 응답 → UI 모델 변환. startedAt/expectedEndAt 파싱 실패 시 null 반환해 리스트에서 제외.
private fun IvInfusionListItemResponse.toIvTimerOrNull(): IVTimer? {
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
    return IVTimer(
        id = ivInfusionId.toString(),
        patient = patientName ?: "환자 #$patientId",
        room = "",  // slim 응답에 roomName 미포함 — 백엔드 보강 시 노출
        drug = drug,
        totalMin = totalMin,
        elapsedMin = elapsedMin,
        endsAt = endsAt,
        startedAt = startedAtStr,
    )
}

// ISO/Offset/Local 3단 fallback 파서 — 백엔드 포맷 차이 대비
private fun parseInstantOrNull(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()
}
