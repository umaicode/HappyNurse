// 메인 백엔드 API 와 AI 서버 API 가 baseUrl 이 다르므로 Retrofit 인스턴스를 구분하기 위한 Hilt qualifier 정의.
package com.happynurse.wear.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiRetrofit
