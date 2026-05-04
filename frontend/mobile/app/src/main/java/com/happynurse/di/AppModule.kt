// Hilt 모듈 — Retrofit(OkHttp + 토큰 인터셉터) 및 API/Repository 싱글톤 제공
package com.happynurse.di

import com.happynurse.BuildConfig
import com.happynurse.data.remote.api.AuthApi
import com.happynurse.data.remote.api.FcmTokenApi
import com.happynurse.data.remote.api.HappyNurseApi
import com.happynurse.data.remote.api.NfcTokenApi
import com.happynurse.data.remote.api.OrganizationApi
import com.happynurse.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

// 인증 토큰 없는 Retrofit (로그인/병원 목록 등 공개 API용)
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicRetrofit

// 인증 토큰 자동 첨부 Retrofit (인증 필요 API용)
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private fun baseUrl() = BuildConfig.BASE_URL

    // ── OkHttp 로깅 (debug 빌드에서만 body 출력)
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

    // ── 토큰 없는 Retrofit (로그인, 병원/병동 목록)
    @Provides
    @Singleton
    @PublicRetrofit
    fun providePublicRetrofit(logging: HttpLoggingInterceptor): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(OkHttpClient.Builder().addInterceptor(logging).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // ── 토큰 자동 첨부 Retrofit (로그인 후 모든 API)
    @Provides
    @Singleton
    @AuthRetrofit
    fun provideAuthRetrofit(
        logging: HttpLoggingInterceptor,
        authRepository: AuthRepository,
    ): Retrofit {
        val tokenInterceptor = Interceptor { chain ->
            val token = runBlocking { authRepository.accessToken.firstOrNull() }
            val req = if (token != null) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(req)
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl())
            .client(OkHttpClient.Builder().addInterceptor(tokenInterceptor).addInterceptor(logging).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ── API 인스턴스
    @Provides @Singleton
    fun provideAuthApi(@PublicRetrofit retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun provideOrganizationApi(@PublicRetrofit retrofit: Retrofit): OrganizationApi =
        retrofit.create(OrganizationApi::class.java)

    @Provides @Singleton
    fun provideHappyNurseApi(@AuthRetrofit retrofit: Retrofit): HappyNurseApi =
        retrofit.create(HappyNurseApi::class.java)

    @Provides @Singleton
    fun provideNfcTokenApi(@AuthRetrofit retrofit: Retrofit): NfcTokenApi =
        retrofit.create(NfcTokenApi::class.java)

    @Provides @Singleton
    fun provideFcmTokenApi(@AuthRetrofit retrofit: Retrofit): FcmTokenApi =
        retrofit.create(FcmTokenApi::class.java)

}
