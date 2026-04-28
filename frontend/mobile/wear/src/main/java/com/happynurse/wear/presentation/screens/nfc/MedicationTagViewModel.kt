// 약물 NFC 태그 ViewModel(Hilt) — NfcManager.readMedicationTag로 약물 정보 복호화 후 5 Rights 검증
package com.happynurse.wear.presentation.screens.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.data.nfc.NfcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MedicationTagUiState {
    object Waiting : MedicationTagUiState()
    data class Matched(val medicationName: String) : MedicationTagUiState()
    object Mismatched : MedicationTagUiState()
    data class Error(val message: String) : MedicationTagUiState()
}

// NFC_026, NFC_032: 약물 NFC 리딩 + 5 Rights 검증
@HiltViewModel
class MedicationTagViewModel @Inject constructor(
    private val nfcManager: NfcManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<MedicationTagUiState>(MedicationTagUiState.Waiting)
    val uiState: StateFlow<MedicationTagUiState> = _uiState

    fun onNfcTagRead(rawData: ByteArray, currentPatientId: String) {
        viewModelScope.launch {
            val result = nfcManager.readMedicationTag(rawData)
            result.onSuccess { medication ->
                val isMatched = nfcManager.verifyMedication(medication, currentPatientId)
                _uiState.value = if (isMatched) {
                    MedicationTagUiState.Matched(medication.name)
                } else {
                    MedicationTagUiState.Mismatched
                }
            }.onFailure {
                _uiState.value = MedicationTagUiState.Error(it.message ?: "알 수 없는 오류")
            }
        }
    }
}
