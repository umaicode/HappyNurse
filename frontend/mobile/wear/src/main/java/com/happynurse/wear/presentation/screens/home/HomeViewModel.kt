// HomeViewModel — 홈 화면(수액/타이머)의 상태와 API 연동을 담당.
// 폰에서 동기화된 토큰/병동 식별자가 준비되면 /iv 와 /reminders/stt 를 주기적으로 호출한다.
package com.happynurse.wear.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.wear.alarm.AlarmScheduler
import com.happynurse.wear.data.remote.wearable.PhoneTokenSyncClient
import com.happynurse.wear.data.remote.WearTokenStore
import com.happynurse.wear.domain.model.IvInfusionTimer
import com.happynurse.wear.domain.model.SttTimer
import com.happynurse.wear.data.repository.IvInfusionRepository
import com.happynurse.wear.data.repository.SttReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

enum class HomeTab(val label: String) {
    IV("수액"), STT("타이머"),
}

data class HomeUiState(
    val selectedTab: HomeTab = HomeTab.IV,
    val ivList: List<IvInfusionTimer> = emptyList(),
    val sttList: List<SttTimer> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ivInfusionRepository: IvInfusionRepository,
    private val sttReminderRepository: SttReminderRepository,
    private val tokenStore: WearTokenStore,
    private val phoneTokenSyncClient: PhoneTokenSyncClient,
    private val alarmScheduler: AlarmScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        android.util.Log.d("HomeVM", "init called")
        observeWardId()
        startCountdownTicker()
        viewModelScope.launch {
            val token = tokenStore.accessToken()
            android.util.Log.d("HomeVM", "init token isBlank=${token.isNullOrBlank()}")
            // 첫 진입 시 토큰이 없으면 폰에 요청
            if (token.isNullOrBlank()) {
                android.util.Log.d("HomeVM", "requesting token from phone...")
                runCatching { phoneTokenSyncClient.requestToken() }
                    .onSuccess { android.util.Log.d("HomeVM", "requestToken success") }
                    .onFailure { android.util.Log.e("HomeVM", "requestToken failed", it) }
            }
        }
    }

    private fun observeWardId() {
        viewModelScope.launch {
            android.util.Log.d("HomeVM", "observeWardId start")
            tokenStore.wardIdFlow.distinctUntilChanged().collectLatest { wardId ->
                android.util.Log.d("HomeVM", "wardId emitted=$wardId")
                if (wardId == null) {
                    android.util.Log.w("HomeVM", "wardId is null -> skip refresh")
                    _state.update { it.copy(ivList = emptyList(), sttList = emptyList()) }
                    return@collectLatest
                }
                while (true) {
                    android.util.Log.d("HomeVM", "refresh start wardId=$wardId")
                    refresh(wardId)
                    delay(REFRESH_INTERVAL_MS)
                }
            }
        }
    }

    private suspend fun refresh(wardId: Long) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val ivResult = ivInfusionRepository.fetch(wardId)
        val sttResult = sttReminderRepository.fetch()
        _state.update { current ->
            current.copy(
                isLoading = false,
                ivList = ivResult.getOrDefault(current.ivList).filter { it.remainingSec > 0 },
                sttList = sttResult.getOrDefault(current.sttList).filter { it.remainingSec > 0 },
                errorMessage = listOfNotNull(
                    ivResult.exceptionOrNull()?.message,
                    sttResult.exceptionOrNull()?.message,
                ).firstOrNull(),
            )
        }
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (true) {
                delay(TICK_INTERVAL_MS)
                val now = Instant.now()
                _state.update { current ->
                    current.copy(
                        ivList = current.ivList
                            .map { it.tickedTo(now) }
                            .filter { it.remainingSec > 0 },
                        sttList = current.sttList
                            .map { it.tickedTo(now) }
                            .filter { it.remainingSec > 0 },
                    )
                }
            }
        }
    }

    fun selectTab(tab: HomeTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun cancelSttAlarm(stt: SttTimer) {
        // 낙관적 업데이트 — 리스트에서 즉시 제거, 백엔드 취소 + 로컬 AlarmManager 취소
        _state.update { current ->
            current.copy(sttList = current.sttList.filterNot { it.sttReminderId == stt.sttReminderId })
        }
        alarmScheduler.cancelSttAlarm(stt.sttReminderId.toString())
        viewModelScope.launch {
            sttReminderRepository.cancel(stt.sttReminderId)
        }
    }

    private fun IvInfusionTimer.tickedTo(now: Instant): IvInfusionTimer {
        val end = expectedEndAt ?: return this
        val newRemaining = Duration.between(now, end).seconds.toInt().coerceAtLeast(0)
        return if (newRemaining == remainingSec) this else copy(remainingSec = newRemaining)
    }

    private fun SttTimer.tickedTo(now: Instant): SttTimer {
        val fire = fireAt ?: return this
        val newRemaining = Duration.between(now, fire).seconds.toInt().coerceAtLeast(0)
        return if (newRemaining == remainingSec) this else copy(remainingSec = newRemaining)
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 30_000L
        const val TICK_INTERVAL_MS = 1_000L
    }
}
