// Hilt 모듈 — Retrofit(OkHttp + 토큰 인터셉터) 및 API/Repository 싱글톤 제공
package com.happynurse.di

import com.happynurse.BuildConfig
import com.happynurse.data.remote.AuthAuthenticator
import com.happynurse.data.remote.api.AuthApi
import com.happynurse.data.remote.api.DrugApi
import com.happynurse.data.remote.api.EncounterApi
import com.happynurse.data.remote.api.FcmTokenApi
import com.happynurse.data.remote.api.HandoverApi
import com.happynurse.data.remote.api.IvApi
import com.happynurse.data.remote.api.NotificationApi
import com.happynurse.data.remote.api.SttApi
import com.happynurse.data.remote.api.SttReminderApi
import com.happynurse.data.remote.api.HappyNurseApi
import com.happynurse.data.remote.api.NfcTokenApi
import com.happynurse.data.remote.api.OrganizationApi
import com.happynurse.data.remote.api.PatientApi
import com.happynurse.data.remote.api.PractitionerApi
import com.happynurse.data.remote.api.WardApi
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

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class PublicRetrofit
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AuthRetrofit
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AiRetrofit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // BASE_URL / AI_BASE_URL 은 build.gradle.kts 의 buildConfigField 에서 build type 별 분기 주입
    private val BASE_URL: String get() = BuildConfig.BASE_URL
    private val AI_BASE_URL: String get() = BuildConfig.AI_BASE_URL

    @Provides @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides @Singleton @PublicRetrofit
    fun providePublicRetrofit(logging: HttpLoggingInterceptor): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(logging).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton @AuthRetrofit
    fun provideAuthRetrofit(
        logging: HttpLoggingInterceptor,
        authRepository: AuthRepository,
        authenticator: AuthAuthenticator,
    ): Retrofit {
        val tokenInterceptor = Interceptor { chain ->
            val token = runBlocking { authRepository.accessToken.firstOrNull() }
            val req = if (token != null)
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            else chain.request()
            chain.proceed(req)
        }
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(tokenInterceptor)
                    .addInterceptor(logging)
                    .authenticator(authenticator)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides @Singleton fun provideAuthApi(@PublicRetrofit r: Retrofit): AuthApi = r.create(AuthApi::class.java)
    @Provides @Singleton fun provideOrganizationApi(@PublicRetrofit r: Retrofit): OrganizationApi = r.create(OrganizationApi::class.java)
    @Provides @Singleton fun provideHappyNurseApi(@AuthRetrofit r: Retrofit): HappyNurseApi = r.create(HappyNurseApi::class.java)
    @Provides @Singleton fun provideNfcTokenApi(@PublicRetrofit r: Retrofit): NfcTokenApi = r.create(NfcTokenApi::class.java)
    @Provides @Singleton fun provideFcmTokenApi(@AuthRetrofit r: Retrofit): FcmTokenApi = r.create(FcmTokenApi::class.java)
    @Provides @Singleton fun providePractitionerApi(@AuthRetrofit r: Retrofit): PractitionerApi = r.create(PractitionerApi::class.java)
    @Provides @Singleton fun provideWardApi(@AuthRetrofit r: Retrofit): WardApi = r.create(WardApi::class.java)
    @Provides @Singleton fun providePatientApi(@AuthRetrofit r: Retrofit): PatientApi = r.create(PatientApi::class.java)
    @Provides @Singleton fun provideEncounterApi(@AuthRetrofit r: Retrofit): EncounterApi = r.create(EncounterApi::class.java)
    @Provides @Singleton fun provideDrugApi(@AuthRetrofit r: Retrofit): DrugApi = r.create(DrugApi::class.java)
    @Provides @Singleton fun provideIvApi(@AuthRetrofit r: Retrofit): IvApi = r.create(IvApi::class.java)
    @Provides @Singleton fun provideNotificationApi(@AuthRetrofit r: Retrofit): NotificationApi = r.create(NotificationApi::class.java)
    @Provides @Singleton fun provideSttApi(@AiRetrofit r: Retrofit): SttApi = r.create(SttApi::class.java)
    @Provides @Singleton fun provideHandoverApi(@AiRetrofit r: Retrofit): HandoverApi = r.create(HandoverApi::class.java)
    @Provides @Singleton fun provideSttReminderApi(@AuthRetrofit r: Retrofit): SttReminderApi = r.create(SttReminderApi::class.java)

    // AI 서버용 별도 Retrofit — STT/correction 등. Bearer 토큰 동일하게 주입 (AuthRepository 의 access_token 재활용).
    // AuthAuthenticator 도 같이 붙여 — AI 서버가 401(토큰 만료) 시 /auth/refresh 자동 호출 후 재시도.
    @Provides @Singleton @AiRetrofit
    fun provideAiRetrofit(
        logging: HttpLoggingInterceptor,
        authRepository: AuthRepository,
        authenticator: AuthAuthenticator,
    ): Retrofit {
        val tokenInterceptor = Interceptor { chain ->
            val token = runBlocking { authRepository.accessToken.firstOrNull() }
            val req = if (token != null)
                chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            else chain.request()
            chain.proceed(req)
        }
        return Retrofit.Builder()
            .baseUrl(AI_BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(tokenInterceptor)
                    .addInterceptor(logging)
                    .authenticator(authenticator)
                    .build(),
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
