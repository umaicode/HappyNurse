// 인증 저장소 — 로그인/로그아웃/토큰 갱신 + DataStore로 토큰 영속 저장
package com.happynurse.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.happynurse.data.remote.apiCall
import com.happynurse.data.remote.api.AuthApi
import com.happynurse.data.remote.model.AppLoginResponse
import com.happynurse.data.remote.model.AppRefreshRequest
import com.happynurse.data.remote.model.LoginRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authApi: AuthApi,
) {
    companion object {
        private val KEY_ACCESS_TOKEN    = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN   = stringPreferencesKey("refresh_token")
        private val KEY_PRACTITIONER_ID = longPreferencesKey("practitioner_id")
        private val KEY_NAME            = stringPreferencesKey("name")
        private val KEY_EMPLOYEE_NUMBER = stringPreferencesKey("employee_number")
        private val KEY_ROLE_CODE       = stringPreferencesKey("role_code")
        private val KEY_ORG_ID          = longPreferencesKey("organization_id")
        private val KEY_WARD_ID         = longPreferencesKey("ward_id")
    }

    val accessToken: Flow<String?> = context.authDataStore.data.map { it[KEY_ACCESS_TOKEN] }

    val isLoggedIn: Flow<Boolean> = accessToken.map { it != null }

    // 본인 wardId 노출 — IV 보드 / 알림 fetch 등에서 사용
    val wardId: Flow<Long?> = context.authDataStore.data.map { it[KEY_WARD_ID] }

    // 본인 이름 노출 — 인수인계 체크리스트 by 메타 표시 등에 사용
    val displayName: Flow<String?> = context.authDataStore.data.map { it[KEY_NAME] }

    suspend fun login(
        organizationId: Long,
        wardId: Long,
        employeeNumber: String,
        password: String,
    ): Result<AppLoginResponse> =
        apiCall("로그인 실패") {
            authApi.login(LoginRequest(organizationId, wardId, employeeNumber, password))
        }.onSuccess { saveSession(it) }

    suspend fun refresh(): Result<AppLoginResponse> {
        val currentRefresh = context.authDataStore.data.firstOrNull()?.get(KEY_REFRESH_TOKEN)
            ?: return Result.failure(Exception("저장된 refreshToken 없음"))
        return apiCall("토큰 갱신 실패") { authApi.refresh(AppRefreshRequest(currentRefresh)) }
            .onSuccess { saveSession(it) }
            .onFailure { clearSession() }
    }

    suspend fun logout() {
        try { authApi.logout() } catch (_: Exception) { }
        clearSession()
    }

    private suspend fun saveSession(data: AppLoginResponse) {
        context.authDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN]    = data.accessToken
            prefs[KEY_REFRESH_TOKEN]   = data.refreshToken
            prefs[KEY_PRACTITIONER_ID] = data.practitionerId
            prefs[KEY_NAME]            = data.name
            prefs[KEY_EMPLOYEE_NUMBER] = data.employeeNumber
            prefs[KEY_ROLE_CODE]       = data.roleCode
            prefs[KEY_ORG_ID]          = data.organizationId
            prefs[KEY_WARD_ID]         = data.wardId
        }
    }

    private suspend fun clearSession() {
        context.authDataStore.edit { it.clear() }
    }
}
