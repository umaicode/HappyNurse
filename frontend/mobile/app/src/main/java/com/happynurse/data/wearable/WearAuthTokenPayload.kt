// 폰 → 워치 토큰 응답 페이로드. 워치 모듈의 동명 클래스와 직렬화 호환되어야 한다.
package com.happynurse.data.wearable

import kotlinx.serialization.Serializable

@Serializable
data class WearAuthTokenPayload(
    val accessToken: String,
    val wardId: Long,
)
