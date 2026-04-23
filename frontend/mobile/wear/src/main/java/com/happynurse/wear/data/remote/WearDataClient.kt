package com.happynurse.wear.data.remote

import android.content.Context
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// 폰 ↔ 워치 DataLayer API 통신 (음성 파일, 알림, 태깅 결과 전송)
@Singleton
class WearDataClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val messageClient = Wearable.getMessageClient(context)
    private val dataClient = Wearable.getDataClient(context)

    // 음성STT_023: 노이즈 캔슬링된 음성 파일을 폰으로 전송
    suspend fun sendAudioToPhone(filePath: String) {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        val audioBytes = java.io.File(filePath).readBytes()
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, "/audio/recording", audioBytes).await()
        }
    }

    // NFC 태깅 결과를 폰으로 전송
    suspend fun sendTagResultToPhone(path: String, data: ByteArray) {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, path, data).await()
        }
    }
}
