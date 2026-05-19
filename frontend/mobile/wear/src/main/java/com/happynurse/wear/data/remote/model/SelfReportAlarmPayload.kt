// 폰 → 워치 환자요청 풀스크린 알람 페이로드. 폰 모듈의 동명 클래스와 직렬화 호환되어야 한다.
package com.happynurse.wear.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class SelfReportAlarmPayload(
    val patientName: String,
    val roomLocation: String,
    val body: String,
    val priority: String,
)
