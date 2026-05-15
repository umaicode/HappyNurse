// 수액 목록 저장소 — /iv 응답과 /wards/me/patients 응답을 결합하여 호실/침상이 포함된 도메인 리스트를 제공한다.
package com.happynurse.wear.data.repository

import com.happynurse.wear.domain.model.IvInfusionTimer
import com.happynurse.wear.data.remote.api.IvInfusionApi
import com.happynurse.wear.data.remote.api.WardPatientApi
import com.happynurse.wear.data.remote.mapper.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IvInfusionRepository @Inject constructor(
    private val ivApi: IvInfusionApi,
    private val wardPatientApi: WardPatientApi,
) {
    suspend fun fetch(wardId: Long): Result<List<IvInfusionTimer>> = runCatching {
        android.util.Log.d("IvRepo", "fetch wardId=$wardId")
        val ivResponse = ivApi.list(wardId = wardId, status = "IN_PROGRESS")
        android.util.Log.d(
            "IvRepo",
            "response success=${ivResponse.success}, size=${ivResponse.data?.size}, msg=${ivResponse.message}",
        )
        if (!ivResponse.success) {
            error(ivResponse.message ?: "수액 목록 조회 실패")
        }
        val ivItems = ivResponse.data.orEmpty()
        ivItems.forEachIndexed { idx, item ->
            android.util.Log.d("IvRepo", "item[$idx]=$item")
        }
        val patientMap = runCatching { wardPatientApi.list() }
            .getOrNull()
            ?.data
            .orEmpty()
            .associate { it.patientId to (it.roomName.orEmpty() to it.bedName.orEmpty()) }
        android.util.Log.d("IvRepo", "patientMap size=${patientMap.size}")
        ivItems.map { it.toDomain(roomBed = patientMap[it.patientId]) }
    }
}
