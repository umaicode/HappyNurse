package com.happynurse.wear.data.notification

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

// Service ↔ ViewModel 브리지. Service 는 viewModelScope 가 없어 tryEmit 으로 비차단 발행
@Singleton
class WearEventBus @Inject constructor() {

    private val _notifications = MutableSharedFlow<WearNotification>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notifications: SharedFlow<WearNotification> = _notifications.asSharedFlow()

    private val _timerStart = MutableSharedFlow<Long>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val timerStart: SharedFlow<Long> = _timerStart.asSharedFlow()

    fun emitNotification(notification: WearNotification) {
        _notifications.tryEmit(notification)
    }

    fun emitTimerStart(durationMillis: Long) {
        _timerStart.tryEmit(durationMillis)
    }
}
