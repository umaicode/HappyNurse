// 와이어프레임 data.jsx 와 동등한 도메인 모델 — 환자/바이탈/오더/일지/알람/수액/인계
package com.happynurse.domain.model

data class Vitals(
    val bp: String,
    val hr: Int,
    val rr: Int,
    val temp: String,
    val spo2: Int,
)

data class Note(
    val time: String,
    val author: String,
    val text: String,
    val tags: List<String> = emptyList(),
    val date: String = "2026-04-30",
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
    val dateWritten: String = "2026-04-30",
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
    val memo: String,
    val vitals: Vitals,
    val notes: List<Note>,
    val orders: List<Order>,
    val patientId: Long = 0L,
    val encounterId: Long = 0L,
    val status: String = "",
    val attendingPhysicianId: Long = 0L,
    val phone: String = "",
    val address: String = "",
    val unconfirmedNursingCount: Int = 0,
    val isMyPatient: Boolean = false,
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

data class NurseAlarm(
    val id: String,
    val patient: String,
    val room: String,
    val date: String,
    val time: String,
    val text: String,
    val createdTime: String,
)

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

enum class NotifCategory { FLUID, WATCH, REQUEST }

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

data class HandoffCheck(val text: String, val done: Boolean)
