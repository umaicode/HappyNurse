package com.happynurse.data.wearable

import android.content.Context
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneDataClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    suspend fun send(path: String, payload: ByteArray = ByteArray(0)) {
        val nodes = nodeClient.connectedNodes.await()
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, path, payload).await()
        }
    }
}
