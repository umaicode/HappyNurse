// 폰에서 워치로 전달되는 인증 페이로드. 워치는 이 정보를 WearTokenStore 에 캐시하여
// 자체 Retrofit 호출 시 Authorization 헤더와 wardId 쿼리에 사용한다.
package com.happynurse.wear.data.notification

import kotlinx.serialization.Serializable

@Serializable
data class WearAuthTokenPayload(
    val accessToken: String,
    val wardId: Long,
)
