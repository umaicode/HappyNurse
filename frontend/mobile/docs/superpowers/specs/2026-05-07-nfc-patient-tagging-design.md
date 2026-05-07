# PR 1 — 환자 wristband NFC 태깅 연결 (설계)

작성일: 2026-05-07
대상 PR: PR 1 (CLAUDE.md 7장 기준)
작성자: 본인 작업 영역 (NFC 태깅 + 수액 타이머)

---

## 목적

환자 wristband NFC 칩을 태깅하여 환자 정보를 식별하고, 후속 작업 화면(약물 등록 / 간호일지 등록) 으로 `patientId` + `encounterId` 를 전달하는 흐름을 완성한다.

## 결정사항 (브레인스토밍 결과)

| 항목 | 결정 | 근거 |
|---|---|---|
| B1 (encounterId 확보) | 옵션 X — `/nfc/patients/entry` + `/patient/{patientId}` 2단계 호출 | 백엔드 변경 0. `PatientResonse` 가 이미 `encounterId` 포함. `PatientService.getPatient()` 에 본인 담당 권한 체크 없음 |
| NFC 검증 환경 | 실기기 | 에뮬레이터 우회 코드 불필요 |
| NFC 칩 데이터 형식 | NDEF URI 레코드 (`https://k14e101.p.ssafy.io/dev/api/nfc/redirect?token=<hex>`) | 환자 휴대폰 태깅 시에도 자동 브라우저 진입 가능 |
| 작업 단위 | 2단계 (Step A 데이터 layer / Step B UI 통합) | 검증 시점 명확. 한 번에 다 하면 검증 후 결함 위치 파악 어려움 |
| 기존 NfcTokenApi (wrapper 없는 컨벤션 예외) | 표준 wrapper 패턴으로 정정 | 본 작업 동안 어차피 손대므로 같이 정정 |
| Repository 분리 | 신규 `NfcPatientRepository` 신설 | CLAUDE.md "기존 Repository 수정 X" 규칙 준수 |

## 백엔드 사실 (검증 완료)

- `Patient.nfcToken` = `HmacSHA256(secret, patientId)` → 64자 hex (`NfcTokenUtil.generate()`)
- 칩 NDEF URI: `.../nfc/redirect?token=<nfcToken>`
- `GET /nfc/patients/entry?token=<hex>` (비로그인 허용) → `NfcEntryResponse { patientId, patientName, roomName }` (encounterId 없음)
- `GET /patient/{patientId}` (Bearer) → `PatientResonse { encounterId, diseaseName, chiefComplaint, surgeryName, attendingPhysicianName, ... }` — 본인 담당 권한 체크 없음
- `WebappService.getPatientEntry(token)` 내부에서 활성 `Encounter` 도 이미 조회 중 (`WebappService.java:43-55`). `NfcEntryResponse` 응답 DTO 만 `encounterId` 누락. 옵션 1 (필드 추가) 으로도 가능하나 본 PR 은 옵션 X 채택.

---

## Step A — 데이터 layer

### 파일 변경 목록

| 파일 | 신규/수정 | 핵심 변경 |
|---|---|---|
| `data/nfc/NfcReaderManager.kt` | 수정 | `parsePatientToken(tag): String?` 의 TODO 채움 |
| `data/remote/api/NfcTokenApi.kt` | 수정 | `Response<ApiResponse<NfcEntryResponse>>` 로 시그니처 변경. 인라인 DTO 제거 (별도 파일로 이동) |
| `data/remote/model/NfcEntryDto.kt` | **신규** | `NfcEntryResponse` data class. 모든 필드에 `@SerializedName` |
| `data/repository/NfcPatientRepository.kt` | **신규** | `resolveByToken(token): Result<NfcPatientInfo>` — `NfcTokenApi` + `PatientApi` 두 호출 합성 |
| `domain/model/NfcModels.kt` | **신규** | `data class NfcPatientInfo(...)` |
| `presentation/screens/nfc/NfcPatientViewModel.kt` | **신규** | `@HiltViewModel` + state machine. `NfcReaderManager` 도 같이 주입받아 Activity 라이프사이클 메서드 노출 |

### 1. `NfcReaderManager.parsePatientToken(tag)` 구현

```kotlin
fun parsePatientToken(tag: Tag): String? {
    val ndef = Ndef.get(tag) ?: return null
    return try {
        ndef.connect()
        val msg = ndef.cachedNdefMessage ?: ndef.ndefMessage ?: return null
        val record = msg.records.firstOrNull() ?: return null
        val uri = record.toUri() ?: return null
        uri.getQueryParameter("token")
    } catch (_: Exception) {
        null
    } finally {
        runCatching { ndef.close() }
    }
}
```

- `Ndef.get(tag)` 가 null 이면 NDEF 미지원 칩 → null 반환 (호출 측이 "유효하지 않은 칩" 처리)
- `record.toUri()` 가 URI record / smart-poster 모두 처리 (Android 표준 API)
- 실패는 모두 null 로 swallow — UI 가 `Idle` 유지하거나 에러 안내

### 2. `NfcTokenApi` 정정 + `NfcEntryDto.kt` 분리

기존 (`NfcTokenApi.kt`):
```kotlin
interface NfcTokenApi {
    @GET("nfc/patients/entry")
    suspend fun resolveByToken(@Query("token") token: String): NfcPatientEntryResponse
}
data class NfcPatientEntryResponse(...)  // 인라인 + @SerializedName 없음
```

변경 후:
```kotlin
// data/remote/api/NfcTokenApi.kt
interface NfcTokenApi {
    @GET("nfc/patients/entry")
    suspend fun resolveByToken(@Query("token") token: String): Response<ApiResponse<NfcEntryResponse>>
}

// data/remote/model/NfcEntryDto.kt (신규)
data class NfcEntryResponse(
    @SerializedName("patientId") val patientId: Long,
    @SerializedName("patientName") val patientName: String,
    @SerializedName("roomName") val roomName: String,
)
```

### 3. `NfcPatientRepository` 신규

```kotlin
@Singleton
class NfcPatientRepository @Inject constructor(
    private val nfcTokenApi: NfcTokenApi,
    private val patientApi: PatientApi,
) {
    suspend fun resolveByToken(token: String): Result<NfcPatientInfo> = runCatching {
        // 1) NFC token → patient 기본 정보
        val entryRes = nfcTokenApi.resolveByToken(token)
        val entryBody = entryRes.body()
        val entry = if (entryRes.isSuccessful && entryBody?.success == true && entryBody.data != null)
            entryBody.data
        else throw Exception(entryBody?.message ?: "NFC 환자 조회 실패 (${entryRes.code()})")

        // 2) patientId → encounterId + 풍부한 환자 정보
        val patientRes = patientApi.getPatient(entry.patientId)
        val patientBody = patientRes.body()
        val patient = if (patientRes.isSuccessful && patientBody?.success == true && patientBody.data != null)
            patientBody.data
        else throw Exception(patientBody?.message ?: "환자 정보 조회 실패 (${patientRes.code()})")

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

### 4. `NfcPatientInfo` 도메인 모델

```kotlin
// domain/model/NfcModels.kt
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

### 5. `NfcPatientViewModel`

```kotlin
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

    fun reset() { _state.value = State.Idle }
}
```

### Step A 검증 기준

- `./gradlew assembleDebug` 통과
- `NfcTokenApi` 시그니처 변경에 따른 기존 호출처 컴파일 에러 없는지 확인 (현재 사용처 0)
- `NfcPatientRepository.resolveByToken` 의 두-단계 호출 코드 리뷰

---

## Step B — UI 통합

### 파일 변경 목록

| 파일 | 신규/수정 | 핵심 변경 |
|---|---|---|
| `presentation/screens/nfc/NfcPatientScreen.kt` | 수정 | ViewModel 주입 + state 분기 UI + DisposableEffect 로 NFC reader-mode |
| `presentation/screens/nfc/NfcReaderEffect.kt` | **신규** | `@Composable` helper — Activity 의 NFC enable/disable 라이프사이클 관리 |
| `presentation/navigation/NavGraph.kt` | 수정 | `NfcPatientScreen` 콜백 인자 (patientId, encounterId) 처리. `LOGENTRY`/`DRUGENTRY` 라우트로 navigate 시 인자 실음 |
| `presentation/navigation/NavRoutes.kt` | 수정 | `LOGENTRY`, `DRUGENTRY` 라우트 패턴에 `?patientId={...}&encounterId={...}` 쿼리 인자 추가. helper 함수 (`logEntry(patientId, encounterId)`) 제공 |
| `presentation/screens/drugentry/DrugEntryScreen.kt` | 수정 | NavBackStackEntry 에서 인자 추출 (Long, Long). 사용은 PR 2 — 우선 변수만 받음 |
| `presentation/screens/logentry/LogEntryScreen.kt` | 수정 | 위와 동일. 단 logentry 는 팀원 작업 영역일 가능성 → 콜백 시그니처만 수정하고 안에서는 사용 안 함 (보존) |

### NFC reader-mode 라이프사이클 — ViewModel 경유

`NfcReaderManager` 는 이미 `@Singleton + @Inject` 라 ViewModel 에 주입받는 게 가장 깔끔. Activity 참조는 Composable 의 `DisposableEffect` 클로저 안에서만 임시로 사용 (저장 X — 메모리 누수 방지).

```kotlin
// NfcPatientViewModel.kt 추가 메서드
fun startNfc(activity: Activity) {
    readerManager.enable(activity) { tag ->
        readerManager.parsePatientToken(tag)?.let(::onTokenScanned)
    }
}
fun stopNfc(activity: Activity) {
    readerManager.disable(activity)
}
```

```kotlin
// presentation/screens/nfc/NfcReaderEffect.kt (신규)
@Composable
fun NfcReaderEffect(viewModel: NfcPatientViewModel) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return
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

### `NfcPatientScreen` UI 분기

```kotlin
@Composable
fun NfcPatientScreen(
    onClose: () -> Unit,
    onLog: (patientId: Long, encounterId: Long) -> Unit,
    onDrug: (patientId: Long, encounterId: Long) -> Unit,
    viewModel: NfcPatientViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    NfcReaderEffect(viewModel)

    Column(...) {
        Header(onClose)
        when (val s = state) {
            State.Idle -> IdleCard("환자 손목띠를 휴대폰에 태깅해 주세요")
            State.Loading -> LoadingCard()
            is State.Success -> {
                PatientInfoCard(s.info)  // 이름/병실/병명/주증상/수술명/담당의
                ActionTile(Icons.Outlined.Mic, "간호일지 등록", "음성 녹음 → STT → 전송",
                    onClick = { onLog(s.info.patientId, s.info.encounterId) })
                ActionTile(Icons.Outlined.MedicalServices, "약물 등록", "약물 NFC 태깅 → 리스트 → 전송",
                    onClick = { onDrug(s.info.patientId, s.info.encounterId) })
            }
            is State.Error -> ErrorCard(s.message, onRetry = viewModel::reset)
        }
    }
}
```

### Step B 검증 기준 (실기기)

- (a) NFC 칩에 NDEF URI 라이팅: `https://k14e101.p.ssafy.io/dev/api/nfc/redirect?token=<유효한 hex>` (실기기로 NFC Tools 같은 앱 활용)
- (b) 앱에서 NfcPatientScreen 진입 → "안내문구" 표시
- (c) 칩 태깅 → "Loading" 잠깐 → 환자 정보 카드 (이름/병실/병명 등) 표시
- (d) "약물 등록" 탭 → DrugEntryScreen 진입. ViewModel/Screen 안에서 `Log.d` 로 받은 `patientId`/`encounterId` 출력 확인
- (e) Back / 닫기 → NfcPatientScreen 떠남. NFC reader-mode 비활성화 확인 (재진입 후 다시 태깅 정상 동작)
- (f) 잘못된 token (없는 환자) 태깅 → Error 카드 표시 + 재시도 가능

---

## Out of Scope

- Step B 의 `LogEntryScreen` 안 로직 구현 (팀원 영역). 본 PR 은 라우트 인자 통과만 보장
- `DrugEntryScreen` 의 약물 NFC 태깅/verify/record 흐름 → PR 2
- `IVTimerSetupScreen` 진입 흐름 → PR 3
- 진행 중 IV (`IVTimerActiveScreen`) → PR 4
- `/drug/tags` 발급 화면 → 후순위
- debug 빌드의 NFC 시뮬레이션 (수동 token 입력 UI) → 실기기 사용 결정으로 불필요
- 백엔드 wristband 시드 / 칩 라이팅 도구 → 운영 책임

## 위험 요인

- 백엔드 dev 서버에 wristband 토큰이 시드되어 있지 않을 수 있음 → 실기기 검증 전에 nfcToken 이 채워진 환자가 있는지 확인 (`SELECT patient_id, name, nfc_token FROM patient WHERE nfc_token IS NOT NULL LIMIT 5;` 등 — 본인이 직접 SQL 가능하지 않으면 백엔드에 시드 요청)
- NDEF URI 형식 가설이 실제 칩과 다를 가능성 → 라이팅 시 본 spec 의 형식 (`?token=<hex>`) 으로 통일 합의
- 두 번째 호출 (`/patient/{patientId}`) 이 본인 담당 환자가 아닐 때 권한 정책 변경 시 실패 가능 — 현재는 권한 체크 없음. 만약 추후 백엔드가 본인 담당만 허용으로 바꾸면 본 흐름 깨짐 → 그때 옵션 1 (NfcEntryResponse 에 encounterId 추가) 로 회귀

---

## 끝
