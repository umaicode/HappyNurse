// 도메인 모델 — 환자/간호일지/오더/프로필 + 알람/수액/알림/인계 (알람 이하는 SampleData)
package com.happynurse.domain.model

data class NfcPatientInfo(
    val patientId: Long,
    val encounterId: Long,
    val patientName: String,
    val roomName: String?,
    val diseaseName: String?,
    val chiefComplaint: String?,
    val surgeryName: String?,
    val attendingPhysicianName: String?,
)

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
)

enum class OrderKind { INJ, FLUID, ORDER, LIS, IMG, PILL }

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

// ⚠️ SampleData — 백엔드 API 미구현 (SampleData.nurseAlarms)
data class NurseAlarm(
    val id: String,
    val patient: String,
    val room: String,
    val date: String,
    val time: String,
    val text: String,
    val createdTime: String,
)

// ⚠️ SampleData — 백엔드 API 미구현 (SampleData.ivTimers)
data class IVTimer(
    val id: String,
    val patient: String,
    val room: String,
    val drug: String,
    val totalMin: Int,
    val elapsedMin: Int,
    val endsAt: String,
    val startedAt: String,
)

// ⚠️ SampleData — 백엔드 API 미구현 (SampleData.notifications)
enum class NotifCategory { FLUID, WATCH, REQUEST }

// ⚠️ SampleData — 백엔드 API 미구현 (SampleData.notifications)
data class Notif(
    val id: String,
    val category: NotifCategory,
    val patient: String,
    val room: String,
    val text: String,
    val time: String,
    val minutesAgo: Int,
    val unread: Boolean,
    val upcoming: Boolean,
)

// ⚠️ SampleData — 백엔드 API 미구현 (SampleData.handoffs)
data class HandoffItem(
    val id: String,
    val patient: String,
    val room: String,
    val tag: String,
    val note: String,
    val aiSummary: List<String>,
    val warnings: String,
    val checklist: List<HandoffCheck>,
)

// ⚠️ SampleData — 백엔드 API 미구현 (SampleData.handoffs)
data class HandoffCheck(val text: String, val done: Boolean)
