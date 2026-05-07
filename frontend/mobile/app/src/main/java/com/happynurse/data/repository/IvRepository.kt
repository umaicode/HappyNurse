// 수액 IV Repository — PR 3 start, PR 4 by-tag (resolve / complete / changeRate). 캐시는 setup→active 전환용.
package com.happynurse.data.repository

import com.happynurse.data.remote.api.IvApi
import com.happynurse.data.remote.model.ChangeRateRequest
import com.happynurse.data.remote.model.IvInfusionResponse
import com.happynurse.data.remote.model.StartIvRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IvRepository @Inject constructor(
    private val api: IvApi,
) {
    // setup → active 1.5초 전환 시 같은 응답을 다시 받을 endpoint 가 없어 메모리 캐시로 보존.
    // active 화면이 본인 ivInfusionId 와 일치하면 사용, 아니면 NFC 재태깅 요청.
    @Volatile private var lastInfusion: IvInfusionResponse? = null

    suspend fun start(
        encounterId: Long,
        medicationOrderIds: List<Long>,
        totalVolumeMl: Double,
        rateGttPerMin: Int,
        patientType: String,
        note: String? = null,
    ): Result<IvInfusionResponse> = runCatching {
        val res = api.start(
            StartIvRequest(
                encounterId = encounterId,
                medicationOrderIds = medicationOrderIds,
                totalVolumeMl = totalVolumeMl,
                rateGttPerMin = rateGttPerMin,
                patientType = patientType,
                note = note,
            ),
        )
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data.also { lastInfusion = it }
        else throw Exception(body?.message ?: "수액 시작 실패")
    }

    suspend fun resolveByTag(tagUid: String): Result<IvInfusionResponse> = runCatching {
        val res = api.getByTag(tagUid)
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data.also { lastInfusion = it }
        else throw Exception(body?.message ?: "수액 조회 실패")
    }

    suspend fun complete(tagUid: String): Result<IvInfusionResponse> = runCatching {
        val res = api.completeByTag(tagUid)
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data.also { lastInfusion = it }
        else throw Exception(body?.message ?: "수액 종료 실패")
    }

    suspend fun changeRate(
        tagUid: String,
        rateGttPerMin: Int,
        patientType: String,
    ): Result<IvInfusionResponse> = runCatching {
        val res = api.changeRateByTag(tagUid, ChangeRateRequest(rateGttPerMin, patientType))
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) body.data.also { lastInfusion = it }
        else throw Exception(body?.message ?: "속도 변경 실패")
    }

    fun cachedById(ivInfusionId: Long): IvInfusionResponse? =
        lastInfusion?.takeIf { it.ivInfusionId == ivInfusionId }
}
