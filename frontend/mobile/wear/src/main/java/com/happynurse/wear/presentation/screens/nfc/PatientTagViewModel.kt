package com.happynurse.wear.presentation.screens.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.data.nfc.NfcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PatientTagUiState {
    object Waiting : PatientTagUiState()
    data class Success(val patientName: String, val patientId: String) : PatientTagUiState()
    data class Error(val message: String) : PatientTagUiState()
}

// NFC_025, NFC_029: 환자 팔찌 NFC 리딩
@HiltViewModel
class PatientTagViewModel @Inject constructor(
    private val nfcManager: NfcManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PatientTagUiState>(PatientTagUiState.Waiting)
    val uiState: StateFlow<PatientTagUiState> = _uiState

    fun onNfcTagRead(rawData: ByteArray) {
        viewModelScope.launch {
            val result = nfcManager.readPatientTag(rawData)
            result.onSuccess { patient ->
                _uiState.value = PatientTagUiState.Success(
                    patientName = patient.name,
                    patientId = patient.id
                )
            }.onFailure {
                _uiState.value = PatientTagUiState.Error(it.message ?: "알 수 없는 오류")
            }
        }
    }
}
