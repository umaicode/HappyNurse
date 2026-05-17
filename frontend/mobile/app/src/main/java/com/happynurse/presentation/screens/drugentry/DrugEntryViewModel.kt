// 약물 등록 화면 ViewModel — NFC 태그 → /drug/verify 누적 → /drug/record 일괄 저장
package com.happynurse.presentation.screens.drugentry

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.nfc.NfcReaderManager
import com.happynurse.data.remote.api.EncounterApi
import com.happynurse.data.repository.DrugRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class DrugEntryViewModel @Inject constructor(
    private val drugRepository: DrugRepository,
    private val readerManager: NfcReaderManager,
    private val encounterApi: EncounterApi,
) : ViewModel() {

    sealed interface SubmitState {
        data object Idle : SubmitState
        data object Submitting : SubmitState
        data class Success(
            val savedCount: Int,
            val drugs: List<VerifiedDrug>,
            val patientName: String?,
            val savedAt: LocalDateTime,
        ) : SubmitState
        data class Error(val message: String) : SubmitState
    }

    data class VerifiedDrug(
        val tagUid: String,
        val medicationOrderId: Long,
        val medicationName: String,
        val orderCode: String?,
    )

    private val _verifiedDrugs = MutableStateFlow<List<VerifiedDrug>>(emptyList())
    val verifiedDrugs: StateFlow<List<VerifiedDrug>> = _verifiedDrugs.asStateFlow()

    private val _verifyError = MutableStateFlow<String?>(null)
    val verifyError: StateFlow<String?> = _verifyError.asStateFlow()

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    private val _patientName = MutableStateFlow<String?>(null)
    val patientName: StateFlow<String?> = _patientName.asStateFlow()

    private var patientId: Long = -1L
    private var encounterId: Long = -1L
    @Volatile private var verifying = false
    // medicationOrderId → (orderName, orderCode) 매핑 캐시. /drug/verify 응답엔 ID 만 와서 별도 fetch.
    private var orderNames: Map<Long, String> = emptyMap()
    private var orderCodes: Map<Long, String?> = emptyMap()

    fun setContext(patientId: Long, encounterId: Long) {
        this.patientId = patientId
        this.encounterId = encounterId
        if (encounterId > 0L) loadOrders()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            runCatching {
                val res = encounterApi.getOrders(encounterId)
                val body = res.body()
                if (res.isSuccessful && body?.success == true && body.data != null) {
                    orderNames = body.data.orders.associate {
                        it.medicationOrderId to (it.orderName ?: "처방 #${it.medicationOrderId}")
                    }
                    orderCodes = body.data.orders.associate {
                        it.medicationOrderId to it.orderCode
                    }
                    _patientName.value = body.data.patientName
                }
            }
        }
    }

    fun onTagScanned(tagUid: String) {
        if (patientId <= 0L) {
            _verifyError.value = "환자 정보가 없습니다"
            return
        }
        if (_verifiedDrugs.value.any { it.tagUid == tagUid }) return  // 중복 태그 무시
        if (verifying) return
        verifying = true
        viewModelScope.launch {
            drugRepository.verify(patientId, tagUid).fold(
                onSuccess = { res ->
                    if (res.verified) {
                        val name = orderNames[res.medicationOrderId]
                            ?: "처방 #${res.medicationOrderId}"
                        _verifiedDrugs.update {
                            it + VerifiedDrug(tagUid, res.medicationOrderId, name, orderCodes[res.medicationOrderId])
                        }
                        _verifyError.value = null
                    } else {
                        _verifyError.value = "검증 실패 — 처방에 없는 약물입니다"
                    }
                },
                onFailure = { _verifyError.value = it.message ?: "검증 실패" },
            )
            verifying = false
        }
    }

    fun consumeVerifyError() { _verifyError.value = null }

    fun removeDrug(tagUid: String) {
        _verifiedDrugs.update { list -> list.filterNot { it.tagUid == tagUid } }
    }

    fun submit() {
        val drugs = _verifiedDrugs.value
        val ids = drugs.map { it.medicationOrderId }
        if (ids.isEmpty() || patientId <= 0L || encounterId <= 0L) return
        if (_submitState.value is SubmitState.Submitting) return
        viewModelScope.launch {
            _submitState.value = SubmitState.Submitting
            drugRepository.saveAdministrations(patientId, encounterId, ids).fold(
                onSuccess = { res ->
                    _submitState.value = SubmitState.Success(
                        savedCount = res.savedCount,
                        drugs = drugs,
                        patientName = _patientName.value,
                        savedAt = LocalDateTime.now(),
                    )
                    _verifiedDrugs.value = emptyList()
                },
                onFailure = {
                    _submitState.value = SubmitState.Error(it.message ?: "저장 실패")
                },
            )
        }
    }

    fun consumeSubmitState() { _submitState.value = SubmitState.Idle }

    fun startNfc(activity: Activity) {
        readerManager.enable(activity, this) { tag ->
            val uid = tag.id.toColonHex()
            if (uid.isNotEmpty()) onTagScanned(uid)
        }
    }

    fun stopNfc(activity: Activity) {
        readerManager.disable(activity, this)
    }

    private fun ByteArray.toColonHex(): String =
        joinToString(separator = ":") { "%02X".format(it) }
}
