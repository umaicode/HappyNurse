// Hilt 모듈 — Retrofit 인스턴스와 API 싱글톤 제공
package com.happynurse.di

import android.os.Build
import com.happynurse.BuildConfig
import com.happynurse.data.remote.api.HappyNurseApi
import com.happynurse.data.remote.api.NfcTokenApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val EMULATOR_BASE_URL = "http://10.0.2.2:8080/"

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        val baseUrl = if (isRunningOnEmulator()) EMULATOR_BASE_URL else BuildConfig.BASE_URL
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHappyNurseApi(retrofit: Retrofit): HappyNurseApi =
        retrofit.create(HappyNurseApi::class.java)

    @Provides
    @Singleton
    fun provideNfcTokenApi(retrofit: Retrofit): NfcTokenApi =
        retrofit.create(NfcTokenApi::class.java)

    // 런타임 에뮬레이터 감지 — AVD 인 경우 호스트 머신 백엔드 (10.0.2.2) 로 가게 함
    private fun isRunningOnEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.PRODUCT == "google_sdk" ||
            Build.PRODUCT == "sdk_gphone64_x86_64" ||
            Build.HARDWARE.contains("ranchu")
    }
}