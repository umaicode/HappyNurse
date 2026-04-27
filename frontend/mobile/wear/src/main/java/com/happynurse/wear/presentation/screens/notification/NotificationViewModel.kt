// 알림 ViewModel(Hilt) — WearNotification/NotificationType(IV_ALERT, TIMER_ALARM) 상태 관리
package com.happynurse.wear.presentation.screens.notification

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class WearNotification(
    val title: String,
    val patientName: String,
    val roomLocation: String,
    val type: NotificationType
)

enum class NotificationType {
    IV_ALERT,   // 알림_043: 수액 알림
    TIMER_ALARM // 알림_044: 타이머 알람
}

@HiltViewModel
class NotificationViewModel @Inject constructor() : ViewModel() {

    private val _currentNotification = MutableStateFlow<WearNotification?>(null)
    val currentNotification: StateFlow<WearNotification?> = _currentNotification

    fun showNotification(notification: WearNotification) {
        _currentNotification.value = notification
    }
}
