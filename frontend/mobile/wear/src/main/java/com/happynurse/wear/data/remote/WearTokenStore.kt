// 워치 자체 토큰 캐시 — 폰에서 동기화 받은 accessToken/wardId 를 DataStore Preferences 로 영속 저장한다.
// AuthInterceptor 가 헤더 부착 시 읽고, 로그아웃 메시지 수신 시 clear 한다.
package com.happynurse.wear.data.remote

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.wearAuthDataStore by preferencesDataStore(name = "wear_auth_prefs")

@Singleton
class WearTokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        val KEY_WARD_ID = longPreferencesKey("ward_id")
    }

    val accessTokenFlow: Flow<String?> = context.wearAuthDataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val wardIdFlow: Flow<Long?> = context.wearAuthDataStore.data.map { it[KEY_WARD_ID] }

    suspend fun accessToken(): String? = accessTokenFlow.firstOrNull()

    suspend fun wardId(): Long? = wardIdFlow.firstOrNull()

    suspend fun save(accessToken: String, wardId: Long) {
        context.wearAuthDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_WARD_ID] = wardId
        }
    }

    suspend fun clear() {
        context.wearAuthDataStore.edit { it.clear() }
    }
}
