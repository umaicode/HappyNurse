# PR 1 — Wristband NFC 태깅 Implementation Plan

> 본 plan 은 `frontend/mobile/docs/superpowers/specs/2026-05-07-nfc-patient-tagging-design.md` 의 구현용. 사용자 명시 요청 없이 git commit 단계는 두지 않는다.

**Goal:** 환자 wristband NFC 칩을 태깅하여 환자 정보를 식별하고, 후속 화면(약물 등록 / 간호일지) 으로 patientId + encounterId 를 전달한다.

**Architecture:** `/nfc/patients/entry` (token → patient 기본 정보) + `/patient/{patientId}` (encounter + 풍부한 환자 정보) 2단계 호출. 백엔드 변경 0. NFC 칩의 NDEF URI record 의 `?token=` 쿼리 파라미터에서 token 추출.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Retrofit, OkHttp, Gson, Coroutines/Flow, Android NFC (`android.nfc.tech.Ndef`).

**프로젝트 컨벤션 (`docs/CLAUDE.md` 3장 기준):**
- Retrofit: `Response<ApiResponse<T>>` wrapper. 모든 DTO 필드에 `@SerializedName`.
- Repository: `Result<T>` 반환. `runCatching` + `body?.success == true && body.data != null` unwrap. `@Singleton + @Inject`.
- ViewModel: `@HiltViewModel`. `MutableStateFlow + asStateFlow()`.
- DTO 명명: `XxxRequest` / `XxxResponse` / `XxxDto`.
- 단위 테스트는 본 프로젝트 패턴상 없음 — 빌드 통과 + 코드 리뷰 + 실기기 검증으로 대체.

**검증 정책:**
- Step A 끝: `./gradlew assembleDebug` 통과 + 추가 사실 확인 (호출처 컴파일 에러 없음)
- Step B 끝: 실기기 NFC 태깅 → 환자 정보 표시 + 후속 화면 인자 도착 확인
- 각 task 끝에 빌드 또는 시각적 검증 step 1개 포함

---

## Step A — Data Layer

### Task A1: `NfcEntryDto.kt` 신규 + `NfcTokenApi.kt` 정정

**Files:**
- Create: `app/src/main/java/com/happynurse/data/remote/model/NfcEntryDto.kt`
- Modify: `app/src/main/java/com/happynurse/data/remote/api/NfcTokenApi.kt`

**근거:** 현 `NfcTokenApi.kt` 가 wrapper 없이 bare DTO 를 반환 + DTO 가 인라인 + `@SerializedName` 누락 — 컨벤션 예외. 본 PR 에서 표준 wrapper 패턴으로 정정 + DTO 분리.

- [ ] **Step 1: `NfcEntryDto.kt` 신규 작성**

`app/src/main/java/com/happynurse/data/remote/model/NfcEntryDto.kt`:
```kotlin
// NFC 진입 응답 DTO — GET /nfc/patients/entry?token=
package com.happynurse.data.remote.model

import com.google.gson.annotations.SerializedName

data class NfcEntryResponse(
    @SerializedName("patientId") val patientId: Long,
    @SerializedName("patientName") val patientName: String,
    @SerializedName("roomName") val roomName: String,
)
```

- [ ] **Step 2: `NfcTokenApi.kt` 시그니처 변경 + 인라인 DTO 제거**

`app/src/main/java/com/happynurse/data/remote/api/NfcTokenApi.kt` 전체 교체:
```kotlin
// NFC 토큰 → 환자 진입 정보 Retrofit 인터페이스 — GET /nfc/patients/entry
package com.happynurse.data.remote.api

import com.happynurse.data.remote.model.ApiResponse
import com.happynurse.data.remote.model.NfcEntryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NfcTokenApi {
    @GET("nfc/patients/entry")
    suspend fun resolveByToken(@Query("token") token: String): Response<ApiResponse<NfcEntryResponse>>
}
```

- [ ] **Step 3: 호출처 영향 확인**

Grep:
```
grep -rn "NfcTokenApi\|NfcPatientEntryResponse\|resolveByToken" app/src/main/java/com/happynurse/
```

기대: AppModule.kt 의 `provideNfcTokenApi` 만 매칭. 화면 호출 0건. (변경 시점부터 컴파일 가능 상태 유지)

- [ ] **Step 4: 빌드 검증**

```
./gradlew :app:compileDebugKotlin
```
기대: 성공. 실패 시 어떤 호출처가 옛 시그니처를 쓰고 있는지 grep 으로 추적.

---

### Task A2: `NfcModels.kt` 신규 — `NfcPatientInfo` 도메인 모델

**Files:**
- Create: `app/src/main/java/com/happynurse/domain/model/NfcModels.kt`

**근거:** CLAUDE.md 12장 — 신규 도메인 모델은 별도 파일. 두 응답(`NfcEntryResponse`, `PatientDetailDto`) 합친 통합 도메인 모델 — UI 가 한 번에 받음.

- [ ] **Step 1: `NfcModels.kt` 작성**

`app/src/main/java/com/happynurse/domain/model/NfcModels.kt`:
```kotlin
// NFC 도메인 모델 — wristband 태깅 결과 표시용
package com.happynurse.domain.model

data class NfcPatientInfo(
    val patientId: Long,
    val encounterId: Long,
    val patientName: String,
    val roomName: String?,
    val diseaseName: String?,
    val chiefComplaint: String?,
    val surgeryName: String?,
    val attendingPhysicianName: String?,
)
```

- [ ] **Step 2: 빌드 검증**

```
./gradlew :app:compileDebugKotlin
```
기대: 성공.

---

### Task A3: `NfcPatientRepository.kt` 신규

**Files:**
- Create: `app/src/main/java/com/happynurse/data/repository/NfcPatientRepository.kt`

**근거:** CLAUDE.md "기존 Repository 수정 X" — `PatientRepository` 에 메서드 추가 불가. 신규 Repository 로 NfcTokenApi + PatientApi 두 호출 합성.

- [ ] **Step 1: 작성**

`app/src/main/java/com/happynurse/data/repository/NfcPatientRepository.kt`:
```kotlin
// NFC 태깅 → 환자 정보 통합 Repository
// GET /nfc/patients/entry → patientId 확인 → GET /patient/{patientId} → encounterId + 풍부한 환자 정보
package com.happynurse.data.repository

import com.happynurse.data.remote.api.NfcTokenApi
import com.happynurse.data.remote.api.PatientApi
import com.happynurse.domain.model.NfcPatientInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcPatientRepository @Inject constructor(
    private val nfcTokenApi: NfcTokenApi,
    private val patientApi: PatientApi,
) {
    suspend fun resolveByToken(token: String): Result<NfcPatientInfo> = runCatching {
        // 1) NFC token → 환자 기본 정보
        val entryRes = nfcTokenApi.resolveByToken(token)
        val entryBody = entryRes.body()
        val entry = if (entryRes.isSuccessful && entryBody?.success == true && entryBody.data != null) {
            entryBody.data
        } else {
            throw Exception(entryBody?.message ?: "NFC 환자 조회 실패 (${entryRes.code()})")
        }

        // 2) patientId → encounterId + 풍부한 환자 정보
        val patientRes = patientApi.getPatient(entry.patientId)
        val patientBody = patientRes.body()
        val patient = if (patientRes.isSuccessful && patientBody?.success == true && patientBody.data != null) {
            patientBody.data
        } else {
            throw Exception(patientBody?.message ?: "환자 정보 조회 실패 (${patientRes.code()})")
        }

        NfcPatientInfo(
            patientId = patient.patientId,
            encounterId = patient.encounterId,
            patientName = patient.name,
            roomName = patient.roomName ?: entry.roomName,
            diseaseName = patient.diseaseName,
            chiefComplaint = patient.chiefComplaint,
            surgeryName = patient.surgeryName,
            attendingPhysicianName = patient.attendingPhysicianName,
        )
    }
}
```

- [ ] **Step 2: 빌드 검증**

```
./gradlew :app:compileDebugKotlin
```
기대: 성공.

---

### Task A4: `NfcReaderManager.parsePatientToken` 구현

**Files:**
- Modify: `app/src/main/java/com/happynurse/data/nfc/NfcReaderManager.kt:26-27`

**근거:** 현재 `parsePatientToken(tag)` 가 `TODO` 로 항상 `null` 반환. NDEF URI record 파싱 → `?token=` 쿼리 파라미터 추출 구현.

- [ ] **Step 1: import 추가**

`NfcReaderManager.kt` 의 import 영역에 다음 추가:
```kotlin
import android.net.Uri
import android.nfc.tech.Ndef
```

- [ ] **Step 2: `parsePatientToken` 본체 교체**

`NfcReaderManager.kt` 의 line 26-27 `// TODO ...` + `fun parsePatientToken(...): String? = null` 부분을 다음으로 교체:

```kotlin
/** NDEF URI 레코드 (예: https://.../nfc/redirect?token=ABC) 의 token 쿼리파라미터 추출. */
fun parsePatientToken(tag: Tag): String? {
    val ndef = Ndef.get(tag) ?: return null
    return try {
        ndef.connect()
        val msg = ndef.cachedNdefMessage ?: ndef.ndefMessage ?: return null
        val record = msg.records.firstOrNull() ?: return null
        val uri: Uri = record.toUri() ?: return null
        uri.getQueryParameter("token")
    } catch (_: Exception) {
        null
    } finally {
        runCatching { ndef.close() }
    }
}
```

- [ ] **Step 3: 빌드 검증**

```
./gradlew :app:compileDebugKotlin
```
기대: 성공. `Ndef`/`Uri` import 안 풀리면 위 import 추가 확인.

---

### Task A5: `NfcPatientViewModel.kt` 신규

**Files:**
- Create: `app/src/main/java/com/happynurse/presentation/screens/nfc/NfcPatientViewModel.kt`

**근거:** Spec 5번 — state machine + Activity 라이프사이클 메서드 노출. NfcReaderManager 도 같이 주입받아 Composable 의 DisposableEffect 가 ViewModel 만 알면 되도록.

- [ ] **Step 1: 작성**

`app/src/main/java/com/happynurse/presentation/screens/nfc/NfcPatientViewModel.kt`:
```kotlin
// NFC 환자 화면 ViewModel — wristband 태깅 → 환자 정보 로드 + reader-mode 라이프사이클 관리
package com.happynurse.presentation.screens.nfc

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happynurse.data.nfc.NfcReaderManager
import com.happynurse.data.repository.NfcPatientRepository
import com.happynurse.domain.model.NfcPatientInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NfcPatientViewModel @Inject constructor(
    private val repository: NfcPatientRepository,
    private val readerManager: NfcReaderManager,
) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data object Loading : State
        data class Success(val info: NfcPatientInfo) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun onTokenScanned(token: String) {
        if (_state.value is State.Loading) return  // 중복 태깅 방지
        viewModelScope.launch {
            _state.value = State.Loading
            repository.resolveByToken(token).fold(
                onSuccess = { _state.value = State.Success(it) },
                onFailure = { _state.value = State.Error(it.message ?: "알 수 없는 오류") },
            )
        }
    }

    fun startNfc(activity: Activity) {
        readerManager.enable(activity) { tag ->
            readerManager.parsePatientToken(tag)?.let(::onTokenScanned)
        }
    }

    fun stopNfc(activity: Activity) {
        readerManager.disable(activity)
    }

    fun reset() {
        _state.value = State.Idle
    }
}
```

- [ ] **Step 2: 빌드 검증**

```
./gradlew :app:compileDebugKotlin
```
기대: 성공.

---

### Task A6: Step A 통합 빌드 검증

**Files:** 없음 (검증만)

- [ ] **Step 1: 전체 디버그 빌드**

```
./gradlew :app:assembleDebug
```
기대: `BUILD SUCCESSFUL`.

- [ ] **Step 2: 정적 사실 확인 (수동 grep)**

```
grep -rn "NfcPatientEntryResponse" app/src/main/java/com/happynurse/
```
기대: 0건 (옛 인라인 DTO 명이 어디서도 안 쓰여야 함).

```
grep -rn "TODO" app/src/main/java/com/happynurse/data/nfc/NfcReaderManager.kt
```
기대: 0건 (parsePatientToken TODO 가 사라졌어야 함).

이 시점에서 데이터 layer 완성. Step B 진입 가능.

---

## Step B — UI Integration

### Task B1: `NavRoutes.kt` 에 라우트 인자 추가

**Files:**
- Modify: `app/src/main/java/com/happynurse/presentation/navigation/NavRoutes.kt`

**근거:** 후속 화면(`LOG_ENTRY`, `DRUG_ENTRY`) 이 `patientId`/`encounterId` 를 query 인자로 받도록 라우트 패턴 확장. helper 함수도 같이 제공해서 호출 측 코드를 깔끔하게.

- [ ] **Step 1: 전체 교체**

`NavRoutes.kt`:
```kotlin
// 네비게이션 라우트 상수 — 4탭 BottomNav + 모달 라우트
package com.happynurse.presentation.navigation

object NavRoutes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val PATIENT_DETAIL = "patient_detail/{id}"
    fun patientDetail(id: String) = "patient_detail/$id"

    const val NFC_PATIENT = "nfc_patient"

    // patientId / encounterId 인자 (NFC 흐름에서만 채워짐. 다른 진입점에선 -1L 로 호출)
    const val LOG_ENTRY = "log_entry?patientId={patientId}&encounterId={encounterId}"
    fun logEntry(patientId: Long, encounterId: Long) = "log_entry?patientId=$patientId&encounterId=$encounterId"

    const val DRUG_ENTRY = "drug_entry?patientId={patientId}&encounterId={encounterId}"
    fun drugEntry(patientId: Long, encounterId: Long) = "drug_entry?patientId=$patientId&encounterId=$encounterId"

    const val IV_TIMER_SETUP = "iv_timer_setup"
}
```

- [ ] **Step 2: 빌드 검증 — 호출처 컴파일 에러 확인**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -40
```
기대: NavGraph.kt 의 `navigate(NavRoutes.LOG_ENTRY)` 같은 옛 호출이 string mismatch 없이 통과 (string 자체는 변하지만 그대로 navigate 가능 — 단 인자 채우는 helper 호출은 후속 task 에서). 컴파일 에러 0.

---

### Task B2: `NfcReaderEffect.kt` 신규 — Composable helper

**Files:**
- Create: `app/src/main/java/com/happynurse/presentation/screens/nfc/NfcReaderEffect.kt`

**근거:** Activity 의 NFC reader-mode 라이프사이클을 `DisposableEffect` 로 자동 enable/disable. Activity 참조는 클로저 안에서만, 저장 X.

- [ ] **Step 1: 작성**

`app/src/main/java/com/happynurse/presentation/screens/nfc/NfcReaderEffect.kt`:
```kotlin
// NfcReaderEffect — Composable scope 안에서 Activity 의 NFC reader-mode 자동 enable/disable
package com.happynurse.presentation.screens.nfc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun NfcReaderEffect(viewModel: NfcPatientViewModel) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        viewModel.startNfc(activity)
        onDispose { viewModel.stopNfc(activity) }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
```

- [ ] **Step 2: 빌드 검증**

```
./gradlew :app:compileDebugKotlin
```
기대: 성공.

---

### Task B3: `DrugEntryScreen` / `LogEntryScreen` 라우트 인자 받기

**Files:**
- Modify: `app/src/main/java/com/happynurse/presentation/screens/drugentry/DrugEntryScreen.kt:47-51`
- Modify: `app/src/main/java/com/happynurse/presentation/screens/logentry/LogEntryScreen.kt:46`

**근거:** PR 1 범위는 인자 통과만. 사용은 PR 2/3. 내부 로직 변경 없음 — `Log.d` 로 도착 확인.

- [ ] **Step 1: `DrugEntryScreen` 시그니처 수정 + Log**

`DrugEntryScreen.kt` 의 `fun DrugEntryScreen(...)` (line 47-51) 부분을 다음으로 교체:
```kotlin
@Composable
fun DrugEntryScreen(
    patientId: Long,
    encounterId: Long,
    onClose: () -> Unit,
    onTimer: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(patientId, encounterId) {
        android.util.Log.d("DrugEntryScreen", "received patientId=$patientId encounterId=$encounterId")
    }
    val drugs = remember {
```

(나머지 함수 본체는 그대로 유지)

- [ ] **Step 2: `LogEntryScreen` 시그니처 수정 + Log**

`LogEntryScreen.kt` 의 `fun LogEntryScreen(onClose: () -> Unit) {` (line 46) 부분을 다음으로 교체:
```kotlin
@androidx.compose.runtime.Composable
fun LogEntryScreen(
    patientId: Long,
    encounterId: Long,
    onClose: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(patientId, encounterId) {
        android.util.Log.d("LogEntryScreen", "received patientId=$patientId encounterId=$encounterId")
    }
```

(LogEntryScreen.kt 가 Composable 어노테이션을 1줄 위에 따로 두는 패턴이면 그 어노테이션은 유지하고 시그니처만 위와 같이 바꿈. 실제 파일 구조에 맞춰 조정.)

- [ ] **Step 3: 빌드 — NavGraph.kt 컴파일 에러 예상**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```
기대: NavGraph.kt 의 `LogEntryScreen(onClose = ...)` 와 `DrugEntryScreen(onClose = ..., onTimer = ...)` 호출이 새 인자 없어 에러. → 다음 Task B5 에서 NavGraph 갱신 후 통과.

이 task 끝에는 빌드 빨간 상태가 정상. B5 까지 가야 통과.

---

### Task B4: `NfcPatientScreen.kt` ViewModel 통합

**Files:**
- Modify: `app/src/main/java/com/happynurse/presentation/screens/nfc/NfcPatientScreen.kt` (전체 재작성 수준)

**근거:** 현재 Screen 은 mockup UI. ViewModel 주입 + state 분기 + NFC reader 효과 부착.

- [ ] **Step 1: 전체 교체**

`NfcPatientScreen.kt`:
```kotlin
// NFC 인식 화면 — wristband 태깅 → 환자 정보 로드 → 다음 작업(간호일지/약물 등록) 선택
package com.happynurse.presentation.screens.nfc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.happynurse.domain.model.NfcPatientInfo
import com.happynurse.presentation.components.HnCard
import com.happynurse.presentation.theme.HnColors

@Composable
fun NfcPatientScreen(
    onClose: () -> Unit,
    onLog: (patientId: Long, encounterId: Long) -> Unit,
    onDrug: (patientId: Long, encounterId: Long) -> Unit,
    viewModel: NfcPatientViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    NfcReaderEffect(viewModel)

    Column(Modifier.fillMaxSize().background(HnColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "닫기",
                modifier = Modifier.size(28.dp).clickable(onClick = onClose),
            )
            Spacer(Modifier.size(8.dp))
            Text("NFC 인식", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            when (val s = state) {
                NfcPatientViewModel.State.Idle -> IdleCard()
                NfcPatientViewModel.State.Loading -> LoadingCard()
                is NfcPatientViewModel.State.Success -> SuccessSection(
                    info = s.info,
                    onLog = { onLog(s.info.patientId, s.info.encounterId) },
                    onDrug = { onDrug(s.info.patientId, s.info.encounterId) },
                )
                is NfcPatientViewModel.State.Error -> ErrorCard(s.message, onRetry = viewModel::reset)
            }
        }
    }
}

@Composable
private fun IdleCard() {
    HnCard(padding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp).clip(CircleShape).background(HnColors.PrimarySoft),
            ) {
                Icon(Icons.Outlined.Nfc, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("환자 손목띠를 휴대폰에 태깅해 주세요", fontSize = 13.sp, color = HnColors.TextSecondary)
        }
    }
}

@Composable
private fun LoadingCard() {
    HnCard(padding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = HnColors.Primary)
            Spacer(Modifier.height(14.dp))
            Text("환자 정보를 불러오는 중입니다", fontSize = 13.sp, color = HnColors.TextSecondary)
        }
    }
}

@Composable
private fun SuccessSection(
    info: NfcPatientInfo,
    onLog: () -> Unit,
    onDrug: () -> Unit,
) {
    HnCard(padding = 20.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp).clip(CircleShape).background(HnColors.TagPillBg),
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = HnColors.Success, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("인식 완료", fontSize = 13.sp, color = HnColors.Success, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(info.patientName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
            info.roomName?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, fontSize = 12.sp, color = HnColors.TextSecondary)
            }
            info.diseaseName?.let {
                Spacer(Modifier.height(6.dp))
                Text("병명: $it", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
            info.chiefComplaint?.let {
                Spacer(Modifier.height(2.dp))
                Text("주증상: $it", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
            info.surgeryName?.let {
                Spacer(Modifier.height(2.dp))
                Text("수술: $it", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
            info.attendingPhysicianName?.let {
                Spacer(Modifier.height(2.dp))
                Text("담당의: $it", fontSize = 12.sp, color = HnColors.TextSecondary)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("다음 작업을 선택하세요", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HnColors.TextSecondary)
    Spacer(Modifier.height(10.dp))
    ActionTile(Icons.Outlined.Mic, "간호일지 등록", "음성 녹음 → STT → 전송", onLog)
    Spacer(Modifier.height(8.dp))
    ActionTile(Icons.Outlined.MedicalServices, "약물 등록", "약물 NFC 태깅 → 리스트 → 전송", onDrug)
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    HnCard(padding = 20.dp, onClick = onRetry) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = HnColors.Danger, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(14.dp))
            Text(message, fontSize = 13.sp, color = HnColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("탭하여 다시 시도", fontSize = 12.sp, color = HnColors.Primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActionTile(icon: ImageVector, title: String, desc: String, onClick: () -> Unit) {
    HnCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(HnColors.PrimarySoft),
            ) { Icon(icon, contentDescription = null, tint = HnColors.Primary, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HnColors.Text)
                Text(desc, fontSize = 12.sp, color = HnColors.TextSecondary, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = HnColors.TextTertiary, modifier = Modifier.size(20.dp))
        }
    }
}
```

> 주의: `HnColors.Danger` 가 `Color.kt` 에 없을 가능성 있음. 컴파일 실패 시 `Color.kt` 확인 후 가장 비슷한 토큰 (예: `Error`, `Warning`) 으로 교체. 새 토큰 추가는 본 PR 범위 밖.

- [ ] **Step 2: `HnColors` 확인**

```
grep -n "val Danger\|val Error\|val Primary\|val Success" app/src/main/java/com/happynurse/presentation/theme/Color.kt
```

`Danger` 가 없으면 위 코드의 `HnColors.Danger` 를 존재하는 빨강 계열 토큰명으로 치환.

- [ ] **Step 3: 빌드 — 여전히 NavGraph 미해결로 빨간 상태 예상**

```
./gradlew :app:compileDebugKotlin 2>&1 | tail -30
```
기대: NfcPatientScreen 자체는 통과. NavGraph 의 NfcPatientScreen 호출이 새 콜백 시그니처와 안 맞아 에러. → B5 에서 통과.

---

### Task B5: `NavGraph.kt` 갱신 — 라우트 인자 전달

**Files:**
- Modify: `app/src/main/java/com/happynurse/presentation/navigation/NavGraph.kt:82-97`

**근거:** B1/B3/B4 의 변경을 NavGraph 가 수용. composable 등록부에 query 인자 정의 + Screen 호출에 인자 전달.

- [ ] **Step 1: import 추가**

`NavGraph.kt` 의 import 영역에 다음 추가 (`navArgument` 는 이미 있음):
```kotlin
// (이미 있음) import androidx.navigation.NavType
// (이미 있음) import androidx.navigation.navArgument
```

- [ ] **Step 2: NFC_PATIENT composable 블록 교체**

`NavGraph.kt:82-88` 의 `composable(NavRoutes.NFC_PATIENT) { ... }` 블록을 다음으로 교체:
```kotlin
        composable(NavRoutes.NFC_PATIENT) {
            NfcPatientScreen(
                onClose = { navController.popBackStack() },
                onLog = { patientId, encounterId ->
                    navController.navigate(NavRoutes.logEntry(patientId, encounterId))
                },
                onDrug = { patientId, encounterId ->
                    navController.navigate(NavRoutes.drugEntry(patientId, encounterId))
                },
            )
        }
```

- [ ] **Step 3: LOG_ENTRY composable 블록 교체**

`NavGraph.kt:89-91` 의 `composable(NavRoutes.LOG_ENTRY) { ... }` 블록을 다음으로 교체:
```kotlin
        composable(
            route = NavRoutes.LOG_ENTRY,
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("encounterId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val patientId = entry.arguments?.getLong("patientId") ?: -1L
            val encounterId = entry.arguments?.getLong("encounterId") ?: -1L
            LogEntryScreen(
                patientId = patientId,
                encounterId = encounterId,
                onClose = { navController.popBackStack() },
            )
        }
```

- [ ] **Step 4: DRUG_ENTRY composable 블록 교체**

`NavGraph.kt:92-97` 의 `composable(NavRoutes.DRUG_ENTRY) { ... }` 블록을 다음으로 교체:
```kotlin
        composable(
            route = NavRoutes.DRUG_ENTRY,
            arguments = listOf(
                navArgument("patientId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("encounterId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val patientId = entry.arguments?.getLong("patientId") ?: -1L
            val encounterId = entry.arguments?.getLong("encounterId") ?: -1L
            DrugEntryScreen(
                patientId = patientId,
                encounterId = encounterId,
                onClose = { navController.popBackStack() },
                onTimer = { navController.navigate(NavRoutes.IV_TIMER_SETUP) },
            )
        }
```

- [ ] **Step 5: 빌드 검증**

```
./gradlew :app:assembleDebug
```
기대: `BUILD SUCCESSFUL`. (Step A6 이후로 모든 컴파일 오류 해결)

---

### Task B6: 정적 사실 점검

- [ ] **Step 1: 변경된 콜백/라우트 일관성 grep**

```
grep -rn "onLog\|onDrug" app/src/main/java/com/happynurse/presentation/screens/nfc/ app/src/main/java/com/happynurse/presentation/navigation/
```
기대: NfcPatientScreen 의 `onLog/onDrug` 가 `(Long, Long) -> Unit` 시그니처. NavGraph 가 그 인자를 받아서 `NavRoutes.logEntry/drugEntry` 로 navigate.

```
grep -rn "patientId.*encounterId\|encounterId.*patientId" app/src/main/java/com/happynurse/presentation/screens/drugentry/ app/src/main/java/com/happynurse/presentation/screens/logentry/
```
기대: `Log.d(...)` 1줄씩 도착 확인 코드 존재.

```
grep -rn "TODO" app/src/main/java/com/happynurse/data/nfc/
```
기대: 0건.

- [ ] **Step 2: warning 확인 (선택)**

```
./gradlew :app:compileDebugKotlin --warning-mode all 2>&1 | grep -i "warning\|deprecated" | head
```
기대: 본 PR 변경 코드에서 새 deprecated/unsafe warning 없음.

---

### Task B7: 실기기 검증 체크리스트

> 빌드 산출물(`app-debug.apk`) 을 실기기에 설치 후 검증.

**사전 준비:**
1. 백엔드 dev 서버에 wristband 토큰이 시드된 환자가 있는지 확인. 없으면 백엔드 담당자에게 시드 요청 (`SELECT patient_id, name, nfc_token FROM patient WHERE nfc_token IS NOT NULL LIMIT 5;` 결과 확인).
2. 빈 NFC 칩 1개 + NDEF 라이팅 도구 (예: `NFC Tools` 앱).
3. 칩에 NDEF URI 라이팅: `https://k14e101.p.ssafy.io/dev/api/nfc/redirect?token=<유효한 hex>` (위에서 확보한 nfc_token 값 사용).

**검증:**

- [ ] **(a) 빈 상태 진입**
  - 앱 로그인 → 환자 탭 → 환자 카드 → "NFC" 진입
  - 기대: "환자 손목띠를 휴대폰에 태깅해 주세요" 안내 카드 표시.

- [ ] **(b) 정상 태깅**
  - NFC 칩을 휴대폰 뒷면에 가까이.
  - 기대: Loading → 약 300ms~1.5s 내 환자 정보 카드 (이름/병실/병명/주증상/수술/담당의 일부) 표시.
  - logcat: `tag NfcPatientViewModel` 또는 OkHttp 인터셉터 로그에서 GET `/nfc/patients/entry` 200 + GET `/patient/{id}` 200 확인.

- [ ] **(c) 후속 화면 인자 도착**
  - 환자 정보 카드 표시 후 "약물 등록" 탭.
  - 기대: DrugEntryScreen 진입. logcat 에 `D/DrugEntryScreen: received patientId=<숫자> encounterId=<숫자>`.
  - back → "간호일지 등록" 탭.
  - 기대: LogEntryScreen 진입. logcat 에 `D/LogEntryScreen: received patientId=<숫자> encounterId=<숫자>`.

- [ ] **(d) 잘못된 token 처리**
  - NFC 칩에 NDEF URI 의 token 값을 의도적으로 무효 hex 로 라이팅 (예: `?token=invalidhex123`).
  - 태깅.
  - 기대: Error 카드 표시 (NFC 환자 조회 실패 메시지). 카드 탭 → Idle 로 reset.

- [ ] **(e) 화면 이탈 시 reader-mode disable**
  - 환자 정보 표시 후 닫기(X) → MainScaffold.
  - 다시 NFC 탭 → 새로 태깅 → 정상 동작.
  - 기대: 매번 깨끗하게 enable/disable. logcat 에 NFC adapter 관련 에러 없음.

- [ ] **(f) NDEF 없는 칩 / 다른 NDEF 형식 칩**
  - NDEF 가 비어있거나 텍스트 record 만 있는 칩 태깅.
  - 기대: 무반응 (parsePatientToken null → onTokenScanned 호출 안 됨). 화면은 Idle 유지.

---

## 변경 파일 요약 (Step A + Step B)

| 변경 종류 | 파일 |
|---|---|
| Create | `app/src/main/java/com/happynurse/data/remote/model/NfcEntryDto.kt` |
| Create | `app/src/main/java/com/happynurse/domain/model/NfcModels.kt` |
| Create | `app/src/main/java/com/happynurse/data/repository/NfcPatientRepository.kt` |
| Create | `app/src/main/java/com/happynurse/presentation/screens/nfc/NfcPatientViewModel.kt` |
| Create | `app/src/main/java/com/happynurse/presentation/screens/nfc/NfcReaderEffect.kt` |
| Modify | `app/src/main/java/com/happynurse/data/nfc/NfcReaderManager.kt` |
| Modify | `app/src/main/java/com/happynurse/data/remote/api/NfcTokenApi.kt` |
| Modify | `app/src/main/java/com/happynurse/presentation/navigation/NavRoutes.kt` |
| Modify | `app/src/main/java/com/happynurse/presentation/navigation/NavGraph.kt` |
| Modify | `app/src/main/java/com/happynurse/presentation/screens/nfc/NfcPatientScreen.kt` |
| Modify | `app/src/main/java/com/happynurse/presentation/screens/drugentry/DrugEntryScreen.kt` |
| Modify | `app/src/main/java/com/happynurse/presentation/screens/logentry/LogEntryScreen.kt` |

기존 파일 변경 영향 (`docs/CLAUDE.md` 2장 회피 규칙 준수):
- `NfcTokenApi.kt`, `NfcReaderManager.kt`, `NavRoutes.kt`, `NavGraph.kt`, `NfcPatientScreen.kt`, `DrugEntryScreen.kt` — 본인 작업 영역 또는 NFC 흐름 통과를 위한 최소 시그니처 변경.
- `LogEntryScreen.kt` — 팀원 영역이지만 라우트 인자 추가만 (사용 안 함, Log 만 1줄). PR 머지 전 팀원에 한 줄 변경 알림.

---

## Out of Scope (PR 1 에 안 들어감)

- 약물 NFC 태깅 / verify / record 흐름 → PR 2
- 수액 시작 (`/iv/start`) → PR 3
- 진행 중 IV 화면 → PR 4
- `/drug/tags` 발급 화면 → 후순위
- 백엔드 wristband 시드 / 칩 라이팅 도구 (운영 책임)
- debug 빌드 NFC 시뮬레이터 (실기기 결정으로 불필요)
- 단위 테스트 (본 프로젝트 패턴상 없음)

---

## Self-Review (작성자 체크)

- [x] Spec 의 Step A 변경 파일 6개 모두 task 로 매핑 (A1: 2 files, A2: 1, A3: 1, A4: 1, A5: 1)
- [x] Spec 의 Step B 변경 파일 6개 모두 task 로 매핑 (B1: 1, B2: 1, B3: 2, B4: 1, B5: 1)
- [x] 코드 블록 모두 완전 — placeholder/TBD/TODO 없음 (Out of Scope 명시 제외)
- [x] 타입/메서드명 일관 — `NfcPatientInfo`, `resolveByToken`, `onTokenScanned`, `startNfc/stopNfc` 모두 task 들 사이 일치
- [x] 검증 정책 — 각 task 끝에 빌드 또는 grep 점검. Step B 끝에 실기기 체크리스트
- [x] 사용자 feedback 반영 — git commit 단계 없음

## Risks (작업 중 부닥칠 가능성)

- **HnColors.Danger 부재** — Task B4 Step 2 에서 확인. 없으면 다른 빨강 토큰으로 치환.
- **백엔드 dev 시드 미존재** — 실기기 검증 (b)~(d) 가 막힘. B7 사전 준비 1번에서 확인.
- **NDEF URI 형식 가정 빗나감** — 칩에 다른 형식 (예: payload 가 raw token 텍스트) 으로 적혀있으면 (b) 가 (f) 처럼 무반응. logcat 에서 `parsePatientToken` 단계 확인 후 `Ndef` → `NdefFormatable` 또는 raw text record 분기 추가.
- **`/patient/{patientId}` 권한 정책 변경** — 현재 본인 담당 무관 조회 가능. 백엔드가 담당 환자만 허용으로 바꾸면 본 흐름 401/403. 그때 옵션 1 (백엔드에 NfcEntryResponse.encounterId 추가) 로 회귀.

---

## 끝
