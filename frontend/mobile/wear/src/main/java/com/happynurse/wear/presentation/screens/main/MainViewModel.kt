package com.happynurse.wear.presentation.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.data.notification.NotificationType
import com.happynurse.wear.data.notification.WearEventBus
import com.happynurse.wear.data.notification.WearNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MainTab(val label: String, val type: NotificationType) {
    IV("수액", NotificationType.IV_ALERT),
    TIMER("타이머", NotificationType.TIMER_ALARM),
    PATIENT("환자", NotificationType.PATIENT_CALL),
}

@HiltViewModel
class MainViewModel @Inject constructor(
    eventBus: WearEventBus,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainTab.IV)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _notifications = MutableStateFlow<Map<NotificationType, List<WearNotification>>>(
        NotificationType.entries.associateWith { emptyList() }
    )
    val notifications: StateFlow<Map<NotificationType, List<WearNotification>>> =
        _notifications.asStateFlow()

    init {
        viewModelScope.launch {
            eventBus.notifications.collect { incoming ->
                _notifications.update { current ->
                    val list = current[incoming.type].orEmpty() + incoming
                    current + (incoming.type to list)
                }
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }
}
