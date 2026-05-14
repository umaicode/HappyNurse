// 세션 상태 ViewModel — DataStore 첫 emit 전엔 null (결정 전), 이후 토큰 유무로 true/false
package com.happynurse.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {
    // null 초기값으로 DataStore 첫 emit 까지 NavGraph 가 진입 분기를 보류하게 함 (LOGIN/MAIN 깜빡임 방지)
    val isLoggedIn: StateFlow<Boolean?> =
        authRepository.isLoggedIn.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
