// 인증 저장소 — 로그인/로그아웃/토큰 갱신 + DataStore로 토큰 영속 저장
package com.happynurse.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    suspend fun login(
        organizationId: Long,
        wardId: Long,
        employeeNumber: String,
        password: String,
    ): Result<AppLoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(organizationId, wardId, employeeNumber, password))
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                saveSession(body.data)
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.message ?: "로그인 실패 (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refresh(): Result<AppLoginResponse> {
        return try {
            val currentRefresh = context.authDataStore.data.firstOrNull()?.get(KEY_REFRESH_TOKEN)
                ?: return Result.failure(Exception("저장된 refreshToken 없음"))
            val response = authApi.refresh(AppRefreshRequest(currentRefresh))
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                saveSession(body.data)
                Result.success(body.data)
            } else {
                clearSession()
                Result.failure(Exception(body?.message ?: "토큰 갱신 실패"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
