// WardEvents DTO → Domain 매퍼
package com.happynurse.data.remote.mapper

import com.happynurse.data.remote.model.WardEventEntryDto
import com.happynurse.data.remote.model.WardEventsDataDto
import com.happynurse.domain.model.WardEventEntry
import com.happynurse.domain.model.WardEvents

fun WardEventEntryDto.toAdmissionDomain(): WardEventEntry = toEntry(timestamp = periodStart)
fun WardEventEntryDto.toDischargeDomain(): WardEventEntry = toEntry(timestamp = periodEnd)

private fun WardEventEntryDto.toEntry(timestamp: String?): WardEventEntry {
    val location = listOfNotNull(
        roomName?.takeIf { it.isNotBlank() },
        bedName?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("번")) it else "${it}번" },
    ).joinToString(" · ")
    val primary = listOfNotNull(
        chiefComplaint?.takeIf { it.isNotBlank() },
        diseaseName?.takeIf { it.isNotBlank() },
        surgeryName?.takeIf { it.isNotBlank() },
    ).firstOrNull().orEmpty()
    return WardEventEntry(
        encounterId = encounterId?.toString().orEmpty(),
        patientName = patientName?.takeIf { it.isNotBlank() } ?: "이름 없음",
        location = location,
        primaryLabel = primary,
        timestamp = timestamp?.takeIf { it.isNotBlank() },
    )
}

fun WardEventsDataDto.toDomain(): WardEvents = WardEvents(
    admissions = admissions.map { it.toAdmissionDomain() },
    discharges = discharges.map { it.toDischargeDomain() },
)
