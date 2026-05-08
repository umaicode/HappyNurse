// 수액 시작 폼 ViewModel — POST /iv/start, 결과 IvInfusionResponse 보존
package com.happynurse.presentation.screens.ivtimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.remote.api.EncounterApi
import com.happynurse.data.remote.model.IvInfusionResponse
import com.happynurse.data.repository.IvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PatientType(val raw: String, val label: String, val gttPerMl: Int) {
    ADULT("ADULT", "성인", 20),
    PEDIATRIC("PEDIATRIC", "소아", 60),
}

/** 처방 1건의 표시용 정보. 카드 표시 + 처방 용량 합계 검증에 사용. */
data class OrderInfo(
    val orderName: String,
    /** doseUnit == "mL" 이고 dose 가 있을 때만 채워짐. 없으면 max 검증에서 제외. */
    val doseMl: Int?,
)

@HiltViewModel
class IvTimerSetupViewModel @Inject constructor(
    private val ivRepository: IvRepository,
    private val encounterApi: EncounterApi,
) : ViewModel() {

    sealed interface SetupState {
        data object Idle : SetupState
        data object Submitting : SetupState
        data class Success(val infusion: IvInfusionResponse) : SetupState
        data class Error(val message: String) : SetupState
    }

    private val _state = MutableStateFlow<SetupState>(SetupState.Idle)
    val state: StateFlow<SetupState> = _state.asStateFlow()

    private val _patientName = MutableStateFlow<String?>(null)
    val patientName: StateFlow<String?> = _patientName.asStateFlow()

    private val _orders = MutableStateFlow<Map<Long, OrderInfo>>(emptyMap())
    val orders: StateFlow<Map<Long, OrderInfo>> = _orders.asStateFlow()

    private var loadedEncounterId: Long = -1L

    fun loadContext(encounterId: Long) {
        if (encounterId <= 0L || encounterId == loadedEncounterId) return
        loadedEncounterId = encounterId
        viewModelScope.launch {
            runCatching {
                val res = encounterApi.getOrders(encounterId)
                val body = res.body()
                if (res.isSuccessful && body?.success == true && body.data != null) {
                    _patientName.value = body.data.patientName
                    _orders.value = body.data.orders.associate { o ->
                        val mlDose =
                            if ("mL".equals(o.doseUnit, ignoreCase = true)) o.dose?.toInt()
                            else null
                        o.medicationOrderId to OrderInfo(
                            orderName = o.orderName ?: "처방 #${o.medicationOrderId}",
                            doseMl = mlDose,
                        )
                    }
                }
            }
        }
    }

    fun start(
        encounterId: Long,
        medicationOrderIds: List<Long>,
        totalVolumeMl: Double,
        rateGttPerMin: Int,
        patientType: PatientType,
        note: String? = null,
    ) {
        if (encounterId <= 0L || medicationOrderIds.isEmpty()) return
        if (_state.value is SetupState.Submitting) return
        viewModelScope.launch {
            _state.value = SetupState.Submitting
            ivRepository.start(
                encounterId = encounterId,
                medicationOrderIds = medicationOrderIds,
                totalVolumeMl = totalVolumeMl,
                rateGttPerMin = rateGttPerMin,
                patientType = patientType.raw,
                note = note,
            ).fold(
                onSuccess = { _state.value = SetupState.Success(it) },
                onFailure = { _state.value = SetupState.Error(it.message ?: "수액 시작 실패") },
            )
        }
    }

    fun resetError() {
        if (_state.value is SetupState.Error) _state.value = SetupState.Idle
    }
}
