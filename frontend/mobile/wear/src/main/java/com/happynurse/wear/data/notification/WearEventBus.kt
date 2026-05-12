// Service ↔ ViewModel 비차단 브리지. WearableListenerService 가 수신한 알림을
// SharedFlow 로 발행하면 ViewModel 이 collect 하여 UI 에 반영한다.
package com.happynurse.wear.data.notification

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearEventBus @Inject constructor() {

    private val _notifications = MutableSharedFlow<WearNotification>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notifications: SharedFlow<WearNotification> = _notifications.asSharedFlow()

    fun emitNotification(notification: WearNotification) {
        _notifications.tryEmit(notification)
    }
}
