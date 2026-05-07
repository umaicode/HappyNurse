// HomeViewModel — 홈 화면(수액/타이머/환자알림 3-tab)의 상태와 카드 삭제 액션을 보유.
// 초기값은 MockData. self_report FCM 수신 시 WearEventBus 로 emit 된 이벤트를 환자알림 리스트에 prepend.
package com.happynurse.wear.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.data.model.MockData
import com.happynurse.wear.data.model.PatientSelfReport
import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.data.model.SymptomType
import com.happynurse.wear.data.notification.NotificationType
import com.happynurse.wear.data.notification.WearEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeTab(val label: String) {
    IV("수액"), STT("타이머"), REQ("환자알림"),
}

data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.IV,
    val ivList: List<IvInfusionTimer> = MockData.ivList,
    val sttList: List<SttTimer> = MockData.sttList,
    val reqList: List<PatientSelfReport> = MockData.reqList,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    eventBus: WearEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // FCM self_report 수신 시 환자알림 탭 카드에 prepend.
        viewModelScope.launch {
            eventBus.notifications.collect { notif ->
                if (notif.type != NotificationType.PATIENT_CALL) return@collect
                val newReport = PatientSelfReport(
                    selfReportId = -notif.timestamp,
                    patientName = notif.patientName.ifBlank { notif.title },
                    symptomType = SymptomType.PAIN,
                    symptomText = notif.title,
                    submittedRelative = "방금",
                    room = notif.roomLocation,
                    bedName = "",
                )
                _state.update { it.copy(reqList = listOf(newReport) + it.reqList) }
            }
        }
    }

    fun selectTab(tab: HomeTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun deleteIv(id: Long) {
        _state.update { it.copy(ivList = it.ivList.filterNot { iv -> iv.ivInfusionId == id }) }
    }

    fun deleteStt(id: String) {
        _state.update { it.copy(sttList = it.sttList.filterNot { stt -> stt.sttTimerId == id }) }
    }

    fun deleteReq(id: Long) {
        _state.update { it.copy(reqList = it.reqList.filterNot { r -> r.selfReportId == id }) }
    }
}
