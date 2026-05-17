// 알림 SSE 스트림 — 백엔드 두 채널을 long-lived 구독해 한 SharedFlow 로 합쳐 발사.
//
// 채널:
//   - personal (/sse/subscribe)         : 본인 담당 알림 (self_report, iv_alert, order_change, timer, vital_alert)
//   - ward     (/sse/ward-subscribe)    : 병동 전체 (nursing_record, medication_admin + 위 5개도 동일하게 발사됨)
//
// 두 채널을 독립된 코루틴 잡으로 띄우고 각각 자체 지수백오프 재연결을 수행한다. 이벤트는 type 그대로
// _events 에 emit — 소비자(ViewModel) 가 sourceType 으로 필터링한다.
//
// 채널별 sourceType 책임 분배 (현재 사용처):
//   TasksViewModel              : personal 채널의 알림 sourceType 5종으로 알림함 새로고침
//   PatientDetailViewModel      : ward 채널의 nursing_record/medication_admin 으로 간호일지 새로고침
//
// 한 sourceType (예: order_change) 이 두 채널에서 동시에 발사될 수도 있는데, 소비자 호출은 idempotent
// (refreshBellNotifications/loadNotes 둘 다 GET 재조회) 하므로 중복은 비용 외 문제 없다.
//
// heartbeat / ready 이벤트는 두 채널 모두에서 무시.
package com.happynurse.data.remote.sse

import android.util.Log
import com.happynurse.BuildConfig
import com.happynurse.data.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationStreamEvent(
    val sourceType: String,
    val data: String,
)

@Singleton
class NotificationStream @Inject constructor(
    private val authRepository: AuthRepository,
) {
    private val _events = MutableSharedFlow<NotificationStreamEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<NotificationStreamEvent> = _events.asSharedFlow()

    // SSE 는 long-lived 연결 — readTimeout 0(무제한) 로 별도 OkHttpClient.
    // Bearer 토큰은 매 연결마다 최신값으로 헤더에 직접 부착 (인터셉터 거치지 않음).
    private val sseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private var personalJob: Job? = null
    private var wardJob: Job? = null
    private var personalSource: EventSource? = null
    private var wardSource: EventSource? = null

    fun start(scope: CoroutineScope) {
        if (personalJob?.isActive != true) {
            personalJob = scope.launch { runChannel("sse/subscribe", "personal") { personalSource = it } }
        }
        if (wardJob?.isActive != true) {
            wardJob = scope.launch { runChannel("sse/ward-subscribe", "ward") { wardSource = it } }
        }
    }

    fun stop() {
        personalJob?.cancel(); personalJob = null
        wardJob?.cancel(); wardJob = null
        personalSource?.cancel(); personalSource = null
        wardSource?.cancel(); wardSource = null
    }

    // 한 채널의 lifecycle — 토큰 대기 → openOnce → 재연결 백오프 무한 루프.
    private suspend fun runChannel(
        path: String,
        channelLabel: String,
        setSource: (EventSource) -> Unit,
    ) {
        var backoffMs = 1_000L
        while (currentCoroutineContext().isActive) {
            val token = authRepository.accessToken.firstOrNull()
            if (token.isNullOrBlank()) {
                delay(1_000L)
                continue
            }
            val finished = openOnce(path, channelLabel, token, setSource)
            val waitMs = if (finished) 1_000L else backoffMs
            delay(waitMs)
            backoffMs = if (finished) 1_000L else (backoffMs * 2).coerceAtMost(30_000L)
        }
    }

    // 한 번 연결을 시도하고 종료(정상/오류)될 때까지 suspend.
    // return: true = 정상 종료(onClosed), false = 실패
    private suspend fun openOnce(
        path: String,
        channelLabel: String,
        token: String,
        setSource: (EventSource) -> Unit,
    ): Boolean {
        val completion = CompletableDeferred<Boolean>()
        val resolve: (Boolean) -> Unit = { ok -> if (!completion.isCompleted) completion.complete(ok) }

        val request = Request.Builder()
            .url(BuildConfig.BASE_URL + path)
            .header("Authorization", "Bearer $token")
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                if (type == null || type == "heartbeat" || type == "ready") return
                _events.tryEmit(NotificationStreamEvent(sourceType = type, data = data))
            }

            override fun onClosed(eventSource: EventSource) {
                resolve(true)
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                // 운영 중 연결 끊김 진단용 — 백오프 재연결과 함께 동작하므로 경고 수준 유지.
                Log.w(TAG, "SSE failure channel=$channelLabel code=${response?.code} msg=${t?.message}")
                resolve(false)
            }
        }

        val source = EventSources.createFactory(sseClient).newEventSource(request, listener)
        setSource(source)
        return completion.await()
    }

    private companion object {
        const val TAG = "NotificationStream"
    }
}
