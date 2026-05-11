// AI 인수인계 응답 DTO — FastAPI (ai 서버), ApiResponse wrapper 없음.
// 응답 스키마 출처: ai/nursing_ai/app/services/handover/schemas.py — 웹 FE handover.ts 와 동일.
package com.happynurse.data.remote.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ---------- Roster Summary (진입 요약) ----------

data class RosterPatientItemDto(
    @SerializedName("encounter_id") val encounterId: Long?,
    @SerializedName("handover_id") val handoverId: Long?,
    @SerializedName("header") val header: String?,
    @SerializedName("risk_score") val riskScore: Double?,
    @SerializedName("rules_fired_brief") val rulesFiredBrief: List<String> = emptyList(),
    @SerializedName("verification_summary") val verificationSummary: Map<String, Int> = emptyMap(),
    @SerializedName("freshness") val freshness: Map<String, Int> = emptyMap(),
)

data class RosterSummaryDto(
    @SerializedName("schema_version") val schemaVersion: String?,
    @SerializedName("kind") val kind: String?,
    @SerializedName("narrative_header") val narrativeHeader: String?,
    @SerializedName("stats") val stats: Map<String, Any?> = emptyMap(),
    @SerializedName("patients") val patients: List<RosterPatientItemDto> = emptyList(),
    @SerializedName("verification_followup") val verificationFollowup: List<Map<String, Any?>> = emptyList(),
    @SerializedName("meta") val meta: Map<String, Any?> = emptyMap(),
)

// ---------- HandoverPayload (auto_summary_json) ----------

data class CitationDto(
    @SerializedName("id") val id: String?,
    @SerializedName("record_id") val recordId: String?,
    @SerializedName("line_range") val lineRange: List<Int> = emptyList(),
    @SerializedName("ts") val ts: String?,
    @SerializedName("label") val label: String?,
)

data class SlotItemDto(
    @SerializedName("kind") val kind: String?,
    @SerializedName("value") val value: String?,
    @SerializedName("quote") val quote: String?,
    @SerializedName("citation_ids") val citationIds: List<String> = emptyList(),
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("source_layer") val sourceLayer: Int?,
    @SerializedName("time_window") val timeWindow: String?,
    @SerializedName("trend") val trend: String?,
    @SerializedName("contingency") val contingency: String?,
    @SerializedName("severity_flag") val severityFlag: String?, // stable | watcher | unstable
)

data class SlotDto(
    @SerializedName("items") val items: List<SlotItemDto> = emptyList(),
    @SerializedName("verification") val verification: String?, // ok | partial | failed
)

data class SlotsDto(
    @SerializedName("patient_problem") val patientProblem: SlotDto?,
    @SerializedName("assessment") val assessment: SlotDto?,
    @SerializedName("situation") val situation: SlotDto?,
    @SerializedName("safety") val safety: SlotDto?,
    @SerializedName("background") val background: SlotDto?,
    @SerializedName("action") val action: SlotDto?,
    @SerializedName("recommendation") val recommendation: SlotDto?,
    @SerializedName("synthesis") val synthesis: SlotDto?,
)

data class RuleFiredDto(
    @SerializedName("rule_id") val ruleId: String?,
    @SerializedName("label") val label: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("severity") val severity: String?, // low | medium | high
    @SerializedName("matched_citation_ids") val matchedCitationIds: List<String> = emptyList(),
)

data class HandoverPayloadDto(
    @SerializedName("schema_version") val schemaVersion: String?,
    @SerializedName("header") val header: String?,
    @SerializedName("illness_severity") val illnessSeverity: String?,
    @SerializedName("slots") val slots: SlotsDto?,
    @SerializedName("citations") val citations: List<CitationDto> = emptyList(),
    @SerializedName("rules_fired") val rulesFired: List<RuleFiredDto> = emptyList(),
    @SerializedName("meta") val meta: Map<String, Any?> = emptyMap(),
)

// ---------- 단건 조회 응답 ----------
// auto_summary_json 은 jsonb — BE 가 string 또는 object 로 내려줄 수 있어 JsonElement 로 받고 mapper 에서 normalize.

data class HandoverDetailDto(
    @SerializedName("handover_id") val handoverId: Any?, // string 또는 number 가능
    @SerializedName("encounter_id") val encounterId: Any?,
    @SerializedName("auto_summary") val autoSummary: String?,
    @SerializedName("auto_summary_json") val autoSummaryJson: JsonElement?,
    @SerializedName("created_at") val createdAt: String?,
)

// ---------- generate ----------

data class HandoverJobDto(
    @SerializedName("job_id") val jobId: String?,
)
