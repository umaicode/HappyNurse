// 알림함 Retrofit — GET /notifications/me (개인), GET /notifications (병동)
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.NotificationListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NotificationApi {
    @GET("notifications/me")
    suspend fun getPersonalInbox(
        @Query("since") since: String? = null,
        @Query("before") before: Long? = null,
        @Query("limit") limit: Int? = null,
    ): Response<ApiResponse<NotificationListResponse>>

    @GET("notifications")
    suspend fun getWardInbox(
        @Query("wardId") wardId: Long,
        @Query("since") since: String? = null,
        @Query("before") before: Long? = null,
        @Query("limit") limit: Int? = null,
    ): Response<ApiResponse<NotificationListResponse>>
}
