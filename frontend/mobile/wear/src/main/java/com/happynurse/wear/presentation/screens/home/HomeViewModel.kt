// HomeViewModel — 홈 화면(수액/타이머 2-tab)의 상태와 카드 삭제 액션을 보유.
package com.happynurse.wear.presentation.screens.home

import androidx.lifecycle.ViewModel
import com.happynurse.wear.data.model.IvInfusionTimer
import com.happynurse.wear.data.model.MockData
import com.happynurse.wear.data.model.SttTimer
import com.happynurse.wear.data.notification.WearEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class HomeTab(val label: String) {
    IV("수액"), STT("타이머"),
}

data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.IV,
    val ivList: List<IvInfusionTimer> = MockData.ivList,
    val sttList: List<SttTimer> = MockData.sttList,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @Suppress("UNUSED_PARAMETER") eventBus: WearEventBus,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun selectTab(tab: HomeTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun deleteStt(id: String) {
        _state.update { it.copy(sttList = it.sttList.filterNot { stt -> stt.sttTimerId == id }) }
    }
}
