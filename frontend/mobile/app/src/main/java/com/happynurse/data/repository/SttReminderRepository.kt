// STT 음성 메모 알람 Repository — 워치알람(업무 페이지) 탭에서 사용
package com.happynurse.data.repository

import com.happynurse.data.remote.api.SttReminderApi
import com.happynurse.data.remote.model.SttReminderListItemResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SttReminderRepository @Inject constructor(
    private val api: SttReminderApi,
) {
    suspend fun listMine(): Result<List<SttReminderListItemResponse>> = runCatching {
        val res = api.listMine()
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data
        else throw Exception(body?.message ?: "STT 알람 조회 실패")
    }
}
