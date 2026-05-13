// 일반 BE /handover/{id} GET 응답 + PATCH /handover/{id}/checks 본문 DTO
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class CheckMetaDto(
    @SerializedName("by") val by: String? = null,
    @SerializedName("at") val at: String? = null,
)

data class HandoverChecksDataDto(
    @SerializedName("handoverId") val handoverId: Long? = null,
    // key: "synthesis.{idx}"
    @SerializedName("checkedItemsJson") val checkedItemsJson: Map<String, CheckMetaDto>? = emptyMap(),
)

// PATCH /handover/{id}/checks 본문 — 서버 record HandoverChecksPatchRequest 와 일치.
// { "checks": { "synthesis.0": true, "synthesis.2": false } }
data class HandoverChecksPatchRequestDto(
    @SerializedName("checks") val checks: Map<String, Boolean>,
)
