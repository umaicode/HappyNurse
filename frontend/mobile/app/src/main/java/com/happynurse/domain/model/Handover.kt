// AI 인수인계 도메인 모델 — DTO 와 분리해 화면 레이어에서 사용.
package com.happynurse.domain.model

enum class SeverityFlag { STABLE, WATCHER, UNSTABLE, UNKNOWN }
enum class VerificationStatus { OK, PARTIAL, FAILED, UNKNOWN }

data class Citation(
    val id: String,
    val recordId: String,
    val lineRange: List<Int>,
    val ts: String,
    val label: String,
)

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

data class Slot(
    val items: List<SlotItem>,
    val verification: VerificationStatus,
)

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

data class RuleFired(
    val ruleId: String,
    val label: String,
    val source: String,
    val severity: String,
    val matchedCitationIds: List<String>,
)

data class HandoverPayload(
    val header: String,
    val illnessSeverity: SeverityFlag,
    val slots: Slots?,
    val citations: List<Citation>,
    val rulesFired: List<RuleFired>,
)

data class HandoverDetail(
    val handoverId: String,
    val encounterId: String,
    val autoSummary: String?,
    val payload: HandoverPayload?,
    val createdAt: String,
)

data class RosterPatientItem(
    val encounterId: String,
    val handoverId: String,
    val header: String,
    val riskScore: Double,
    val rulesFiredBrief: List<String>,
    val verificationSummary: Map<String, Int>,
    val newRecordsSinceReport: Int,
)

data class RosterSummary(
    val narrativeHeader: String,
    val patientCount: Int,
    val watcherCount: Int,
    val unstableCount: Int,
    val patients: List<RosterPatientItem>,
)
