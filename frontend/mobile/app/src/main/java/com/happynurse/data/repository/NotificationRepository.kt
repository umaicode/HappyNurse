// 알림함 Repository — me / ward 두 endpoint, cursor 페이지네이션 지원
package com.happynurse.data.repository

import com.happynurse.data.remote.api.NotificationApi
import com.happynurse.data.remote.model.NotificationListResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val api: NotificationApi,
) {
    // 개인 알림함 — 본인 수신분만 (recipientPractitionerId = 본인)
    suspend fun getPersonal(
        since: String? = null,
        before: Long? = null,
        limit: Int? = null,
    ): Result<NotificationListResponse> = runCatching {
        val res = api.getPersonalInbox(since, before, limit)
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data
        else throw Exception(body?.message ?: "개인 알림 조회 실패")
    }

    // 병동 알림함 — wardId 필수, 응답에 recipientPractitionerId 로 필터링 가능
    suspend fun getWard(
        wardId: Long,
        since: String? = null,
        before: Long? = null,
        limit: Int? = null,
    ): Result<NotificationListResponse> = runCatching {
        val res = api.getWardInbox(wardId, since, before, limit)
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data
        else throw Exception(body?.message ?: "병동 알림 조회 실패")
    }
}
