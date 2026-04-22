package com.happynurse.presentation.screens.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NfcWriteUiState {
    object Idle : NfcWriteUiState()
    object Writing : NfcWriteUiState()
    object Success : NfcWriteUiState()
    data class Error(val message: String) : NfcWriteUiState()
}

// NFC_028, NFC_030: 환자 팔찌 NFC 라이팅 + AES-256 암호화
// NFC_031, NFC_033: 약물 NFC 라이팅 + AES-256 암호화
@HiltViewModel
class NfcWriteViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<NfcWriteUiState>(NfcWriteUiState.Idle)
    val uiState: StateFlow<NfcWriteUiState> = _uiState

    fun writePatientTag(patientId: String, patientName: String, roomLocation: String) {
        viewModelScope.launch {
            _uiState.value = NfcWriteUiState.Writing
            // TODO: AES-256 암호화 후 NFC 라이팅
            _uiState.value = NfcWriteUiState.Success
        }
    }

    fun writeMedicationTag(medicationId: String, medicationName: String, dosage: String) {
        viewModelScope.launch {
            _uiState.value = NfcWriteUiState.Writing
            // TODO: AES-256 암호화 후 NFC 라이팅
            _uiState.value = NfcWriteUiState.Success
        }
    }
}
