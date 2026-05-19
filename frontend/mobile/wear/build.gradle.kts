plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// google-services.json 이 존재할 때만 Firebase 플러그인 적용 (Console 등록 전 빌드 깨짐 방지)
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "com.happynurse.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.happynurse"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://k14e101.p.ssafy.io/dev/api/\""
            )
            buildConfigField(
                "String",
                "AI_BASE_URL",
                "\"https://k14e101.p.ssafy.io/dev/ai/\""
            )
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField(
                "String",
                "BASE_URL",
                "\"https://k14e101.p.ssafy.io/api/\""
            )
            buildConfigField(
                "String",
                "AI_BASE_URL",
                "\"https://k14e101.p.ssafy.io/ai/\""
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
    // Wear OS Compose (Material 3 Expressive)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Foundation extras (Pager 등)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)

    // Wear OS Core
    implementation(libs.androidx.wear)
    implementation(libs.play.services.wearable)

    // DataLayer (폰-워치 통신)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.androidx.wear.phone.interactions)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // ViewModel + Coroutine
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // 네트워크 — 워치가 직접 백엔드 REST API 호출
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // 토큰 캐시 — 폰에서 동기화한 accessToken/wardId 영속 저장
    implementation(libs.androidx.datastore.preferences)

    // Firebase Cloud Messaging — 워치 직접 푸시 수신
    implementation(libs.firebase.messaging)
}
