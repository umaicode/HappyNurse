// 도메인 모델 — 환자/간호일지/오더/프로필 + 수액타이머/워치알람/알림함 + AI 인수인계
package com.happynurse.domain.model

// NFC 태깅으로 조회한 환자 기본 정보 — NFC 환자 화면에 표시
data class NfcPatientInfo(
    val patientId: Long,
    val encounterId: Long,
    val patientName: String,
    val roomName: String?,
    val bedName: String?,
    val birthDate: String?,
    val diseaseName: String?,
    val periodStart: String?,
    val surgeryName: String?,
    val attendingPhysicianName: String?,
)

// 간호일지 한 건 — 환자 상세의 간호일지 탭에서 사용
data class Note(
    val time: String,
    val author: String,
    val text: String,
    val tags: List<String> = emptyList(),
    val date: String = "",
    val nursingRecordId: Long = 0L,
    val type: String = "STT_NOTE",
    val status: String = "draft",
    val editable: Boolean = true,
    val taggingId: String? = null, // MEDICATION (NFC 알약/IV) 그룹 식별자. STT_NOTE 는 null.
)

// Note 의 SSE highlight 용 안정 식별자.
//  - STT_NOTE: "stt:{nursingRecordId}" (nursingRecordId > 0 인 경우만)
//  - MEDICATION (NFC 알약 / IV): "med:{taggingId}" (taggingId 비어있지 않은 경우만)
// 키 없는 노트(예: 매핑 누락) 는 highlight 대상에서 제외.
fun Note.highlightKey(): String? = when (type) {
    "STT_NOTE" -> nursingRecordId.takeIf { it > 0L }?.let { "stt:$it" }
    "MEDICATION" -> taggingId?.takeIf { it.isNotBlank() }?.let { "med:$it" }
    else -> null
}

// 의사 오더 종류 — 주사/수액/처치/검사/영상/투약
enum class OrderKind { INJ, FLUID, ORDER, LIS, IMG, PILL }

// 의사 오더 한 건 — 환자 상세의 의사오더 탭에서 사용
data class Order(
    val kind: OrderKind,
    val code: String,
    val name: String,
    val dose: String,
    val freq: String,
    val unit: String,
    val usage: String,
    val status: String,
    val note: String,
    val dateWritten: String = "",
    val timeWritten: String = "",
    val medicationOrderId: Long = 0L,
    val prescriberId: Long = 0L,
    val prescriberName: String = "",
    val route: String = "",
)

// 환자 정보 — 환자 리스트/카드/상세 화면에서 공통 사용
data class Patient(
    val id: String,
    val name: String,
    val sex: String,
    val age: Int,
    val birthdate: String,
    val mrn: String,
    val ward: String,
    val room: String,
    val bed: String,
    val admittedOn: String,
    val daysSince: Int,
    val nurse: String,
    val department: String,
    val doctor: String,
    val chief: String,
    val surgery: String,
    val patientId: Long = 0L,
    val encounterId: Long = 0L,
    val status: String = "",
    val attendingPhysicianId: Long = 0L,
    val phone: String = "",
    val unconfirmedNursingCount: Int = 0,
    val isMyPatient: Boolean = false,
    val diseaseName: String = "",
)

// 로그인한 간호사 프로필 — 마이페이지/세션에 사용
data class NurseProfile(
    val practitionerId: Long,
    val name: String,
    val employeeNumber: String,
    val roleCode: String,
    val wardId: Long,
    val wardName: String,
    val organizationId: Long,
    val organizationName: String,
)

// 진행 중인 IV(수액) 타이머 — 업무 페이지/IV 진행 화면에서 사용
data class IvTimer(
    val id: String,
    val patientId: Long = -1L,
    val patient: String,
    val room: String,
    val bed: String = "",
    val drug: String,
    val totalMin: Int,
    val elapsedMin: Int,
    val endsAt: String,
    val startedAt: String,
    val startedAtEpochMs: Long = 0L, // 라이브 카운트다운용 — UI에서 매초 elapsed 재계산
    val currentRateMlPerHr: Double? = null,
    val rateGttPerMin: Int? = null, // 서버 slim 응답의 실제 gtt/min — patientType 기반 역환산값
)

// 알림 카테고리 — 수액/오더/워치/요청/웹세션
enum class NotificationCategory { FLUID, ORDER, WATCH, REQUEST, SESSION }

// 환자요청 긴급도 — critical/high/medium/low (서버 priority 필드 매핑)
enum class NotificationPriority { CRITICAL, HIGH, MEDIUM, LOW }

// 알림함 한 건 — 상단 벨 시트/알림 목록에서 사용
data class Notification(
    val id: String,
    val category: NotificationCategory,
    val patient: String,
    val room: String,
    val text: String,
    val time: String,
    val minutesAgo: Int,
    val unread: Boolean,
    val upcoming: Boolean,
    val priority: NotificationPriority? = null,
)

// 워치 알람(STT 리마인더) — 업무 페이지 워치알람 탭, GET /reminders/stt 응답
data class WatchAlarm(
    val sttReminderId: Long,
    val contentSummary: String,
    val fireAtEpochMillis: Long?,
    val sttText: String,
)

// ────────────────────────────────────────────────────────────────────────────
// AI 인수인계 (Handover) 도메인
// ────────────────────────────────────────────────────────────────────────────

// 환자 중증도 플래그 — 안정/관찰/불안정/미상
enum class SeverityFlag { STABLE, WATCHER, UNSTABLE, UNKNOWN }

// 인수인계 슬롯의 검증 상태
enum class VerificationStatus { OK, PARTIAL, FAILED, UNKNOWN }

// 인수인계 근거(Citation) — 원본 기록 줄 단위 인용
data class Citation(
    val id: String,
    val recordId: String,
    val lineRange: List<Int>,
    val ts: String,
    val label: String,
)

// 인수인계 슬롯 내 한 항목
data class SlotItem(
    val kind: String?,
    val value: String?,
    val quote: String?,
    val citationIds: List<String>,
    val confidence: Double?,
    val sourceLayer: Int?,
    val timeWindow: String?,
    val trend: String?,
    val contingency: String?,
    val severityFlag: SeverityFlag,
)

// 인수인계 슬롯 — 항목들과 검증 결과를 묶음
data class Slot(
    val items: List<SlotItem>,
    val verification: VerificationStatus,
)

// 인수인계 SBAR + 확장 슬롯 그룹
data class Slots(
    val patientProblem: Slot,
    val assessment: Slot,
    val situation: Slot,
    val safety: Slot,
    val background: Slot,
    val action: Slot,
    val recommendation: Slot,
    val synthesis: Slot,
)

// 인수인계 룰 발화 결과
data class RuleFired(
    val ruleId: String,
    val label: String,
    val source: String,
    val severity: String,
    val matchedCitationIds: List<String>,
)

// 인수인계 페이로드 — 헤더 + 슬롯 + 인용 + 룰
data class HandoverPayload(
    val header: String,
    val illnessSeverity: SeverityFlag,
    val slots: Slots?,
    val citations: List<Citation>,
    val rulesFired: List<RuleFired>,
)

// 인수인계 단건 상세 — 화면에서 직접 사용
data class HandoverDetail(
    val handoverId: String,
    val encounterId: String,
    val autoSummary: String?,
    val payload: HandoverPayload?,
    val createdAt: String,
)

// 인수인계 목록의 환자 한 줄 — 위험도/룰 요약 포함
data class RosterPatientItem(
    val encounterId: String,
    val handoverId: String,
    val header: String,
    val riskScore: Double,
    val rulesFiredBrief: List<String>,
    val verificationSummary: Map<String, Int>,
    val newRecordsSinceReport: Int,
)

// 인수인계 로스터 요약 — 병동 환자 전체 통계 + 환자 목록
data class RosterSummary(
    val narrativeHeader: String,
    val patientCount: Int,
    val watcherCount: Int,
    val unstableCount: Int,
    val patients: List<RosterPatientItem>,
)

// 인수인계 체크리스트(서버 영속) — synthesis 슬롯 한정
data class CheckMeta(
    val by: String,
    val at: String,
)

data class HandoverChecks(
    val handoverId: String,
    val checkedSynthesisIndex: Map<Int, CheckMeta>,
)

// 인수인계 화면 상단 입원/퇴원 환자
data class WardEventEntry(
    val encounterId: String,
    val patientName: String,
    val location: String,       // "roomName · bedName"
    val primaryLabel: String,   // chiefComplaint > diseaseName > surgeryName 우선
    val timestamp: String?,     // 입원 = periodStart, 퇴원 = periodEnd
)

data class WardEvents(
    val admissions: List<WardEventEntry> = emptyList(),
    val discharges: List<WardEventEntry> = emptyList(),
) {
    val isEmpty: Boolean get() = admissions.isEmpty() && discharges.isEmpty()
}
