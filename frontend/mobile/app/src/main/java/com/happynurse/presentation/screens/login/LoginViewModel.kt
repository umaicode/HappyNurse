// 로그인 화면 ViewModel — 병원/병동 목록 로드, 로그인/로그아웃 상태 관리
package com.happynurse.presentation.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.remote.api.OrganizationApi
import com.happynurse.data.remote.fcm.FcmTokenRegistrar
import com.happynurse.data.remote.model.OrganizationDto
import com.happynurse.data.remote.model.WardDto
import com.happynurse.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val organizations: List<OrganizationDto> = emptyList(),
    val wards: List<WardDto> = emptyList(),
    val selectedOrg: OrganizationDto? = null,
    val selectedWard: WardDto? = null,
    val employeeNumber: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val organizationApi: OrganizationApi,
    private val fcmTokenRegistrar: FcmTokenRegistrar,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        loadOrganizations()
    }

    private fun loadOrganizations() {
        viewModelScope.launch {
            try {
                val res = organizationApi.listOrganizations()
                if (res.isSuccessful) {
                    val orgs = res.body()?.data.orEmpty()
                    _uiState.value = _uiState.value.copy(organizations = orgs)
                }
            } catch (_: Exception) { }
        }
    }

    fun selectOrganization(org: OrganizationDto) {
        _uiState.value = _uiState.value.copy(
            selectedOrg = org,
            selectedWard = null,
            wards = emptyList(),
        )
        viewModelScope.launch {
            try {
                val res = organizationApi.listWards(org.organizationId)
                if (res.isSuccessful) {
                    _uiState.value = _uiState.value.copy(wards = res.body()?.data.orEmpty())
                }
            } catch (_: Exception) { }
        }
    }

    fun selectWard(ward: WardDto) {
        _uiState.value = _uiState.value.copy(selectedWard = ward)
    }

    fun setEmployeeNumber(v: String) {
        _uiState.value = _uiState.value.copy(employeeNumber = v, error = null)
    }

    fun setPassword(v: String) {
        _uiState.value = _uiState.value.copy(password = v, error = null)
    }

    fun login() {
        val s = _uiState.value
        val org = s.selectedOrg ?: return
        val ward = s.selectedWard ?: return
        viewModelScope.launch {
            _uiState.value = s.copy(loading = true, error = null)
            val result = authRepository.login(
                organizationId = org.organizationId,
                wardId = ward.wardId,
                employeeNumber = s.employeeNumber,
                password = s.password,
            )
            result.fold(
                onSuccess = {
                    fcmTokenRegistrar.registerCurrentToken()
                    _uiState.value = _uiState.value.copy(loading = false, loggedIn = true)
                },
                onFailure = { _uiState.value = _uiState.value.copy(loading = false, error = it.message) },
            )
        }
    }

    fun loginWithBiometric() {
        // 생체인증 성공 후 저장된 토큰으로 로그인된 상태로 전환
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loggedIn = true)
        }
    }
}
