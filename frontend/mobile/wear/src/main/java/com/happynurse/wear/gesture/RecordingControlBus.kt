// 녹음 상태/제어 신호 버스 — RecordViewModel(녹음 화면) 과 GestureService(서비스) 사이 단방향 통신.
// - isRecording: 실제 녹음 진행 중. 제스처 → stop 신호 분기 기준.
// - isBusy: 녹음/처리/등록 중 (RECORDING/PROCESSING/RESULT/SUBMITTING). 제스처로 새 녹음 launch 차단 기준.
// - awaitingConfirm: RESULT phase. 싱글 스냅 → confirm 트리거 분기 기준.
// - stopRequests / confirmRequests: 외부(서비스) → ViewModel 로의 액션 요청 신호.
package com.happynurse.wear.gesture

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingControlBus @Inject constructor() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _awaitingConfirm = MutableStateFlow(false)
    val awaitingConfirm: StateFlow<Boolean> = _awaitingConfirm.asStateFlow()

    private val _stopRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val stopRequests: SharedFlow<Unit> = _stopRequests.asSharedFlow()

    private val _confirmRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val confirmRequests: SharedFlow<Unit> = _confirmRequests.asSharedFlow()

    fun setRecording(recording: Boolean) {
        _isRecording.value = recording
    }

    fun setBusy(busy: Boolean) {
        _isBusy.value = busy
    }

    fun setAwaitingConfirm(awaiting: Boolean) {
        _awaitingConfirm.value = awaiting
    }

    fun requestStop() {
        _stopRequests.tryEmit(Unit)
    }

    fun requestConfirm() {
        _confirmRequests.tryEmit(Unit)
    }
}
