// 401 응답 시 refreshToken으로 access 갱신 후 원 요청 재시도, 실패 시 세션 정리(자동 로그아웃)
package com.happynurse.data.remote

import com.happynurse.data.repository.AuthRepository
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthAuthenticator @Inject constructor(
    private val authRepository: Lazy<AuthRepository>,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        if (responseCount(response) >= 2) return null

        val newToken = runBlocking {
            authRepository.get().refresh().getOrNull()?.accessToken
        } ?: run {
            runBlocking { authRepository.get().logout() }
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
