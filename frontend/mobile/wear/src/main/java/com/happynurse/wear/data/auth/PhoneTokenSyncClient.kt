// 워치에서 폰에 토큰 동기화를 요청하는 MessageClient 래퍼.
// 응답은 WearDataListenerService 가 WEAR_AUTH_TOKEN_RESPONSE path 로 수신한다.
package com.happynurse.wear.data.auth

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.happynurse.wear.data.remote.WearableMessagePaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneTokenSyncClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val messageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient by lazy { Wearable.getNodeClient(context) }

    suspend fun requestToken(): Result<Unit> = runCatching {
        val nodes = nodeClient.connectedNodes.await()
        if (nodes.isEmpty()) error("연결된 폰을 찾지 못했습니다")
        nodes.forEach { node ->
            messageClient.sendMessage(
                node.id,
                WearableMessagePaths.WEAR_AUTH_TOKEN_REQUEST,
                ByteArray(0),
            ).await()
        }
    }
}
