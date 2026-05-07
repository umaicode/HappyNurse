// 세션 상태 ViewModel — accessToken 유무를 관찰해 자동 로그아웃 시 로그인 화면으로 유도
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
    val isLoggedIn: StateFlow<Boolean> =
        authRepository.isLoggedIn.stateIn(viewModelScope, SharingStarted.Eagerly, true)
}
