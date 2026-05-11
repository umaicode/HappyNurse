// 워치 네트워크 DI 모듈 — 메인 API(@MainRetrofit) 와 AI 서버(@AiRetrofit) 두 Retrofit 인스턴스를 분리해 제공한다.
package com.happynurse.wear.di

import com.happynurse.wear.BuildConfig
import com.happynurse.wear.data.auth.AuthInterceptor
import com.happynurse.wear.data.remote.api.IvInfusionApi
import com.happynurse.wear.data.remote.api.SttRecognitionApi
import com.happynurse.wear.data.remote.api.SttReminderApi
import com.happynurse.wear.data.remote.api.WardPatientApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            // STT 음성 업로드를 고려해 read 타임아웃을 길게 잡는다.
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @MainRetrofit
    fun provideMainRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    @AiRetrofit
    fun provideAiRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.AI_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideIvInfusionApi(@MainRetrofit retrofit: Retrofit): IvInfusionApi =
        retrofit.create(IvInfusionApi::class.java)

    @Provides
    @Singleton
    fun provideSttReminderApi(@MainRetrofit retrofit: Retrofit): SttReminderApi =
        retrofit.create(SttReminderApi::class.java)

    @Provides
    @Singleton
    fun provideWardPatientApi(@MainRetrofit retrofit: Retrofit): WardPatientApi =
        retrofit.create(WardPatientApi::class.java)

    @Provides
    @Singleton
    fun provideSttRecognitionApi(@AiRetrofit retrofit: Retrofit): SttRecognitionApi =
        retrofit.create(SttRecognitionApi::class.java)
}
