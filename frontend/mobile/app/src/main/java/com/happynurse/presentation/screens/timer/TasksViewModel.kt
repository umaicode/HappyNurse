// 업무 페이지 ViewModel — 수액타이머 / 워치알람 2탭 상태 + 알림 벨 데이터 관리
package com.happynurse.presentation.screens.timer

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
import com.happynurse.domain.model.IvTimer
import com.happynurse.domain.model.Notification
import com.happynurse.domain.model.NotificationCategory
import com.happynurse.domain.model.NotificationPriority
import com.happynurse.domain.model.WatchAlarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
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

    private val _ivTimers = MutableStateFlow<List<IvTimer>>(emptyList())
    val ivTimers: StateFlow<List<IvTimer>> = _ivTimers.asStateFlow()

    private val _watchAlarms = MutableStateFlow<List<WatchAlarm>>(emptyList())
    val watchAlarms: StateFlow<List<WatchAlarm>> = _watchAlarms.asStateFlow()

    // 상단 알림 벨 시트(MainScaffold) 데이터 — 병동 알림함 + 본인 워치 알람을 합친 목록
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // 벨 배지 실시간 갱신 — ViewModel 살아있는 동안 주기적으로 알림함 폴링.
        // 백엔드에 push 채널이 phone 쪽으로 안 와있어서 폴링이 가장 단순하고 견고함.
        viewModelScope.launch {
            while (isActive) {
                refreshBellNotifications()
                delay(BELL_POLL_INTERVAL_MS)
            }
        }
        // 워치 알람 목록 실시간 갱신 — 워치가 백엔드에 직접 등록하므로 폴링으로 동기화.
        viewModelScope.launch {
            while (isActive) {
                refreshWatchAlarms()
                delay(WATCH_ALARM_POLL_INTERVAL_MS)
            }
        }
    }

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
                    val now = System.currentTimeMillis()
                    _watchAlarms.value = list
                        .map { it.toDomain() }
                        .filter { (it.fireAtEpochMillis ?: 0L) > now }
                        .sortedBy { it.fireAtEpochMillis ?: Long.MAX_VALUE }
                    _error.value = null
                },
                onFailure = { _error.value = it.message ?: "워치 알람 조회 실패" },
            )
        }
    }

    // 벨 시트 데이터 — 병동 알림함 + 본인 워치 알람을 합쳐 본인 담당 환자만 client-side filter
    fun refreshBellNotifications() {
        viewModelScope.launch {
            val wardId = authRepository.wardId.firstOrNull() ?: return@launch
            val wardPatients = patientRepository.getMyWardPatients().getOrNull() ?: emptyList()
            val myPatientIds = wardPatients.filter { it.isMyPatient }.map { it.patientId }.toSet()
            val locationMap = wardPatients.associate { it.patientId to (it.room to it.bed) }

            val serverNotifications = notificationRepository.getWard(wardId, limit = 50).fold(
                onSuccess = { res ->
                    res.items
                        .filter { it.patientId != null && it.patientId in myPatientIds }
                        .map { it.toNotification(locationMap) }
                },
                onFailure = { e ->
                    _error.value = e.message ?: "알림 조회 실패"
                    emptyList()
                },
            )

            val watchNotifications = sttReminderRepository.listMine().fold(
                onSuccess = { list ->
                    val now = System.currentTimeMillis()
                    list.map { it.toDomain() }
                        .filter { (it.fireAtEpochMillis ?: 0L) > now }
                        .map { it.toBellNotification(now) }
                },
                onFailure = { emptyList() },
            )

            _notifications.value = (serverNotifications + watchNotifications)
                .sortedWith(
                    // 과거 알림(minutesAgo >= 0) 먼저, 그 안에서 최신순. 그 뒤 미래 알림은 가까운 순.
                    compareBy<Notification>(
                        { if (it.minutesAgo >= 0) 0 else 1 },
                        { kotlin.math.abs(it.minutesAgo) },
                    ),
                )
        }
    }

    private companion object {
        const val BELL_POLL_INTERVAL_MS = 15_000L   // 15초마다 벨 카운트 갱신
        const val WATCH_ALARM_POLL_INTERVAL_MS = 30_000L  // 30초마다 워치 알람 동기화
    }
}

private fun WatchAlarm.toBellNotification(nowMillis: Long): Notification {
    val fireAt = fireAtEpochMillis ?: nowMillis
    val minutesUntil = ((fireAt - nowMillis) / 60_000L).toInt()
    val time = Instant.ofEpochMilli(fireAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    return Notification(
        id = "watch-$sttReminderId",
        category = NotificationCategory.WATCH,
        patient = "",
        room = "",
        text = contentSummary.ifBlank { sttText.ifBlank { "(내용 없음)" } },
        time = time,
        minutesAgo = -minutesUntil,
        unread = false,
        upcoming = false,
    )
}

private fun NotificationListItemResponse.toNotification(
    locationMap: Map<Long, Pair<String, String>> = emptyMap(),
): Notification {
    val instant = parseInstantOrNull(createdAt) ?: Instant.now()
    val now = Instant.now()
    val minutesAgo = ((now.epochSecond - instant.epochSecond) / 60).toInt().coerceAtLeast(0)
    val zoned = instant.atZone(ZoneId.systemDefault())
    val time = zoned.format(DateTimeFormatter.ofPattern("HH:mm"))
    val cat = when (sourceType) {
        "iv_alert" -> NotificationCategory.FLUID
        "order_change" -> NotificationCategory.ORDER
        "self_report" -> NotificationCategory.REQUEST
        "vital_alert", "timer" -> NotificationCategory.WATCH
        "web_login", "web_logout" -> NotificationCategory.SESSION
        else -> NotificationCategory.REQUEST
    }
    val (room, bed) = patientId?.let { locationMap[it] } ?: ("" to "")
    val roomLabel = listOf(room, bed).filter { it.isNotBlank() }.joinToString("-")
    val pri = when (priority?.lowercase()) {
        "critical" -> NotificationPriority.CRITICAL
        "high"     -> NotificationPriority.HIGH
        "medium"   -> NotificationPriority.MEDIUM
        "low"      -> NotificationPriority.LOW
        else       -> null
    }
    return Notification(
        id = notificationId.toString(),
        category = cat,
        patient = patientName ?: "",
        room = roomLabel,
        text = body?.takeIf { it.isNotBlank() } ?: title.orEmpty(),
        time = time,
        minutesAgo = minutesAgo,
        unread = false,
        upcoming = false,
        priority = pri,
    )
}

private fun IvInfusionListItemResponse.toIvTimerOrNull(
    locationMap: Map<Long, Pair<String, String>> = emptyMap(),
): IvTimer? {
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
    return IvTimer(
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
        startedAtEpochMs = started.toEpochMilli(),
        currentRateMlPerHr = currentRateMlPerHr,
        rateGttPerMin = rateGttPerMin,
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
