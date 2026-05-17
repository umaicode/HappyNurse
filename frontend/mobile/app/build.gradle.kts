// app 모듈 빌드 설정 — Compose/Hilt/Retrofit/FCM 등 의존성을 버전 카탈로그(libs.*)로 일원화 관리
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.happynurse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.happynurse"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // 실기기 배포 BASE_URL — debug=DEV, release=PROD
        // (에뮬레이터에서 로컬 백엔드를 띄울 경우 buildConfigField 를 임시로 http://10.0.2.2:8080/ 로 바꿔서 빌드)
        debug {
            buildConfigField("String", "BASE_URL", "\"https://k14e101.p.ssafy.io/dev/api/\"")
            buildConfigField("String", "AI_BASE_URL", "\"https://k14e101.p.ssafy.io/dev/ai/\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://k14e101.p.ssafy.io/api/\"")
            buildConfigField("String", "AI_BASE_URL", "\"https://k14e101.p.ssafy.io/ai/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core / Lifecycle / Activity
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM + UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit + OkHttp logging
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    // SSE — 알림 실시간 스트림 (/sse/subscribe)
    implementation(libs.okhttp.sse)

    // Hilt (DI)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // FCM
    implementation(libs.firebase.messaging)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Wear OS DataLayer (폰-워치 통신)
    implementation(libs.play.services.wearable)

    // Coil (이미지 로딩)
    implementation(libs.coil.compose)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
