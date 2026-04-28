// 워치 Hilt 모듈 — Nfc/Gesture/Audio/DataClient 싱글톤 제공
package com.happynurse.wear.di

import android.content.Context
import com.happynurse.wear.data.audio.AudioRecorder
import com.happynurse.wear.data.nfc.NfcManager
import com.happynurse.wear.data.remote.WearDataClient
import com.happynurse.wear.data.sensor.GestureDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearAppModule {

    @Provides
    @Singleton
    fun provideNfcManager(): NfcManager = NfcManager()

    @Provides
    @Singleton
    fun provideGestureDetector(): GestureDetector = GestureDetector()

    @Provides
    @Singleton
    fun provideAudioRecorder(
        @ApplicationContext context: Context
    ): AudioRecorder = AudioRecorder(context)

    @Provides
    @Singleton
    fun provideWearDataClient(
        @ApplicationContext context: Context
    ): WearDataClient = WearDataClient(context)
}
