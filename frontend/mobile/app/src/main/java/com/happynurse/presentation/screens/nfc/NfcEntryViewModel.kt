package com.happynurse.presentation.screens.nfc

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.remote.api.NfcTokenApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

sealed class NfcEntryUiState {
    object Loading : NfcEntryUiState()
    data class Success(val patientId: Long) : NfcEntryUiState()
    data class InvalidToken(val message: String) : NfcEntryUiState()
    data class NetworkError(val message: String) : NfcEntryUiState()
    data class UnknownError(val message: String) : NfcEntryUiState()
}

@HiltViewModel
class NfcEntryViewModel @Inject constructor(
    private val nfcTokenApi: NfcTokenApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NfcEntryUiState>(NfcEntryUiState.Loading)
    val uiState: StateFlow<NfcEntryUiState> = _uiState

    // TODO 로그인 wiring 후: AuthRepository.isLoggedIn() 체크 추가
    //  - 로그인 안됨 → NavGraph 가 LoginScreen 으로 redirect (token 을 navArg 로 전달)
    //  - 로그인됨 → 아래 resolve 로직 실행
    fun resolve(token: String) {
        viewModelScope.launch {
            _uiState.value = try {
                val response = nfcTokenApi.resolveByToken(token)
                NfcEntryUiState.Success(response.patientId)
            } catch (exception: HttpException) {
                val body = runCatching { exception.response()?.errorBody()?.string() }.getOrNull()
                Log.w(TAG, "백엔드 응답 오류: code=${exception.code()} body=$body", exception)
                NfcEntryUiState.InvalidToken("HTTP ${exception.code()} — ${body ?: exception.message()}")
            } catch (exception: IOException) {
                Log.w(TAG, "네트워크 오류", exception)
                NfcEntryUiState.NetworkError("서버 연결 실패: ${exception.message}")
            } catch (exception: Exception) {
                Log.w(TAG, "알 수 없는 오류", exception)
                NfcEntryUiState.UnknownError(exception.message ?: "알 수 없는 오류")
            }
        }
    }

    companion object {
        private const val TAG = "NfcEntry"
    }
}
