// 의사오더 DTO — GET /encounters/{encounterId}/orders 응답
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class OrderListResponse(
    @SerializedName("encounterId") val encounterId: Long,
    @SerializedName("patientId") val patientId: Long,
    @SerializedName("patientName") val patientName: String?,
    @SerializedName("totalCount") val totalCount: Int = 0,
    @SerializedName("orders") val orders: List<OrderDto> = emptyList(),
)

data class OrderDto(
    @SerializedName("medicationOrderId") val medicationOrderId: Long,
    @SerializedName("orderType") val orderType: String,
    @SerializedName("orderCode") val orderCode: String?,
    @SerializedName("orderName") val orderName: String?,
    @SerializedName("dose") val dose: Double? = null,
    @SerializedName("frequency") val frequency: Int? = null,
    @SerializedName("doseUnit") val doseUnit: String?,
    @SerializedName("route") val route: String?,
    @SerializedName("remarks") val remarks: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("dateWritten") val dateWritten: String?,
    @SerializedName("prescriberId") val prescriberId: Long = 0L,
    @SerializedName("prescriberName") val prescriberName: String?,
)
