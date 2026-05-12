// 사용자가 스와이프/모두지우기로 닫은 알림 id 영속 저장 — 백엔드 삭제 API 가 없어서 로컬 보관
package com.happynurse.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationDismissDataStore by preferencesDataStore(name = "notification_dismiss_prefs")

@Singleton
class NotificationDismissRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringSetPreferencesKey("dismissed_ids")

    val dismissedIds: Flow<Set<String>> =
        context.notificationDismissDataStore.data.map { it[key].orEmpty() }

    suspend fun snapshot(): Set<String> = dismissedIds.firstOrNull().orEmpty()

    suspend fun dismiss(id: String) {
        context.notificationDismissDataStore.edit { prefs ->
            prefs[key] = (prefs[key].orEmpty() + id)
        }
    }

    suspend fun dismissAll(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.notificationDismissDataStore.edit { prefs ->
            prefs[key] = (prefs[key].orEmpty() + ids)
        }
    }
}
