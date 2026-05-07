// SttResultViewModel — s12 "확인" 시 STT 타이머를 폰에 전송 (폰이 백엔드에 등록 위임).
// 응답은 비동기 — 워치는 즉시 s20a 로 진입.
package com.happynurse.wear.presentation.screens.stt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.data.notification.WearSttCreatePayload
import com.happynurse.wear.data.remote.WearDataClient
import com.happynurse.wear.data.remote.WearableMessagePaths
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SttResultViewModel @Inject constructor(
    private val wearDataClient: WearDataClient,
) : ViewModel() {

    fun submitToPhone(payload: WearSttCreatePayload) {
        viewModelScope.launch {
            runCatching {
                val bytes = Json.encodeToString(payload).toByteArray(Charsets.UTF_8)
                wearDataClient.send(WearableMessagePaths.WEAR_STT_TIMER_CREATE, bytes)
                Log.d(TAG, "STT 등록 페이로드 폰 전달 성공")
            }.onFailure { Log.w(TAG, "STT 등록 전달 실패", it) }
        }
    }

    companion object {
        private const val TAG = "SttResultVM"
    }
}
