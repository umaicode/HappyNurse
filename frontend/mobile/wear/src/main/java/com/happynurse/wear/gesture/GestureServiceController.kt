// GestureServiceController — accessToken 의 존재 여부에 따라 GestureService 를 start/stop.
// WearApplication.onCreate() 에서 bind() 1회 호출.
package com.happynurse.wear.gesture

import android.content.Context
import com.happynurse.wear.data.remote.WearTokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: WearTokenStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var bound = false

    fun bind() {
        if (bound) return
        bound = true
        tokenStore.accessTokenFlow
            .map { !it.isNullOrBlank() }
            .distinctUntilChanged()
            .onEach { loggedIn ->
                if (loggedIn) GestureService.start(context) else GestureService.stop(context)
            }
            .launchIn(scope)
    }
}
