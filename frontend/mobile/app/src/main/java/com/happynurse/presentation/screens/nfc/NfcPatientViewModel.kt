// NFC 환자 화면 ViewModel — wristband 태깅 → 환자 정보 로드 + reader-mode 라이프사이클 관리
package com.happynurse.presentation.screens.nfc

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.nfc.NfcReaderManager
import com.happynurse.data.repository.NfcPatientRepository
import com.happynurse.domain.model.NfcPatientInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NfcPatientViewModel @Inject constructor(
    private val repository: NfcPatientRepository,
    private val readerManager: NfcReaderManager,
) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class Success(val info: NfcPatientInfo) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun onTokenScanned(token: String) {
        if (_state.value is State.Loading) return  // 중복 태깅 방지
        viewModelScope.launch {
            _state.value = State.Loading
            repository.resolveByToken(token).fold(
                onSuccess = { _state.value = State.Success(it) },
                onFailure = { _state.value = State.Error(it.message ?: "알 수 없는 오류") },
            )
        }
    }

    fun startNfc(activity: Activity) {
        readerManager.enable(activity, this) { tag ->
            readerManager.parsePatientToken(tag)?.let(::onTokenScanned)
        }
    }

    fun stopNfc(activity: Activity) {
        readerManager.disable(activity, this)
    }

    fun reset() {
        _state.value = State.Idle
    }
}
