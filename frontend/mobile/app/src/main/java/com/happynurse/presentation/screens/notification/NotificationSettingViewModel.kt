package com.happynurse.presentation.screens.notification

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class NotificationSettings(
    val ivAlertEnabled: Boolean = true,
    val timerAlarmEnabled: Boolean = true,
    val orderAlertEnabled: Boolean = true,
    // 알림_046: 필수 안전 알림(투약 오류 경고)은 Off 불가
    val medicationErrorAlertEnabled: Boolean = true // 항상 true, UI에서 비활성화
)

@HiltViewModel
class NotificationSettingViewModel @Inject constructor() : ViewModel() {

    private val _settings = MutableStateFlow(NotificationSettings())
    val settings: StateFlow<NotificationSettings> = _settings

    fun updateIvAlert(enabled: Boolean) {
        _settings.value = _settings.value.copy(ivAlertEnabled = enabled)
    }

    fun updateTimerAlarm(enabled: Boolean) {
        _settings.value = _settings.value.copy(timerAlarmEnabled = enabled)
    }

    fun updateOrderAlert(enabled: Boolean) {
        _settings.value = _settings.value.copy(orderAlertEnabled = enabled)
    }
}
