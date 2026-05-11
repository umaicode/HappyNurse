// 워치 Hilt 모듈 — @Inject 로 자동 주입 불가능한 외부 클래스(GestureDetector 등) 만 명시 제공한다.
// AudioRecorder 등 @Singleton + @Inject constructor 로 정의된 클래스는 Hilt 가 자동 주입한다.
package com.happynurse.wear.di

import android.content.Context
import com.happynurse.wear.alarm.AlarmScheduler
import com.happynurse.wear.data.remote.WearDataClient
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
    fun provideWearDataClient(
        @ApplicationContext context: Context,
    ): WearDataClient = WearDataClient(context)

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context,
    ): AlarmScheduler = AlarmScheduler(context)
}
