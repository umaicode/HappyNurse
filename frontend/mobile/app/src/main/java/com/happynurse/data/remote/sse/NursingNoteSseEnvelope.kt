// nursing_record / medication_admin SSE 이벤트 envelope 의 JSON 직렬화 형태.
// 백엔드 NotificationEnvelope (api/NotificationEnvelope.java) 와 동일 구조 — 모바일에서 필요한 필드만 골라 둠.
// 화면 갱신 트리거 용도라 envelope.occurredAt 만 있어도 충분하지만, patientId 가드 등 후속 가능성을 위해 같이 받는다.
package com.happynurse.data.remote.sse

import com.google.gson.annotations.SerializedName

data class NursingNoteSseEnvelope(
    @SerializedName("sourceType") val sourceType: String? = null,
    @SerializedName("wardId") val wardId: Long? = null,
    @SerializedName("patientId") val patientId: Long? = null,
    @SerializedName("sourceEntityId") val sourceEntityId: Long? = null,
    // ISO instant string (BE Instant 직렬화). LocalDate 변환은 호출자에서.
    @SerializedName("occurredAt") val occurredAt: String? = null,
)
