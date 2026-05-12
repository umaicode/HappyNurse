// OkHttp 토큰 인터셉터 — DataStore 의 accessToken 을 Authorization 헤더에 부착
package com.happynurse.data.remote

import com.happynurse.data.repository.AuthRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor

fun bearerTokenInterceptor(authRepository: AuthRepository): Interceptor =
    Interceptor { chain ->
        val token = runBlocking { authRepository.accessToken.firstOrNull() }
        val req = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(req)
    }
