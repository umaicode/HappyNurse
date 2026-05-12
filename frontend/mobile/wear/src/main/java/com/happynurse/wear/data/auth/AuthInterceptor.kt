// OkHttp 인터셉터 — 모든 요청에 Authorization: Bearer {accessToken} 헤더를 부착한다.
// 401 응답을 받으면 폰에 토큰 재동기화를 요청한 뒤 한 번만 재시도한다.
package com.happynurse.wear.data.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: WearTokenStore,
    private val phoneTokenSyncClient: PhoneTokenSyncClient,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.accessToken() }
        val initialRequest = chain.request().attachAuth(token)
        val response = chain.proceed(initialRequest)
        if (response.code != UNAUTHORIZED) return response

        response.close()
        val refreshed = runBlocking {
            phoneTokenSyncClient.requestToken().getOrElse { return@runBlocking null }
            // 폰의 응답이 도착해 DataStore 가 업데이트되기까지 약간 기다린다.
            repeat(REFRESH_POLL_ATTEMPTS) {
                delay(REFRESH_POLL_INTERVAL_MS)
                val candidate = tokenStore.accessTokenFlow.firstOrNull()
                if (candidate != null && candidate != token) return@runBlocking candidate
            }
            null
        } ?: return chain.proceed(initialRequest)

        val retried = chain.request().attachAuth(refreshed)
        return chain.proceed(retried)
    }

    private fun Request.attachAuth(token: String?): Request {
        if (token.isNullOrBlank()) return this
        return newBuilder().header("Authorization", "Bearer $token").build()
    }

    private companion object {
        const val UNAUTHORIZED = 401
        const val REFRESH_POLL_ATTEMPTS = 10
        const val REFRESH_POLL_INTERVAL_MS = 200L
    }
}
