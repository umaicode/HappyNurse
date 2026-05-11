// AI 인수인계 DTO → Domain 매퍼.
// auto_summary_json 은 jsonb (BE 가 string 또는 object 로 내려줌) → JsonElement 단계에서 normalize.
package com.happynurse.data.remote.mapper

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.happynurse.data.remote.model.CitationDto
import com.happynurse.data.remote.model.HandoverDetailDto
import com.happynurse.data.remote.model.HandoverPayloadDto
import com.happynurse.data.remote.model.RosterPatientItemDto
import com.happynurse.data.remote.model.RosterSummaryDto
import com.happynurse.data.remote.model.RuleFiredDto
import com.happynurse.data.remote.model.SlotDto
import com.happynurse.data.remote.model.SlotItemDto
import com.happynurse.data.remote.model.SlotsDto
import com.happynurse.domain.model.Citation
import com.happynurse.domain.model.HandoverDetail
import com.happynurse.domain.model.HandoverPayload
import com.happynurse.domain.model.RosterPatientItem
import com.happynurse.domain.model.RosterSummary
import com.happynurse.domain.model.RuleFired
import com.happynurse.domain.model.SeverityFlag
import com.happynurse.domain.model.Slot
import com.happynurse.domain.model.SlotItem
import com.happynurse.domain.model.Slots
import com.happynurse.domain.model.VerificationStatus

private val gson = Gson()

private fun String?.toSeverity(): SeverityFlag = when (this?.lowercase()) {
    "stable" -> SeverityFlag.STABLE
    "watcher" -> SeverityFlag.WATCHER
    "unstable" -> SeverityFlag.UNSTABLE
    else -> SeverityFlag.UNKNOWN
}

private fun String?.toVerification(): VerificationStatus = when (this?.lowercase()) {
    "ok" -> VerificationStatus.OK
    "partial" -> VerificationStatus.PARTIAL
    "failed" -> VerificationStatus.FAILED
    else -> VerificationStatus.UNKNOWN
}

fun RosterSummaryDto.toDomain(): RosterSummary {
    val patients = patients.map { it.toDomain() }
    val patientCount = (stats["patient_count"] as? Number)?.toInt() ?: patients.size
    val watcher = patients.count { it.headerSeverity == SeverityFlag.WATCHER }
    val unstable = patients.count { it.headerSeverity == SeverityFlag.UNSTABLE }
    return RosterSummary(
        narrativeHeader = narrativeHeader.orEmpty(),
        patientCount = patientCount,
        watcherCount = watcher,
        unstableCount = unstable,
        patients = patients,
    )
}

// risk_score 기반으로 표시용 severity 를 부여 — header 문자열 자체엔 severity 가 없음.
private val RosterPatientItem.headerSeverity: SeverityFlag
    get() = when {
        riskScore >= 0.7 -> SeverityFlag.UNSTABLE
        riskScore >= 0.4 -> SeverityFlag.WATCHER
        else -> SeverityFlag.STABLE
    }

fun RosterPatientItem.severity(): SeverityFlag = headerSeverity

fun RosterPatientItemDto.toDomain(): RosterPatientItem = RosterPatientItem(
    encounterId = encounterId?.toString().orEmpty(),
    handoverId = handoverId?.toString().orEmpty(),
    header = header.orEmpty(),
    riskScore = riskScore ?: 0.0,
    rulesFiredBrief = rulesFiredBrief,
    verificationSummary = verificationSummary,
    newRecordsSinceReport = (freshness["new_records_since_report"] ?: 0),
)

fun HandoverDetailDto.toDomain(fallbackId: String): HandoverDetail = HandoverDetail(
    handoverId = handoverId?.toString() ?: fallbackId,
    encounterId = encounterId?.toString().orEmpty(),
    autoSummary = autoSummary,
    payload = normalizePayload(autoSummaryJson)?.toDomain(),
    createdAt = createdAt.orEmpty(),
)

// jsonb 는 string(JsonPrimitive) 또는 object 로 내려옴 — 두 경우 모두 처리.
private fun normalizePayload(value: JsonElement?): HandoverPayloadDto? {
    if (value == null || value.isJsonNull) return null
    return try {
        if (value.isJsonPrimitive && (value as JsonPrimitive).isString) {
            gson.fromJson(value.asString, HandoverPayloadDto::class.java)
        } else {
            gson.fromJson(value, HandoverPayloadDto::class.java)
        }
    } catch (_: Exception) {
        null
    }
}

private fun HandoverPayloadDto.toDomain(): HandoverPayload = HandoverPayload(
    header = header.orEmpty(),
    illnessSeverity = illnessSeverity.toSeverity(),
    slots = slots?.toDomain(),
    citations = citations.map { it.toDomain() },
    rulesFired = rulesFired.map { it.toDomain() },
)

private fun SlotsDto.toDomain(): Slots = Slots(
    patientProblem = patientProblem.toDomain(),
    assessment = assessment.toDomain(),
    situation = situation.toDomain(),
    safety = safety.toDomain(),
    background = background.toDomain(),
    action = action.toDomain(),
    recommendation = recommendation.toDomain(),
    synthesis = synthesis.toDomain(),
)

private fun SlotDto?.toDomain(): Slot = Slot(
    items = this?.items?.map { it.toDomain() }.orEmpty(),
    verification = this?.verification.toVerification(),
)

private fun SlotItemDto.toDomain(): SlotItem = SlotItem(
    kind = kind,
    value = value,
    quote = quote,
    citationIds = citationIds,
    confidence = confidence,
    sourceLayer = sourceLayer,
    timeWindow = timeWindow,
    trend = trend,
    contingency = contingency,
    severityFlag = severityFlag.toSeverity(),
)

private fun CitationDto.toDomain(): Citation = Citation(
    id = id.orEmpty(),
    recordId = recordId.orEmpty(),
    lineRange = lineRange,
    ts = ts.orEmpty(),
    label = label.orEmpty(),
)

private fun RuleFiredDto.toDomain(): RuleFired = RuleFired(
    ruleId = ruleId.orEmpty(),
    label = label.orEmpty(),
    source = source.orEmpty(),
    severity = severity.orEmpty(),
    matchedCitationIds = matchedCitationIds,
)
