package com.happynurse.wear.data.sensor

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

// 음성STT_020/021: 엄지+검지 두 번 맞대기 제스처 감지
// 워치 가속도계·자이로스코프 활용
@Singleton
class GestureDetector @Inject constructor() {

    enum class GestureEvent {
        PINCH_DOUBLE // 엄지+검지 두 번 맞대기
    }

    private val _gestureEvents = MutableSharedFlow<GestureEvent>()
    val gestureEvents: SharedFlow<GestureEvent> = _gestureEvents

    // TODO: SensorManager로 가속도계/자이로스코프 데이터 수집 및 제스처 분류 모델 적용
    suspend fun emitGesture(event: GestureEvent) {
        _gestureEvents.emit(event)
    }
}
