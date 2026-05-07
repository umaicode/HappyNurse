// Hilt 모듈 — Retrofit(OkHttp + 토큰 인터셉터) 및 API/Repository 싱글톤 제공
package com.happynurse.di

import com.happynurse.data.remote.AuthAuthenticator
import com.happynurse.data.remote.api.AuthApi
import com.happynurse.data.remote.api.EncounterApi
import com.happynurse.data.remote.api.FcmTokenApi
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

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // URL은 BuildConfig 대신 상수 문자열로 선언 — AGP 9.x + KSP 타이밍 이슈 회피
    private const val BASE_URL = "https://k14e101.p.ssafy.io/dev/api/"

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
}
