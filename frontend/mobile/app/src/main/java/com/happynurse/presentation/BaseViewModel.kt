// 공통 ViewModel — error StateFlow + Result<T> launch 헬퍼로 보일러플레이트 제거
package com.happynurse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    protected val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun consumeError() {
        _error.value = null
    }

    // viewModelScope.launch { repo.x().fold(onSuccess, onFailure→_error) } 보일러플레이트 1줄로 축약.
    // onFailure 시 throwable.message 를 _error 에 저장 (없으면 defaultErrorMessage).
    protected fun <T> launchWithResult(
        defaultErrorMessage: String = "오류가 발생했습니다",
        block: suspend () -> Result<T>,
        onSuccess: (T) -> Unit,
    ): Job = viewModelScope.launch {
        block().fold(
            onSuccess = onSuccess,
            onFailure = { _error.value = it.message ?: defaultErrorMessage },
        )
    }
}
