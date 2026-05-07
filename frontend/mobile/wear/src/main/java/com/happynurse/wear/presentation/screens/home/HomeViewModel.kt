// HomeViewModel — 홈 화면(수액/타이머/환자알림 3-tab)의 상태와 카드 삭제 액션을 보유.
// 초기값은 MockData. 실제 데이터는 추후 WearEventBus / WearDataClient 에서 주입.
package com.happynurse.wear.presentation.screens.home

import androidx.lifecycle.ViewModel
import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.data.model.MockData
import com.happynurse.wear.data.model.PatientSelfReport
import com.happynurse.wear.data.model.SttTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

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
