// DTO → Domain 변환 확장함수 모음 — 환자/노트/오더/프로필 매핑 일원화
package com.happynurse.data.remote.mapper

import com.happynurse.data.remote.model.AppProfileResponse
import com.happynurse.data.remote.model.NursingNoteDto
import com.happynurse.data.remote.model.OrderDto
import com.happynurse.data.remote.model.PatientDetailDto
import com.happynurse.data.remote.model.SttReminderListItemResponse
import com.happynurse.data.remote.model.WardPatientDto
import com.happynurse.domain.model.Note
import com.happynurse.domain.model.NurseProfile
import com.happynurse.domain.model.Order
import com.happynurse.domain.model.OrderKind
import com.happynurse.domain.model.Patient
import com.happynurse.domain.model.WatchAlarm
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.Period
import java.time.format.DateTimeFormatter

private fun mapSex(gender: String?): String = when (gender?.lowercase()) {
    "female" -> "F"
    "male" -> "M"
    else -> "M"
}

private fun ageFromBirth(birthDate: String?): Int {
    val d = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return 0
    return Period.between(d, LocalDate.now()).years
}

private fun daysSince(periodStart: String?): Int {
    val start = runCatching { OffsetDateTime.parse(periodStart).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(periodStart?.take(10)) }.getOrNull()
        ?: return 0
    return java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now()).toInt().coerceAtLeast(0)
}

fun AppProfileResponse.toDomain(): NurseProfile = NurseProfile(
    practitionerId = practitionerId,
    name = name,
    employeeNumber = employeeNumber,
    roleCode = roleCode,
    wardId = wardId,
    wardName = wardName,
    organizationId = organizationId,
    organizationName = organizationName,
)

fun WardPatientDto.toDomain(): Patient = Patient(
    id = patientId.toString(),
    name = name,
    sex = mapSex(gender),
    age = ageFromBirth(birthDate),
    birthdate = birthDate,
    mrn = "",
    ward = "",
    room = roomName.orEmpty(),
    bed = bedName.orEmpty(),
    admittedOn = "",
    daysSince = 0,
    nurse = assignedNurseName.orEmpty(),
    department = "",
    doctor = "",
    chief = chiefComplaint.orEmpty(),
    surgery = surgeryName.orEmpty(),
    patientId = patientId,
    encounterId = encounterId,
    unconfirmedNursingCount = unconfirmedNursingCount,
    isMyPatient = isMyPatient,
    diseaseName = diseaseName.orEmpty(),
)

fun PatientDetailDto.toDomain(): Patient = Patient(
    id = patientId.toString(),
    name = name,
    sex = mapSex(gender),
    age = ageFromBirth(birthDate),
    birthdate = birthDate,
    mrn = identifierValue.orEmpty(),
    ward = wardName.orEmpty(),
    room = roomName.orEmpty(),
    bed = bedName.orEmpty(),
    admittedOn = periodStart?.take(10).orEmpty(),
    daysSince = daysSince(periodStart),
    nurse = "",
    department = departmentCode.orEmpty(),
    doctor = attendingPhysicianName.orEmpty(),
    chief = chiefComplaint.orEmpty(),
    surgery = surgeryName.orEmpty(),
    patientId = patientId,
    encounterId = encounterId,
    status = status.orEmpty(),
    attendingPhysicianId = attendingPhysicianId,
    phone = phone.orEmpty(),
    diseaseName = diseaseName.orEmpty(),
)

fun NursingNoteDto.toDomain(): Note {
    val ldt: LocalDateTime? = runCatching { OffsetDateTime.parse(occurredAt).toLocalDateTime() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(occurredAt) }.getOrNull()
    val time = ldt?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
    val date = ldt?.toLocalDate()?.toString().orEmpty()
    val tag = if (type == "MEDICATION") "투약" else "STT"
    val text = content
        ?: medications.orEmpty().joinToString { "${it.productName.orEmpty()} ${it.dosageQuantity.orEmpty()}".trim() }
    val author = authorName?.takeIf { it.isNotBlank() }?.let { "$it 간호사" }.orEmpty()
    return Note(
        time = time,
        author = author,
        text = text,
        tags = listOf(tag),
        date = date,
        nursingRecordId = nursingRecordId ?: 0L,
        type = type,
        status = status.orEmpty(),
        editable = editable,
    )
}

fun SttReminderListItemResponse.toDomain(): WatchAlarm = WatchAlarm(
    sttReminderId = sttReminderId,
    contentSummary = contentSummary.orEmpty(),
    fireAtEpochMillis = fireAtEpochMillis,
    sttText = sttText.orEmpty(),
)

fun OrderDto.toDomain(): Order {
    val kind = when (orderType) {
        "MEDICATION" -> OrderKind.INJ
        "FLUID" -> OrderKind.FLUID
        "INSTRUCTION", "TREATMENT" -> OrderKind.ORDER
        "LIS" -> OrderKind.LIS
        "IMAGE" -> OrderKind.IMG
        else -> OrderKind.ORDER
    }
    val doseStr = listOfNotNull(
        dose?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() },
        doseUnit,
    ).joinToString("").ifBlank { "-" }
    val freqStr = frequency?.let { "q${it}h" } ?: "-"
    val dateOnly = dateWritten?.take(10).orEmpty()
    val timeOnly = run {
        val ldt = runCatching { OffsetDateTime.parse(dateWritten).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(dateWritten) }.getOrNull()
        ldt?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
    }
    return Order(
        kind = kind,
        code = orderCode.orEmpty(),
        name = orderName.orEmpty(),
        dose = doseStr,
        freq = freqStr,
        unit = doseUnit.orEmpty(),
        usage = route.orEmpty(),
        status = status.orEmpty(),
        note = remarks.orEmpty(),
        dateWritten = dateOnly,
        timeWritten = timeOnly,
        medicationOrderId = medicationOrderId,
        prescriberId = prescriberId,
        prescriberName = prescriberName.orEmpty(),
        route = route.orEmpty(),
    )
}
