# HappyNurse 개발 가이드

## 목차
1. [프로젝트 구조](#1-프로젝트-구조)
2. [기술 스택](#2-기술-스택)
3. [폴더별 역할 및 파일 추가 가이드](#3-폴더별-역할-및-파일-추가-가이드)
4. [코드 컨벤션](#4-코드-컨벤션)
5. [작업 유형별 체크리스트](#5-작업-유형별-체크리스트)

---

## 1. 프로젝트 구조

### 전체 구성
```
HappyNurse/
└── frontend/
    ├── app/    # Android 프로젝트 (Gradle 멀티모듈)
    │   ├── app/    # 폰 앱 모듈 (간호사용 모바일)
    │   └── wear/   # 워치 앱 모듈 (갤럭시 워치 7)
    └── web/    # Next.js 프로젝트 (PC 대시보드)
```

---

### 1-1. 폰 앱 (`frontend/app/app/`)

```
app/src/main/java/com/happynurse/
│
├── HappyNurseApplication.kt       # Hilt 앱 진입점
├── MainActivity.kt                # 단일 Activity
│
├── data/
│   ├── remote/
│   │   └── api/
│   │       └── HappyNurseApi.kt   # Retrofit API 인터페이스 정의
│   └── local/                     # Room Entity, DAO (미생성 — 필요 시 추가)
│
├── di/
│   └── AppModule.kt               # Hilt 의존성 주입 모듈
│
└── presentation/
    ├── navigation/
    │   └── NavGraph.kt            # 화면 라우팅
    ├── screens/
    │   ├── login/                 # 로그인 (생체인증 포함)
    │   ├── patient/               # 환자 목록(PatientList) / 상세(PatientDetail)
    │   ├── journal/               # 내 간호 일지 (타임라인)
    │   ├── handover/              # 교대 인계
    │   ├── mypage/                # 마이페이지 (프로필, 비밀번호)
    │   ├── notification/          # 알림 설정
    │   └── nfc/                   # NFC 라이팅 (관리자용)
    └── ui/
        └── theme/                 # Color, Theme, Type
```

---

### 1-2. 워치 앱 (`frontend/app/wear/`)

```
wear/src/main/java/com/happynurse/wear/
│
├── WearApplication.kt             # Hilt 앱 진입점
├── WearMainActivity.kt            # 단일 Activity
│
├── data/
│   ├── nfc/
│   │   └── NfcManager.kt         # NFC 리딩 + AES-256 복호화
│   ├── sensor/
│   │   └── GestureDetector.kt    # 제스처 감지 (가속도계/자이로)
│   ├── audio/
│   │   └── AudioRecorder.kt      # 음성 녹음 + 노이즈 캔슬링
│   └── remote/
│       ├── WearDataClient.kt      # 폰으로 데이터 전송 (DataLayer)
│       └── WearDataListenerService.kt  # 폰에서 메시지 수신
│
├── di/
│   └── WearAppModule.kt           # Hilt 의존성 주입
│
└── presentation/
    ├── navigation/
    │   └── WearNavGraph.kt        # 워치 화면 라우팅 (WearRoute 상수 정의 포함)
    ├── screens/
    │   ├── main/                  # 워치 홈 (태깅/녹음 진입)
    │   ├── nfc/
    │   │   ├── PatientTagScreen.kt    # 환자 NFC 태깅
    │   │   ├── PatientTagViewModel.kt
    │   │   ├── MedicationTagScreen.kt # 약물 NFC 태깅
    │   │   └── MedicationTagViewModel.kt
    │   ├── recording/             # 음성 녹음 (제스처/버튼)
    │   └── notification/          # 수액·타이머 알림
    └── theme/
        └── Theme.kt               # Wear Compose 테마
```

---

### 1-3. 웹 (`frontend/web/` - Next.js App Router)

```
web/src/
├── app/                           # Next.js App Router
│   ├── (auth)/                    # 인증 라우트 그룹 (URL 미포함)
│   │   ├── login/page.tsx         # 웹 로그인 (병원코드·아이디·비밀번호·직급)
│   │   └── find-password/page.tsx # 비밀번호 찾기
│   ├── (web)/                     # 인증 후 라우트 그룹 (URL 미포함)
│   │   ├── layout.tsx             # Header + Sidebar 공통 레이아웃
│   │   └── dashboard/page.tsx     # 환자 미선택: 안내 / 선택: 간호기록 뷰 (?id=)
│   ├── globals.css
│   ├── layout.tsx                 # 루트 레이아웃 (html·body·폰트)
│   └── page.tsx                   # / → /dashboard 리다이렉트
│
├── components/
│   ├── common/                    # 범용 공통 컴포넌트 (3회+ 사용)
│   │   ├── Modal.tsx
│   │   └── Spinner.tsx
│   ├── layout/                    # 레이아웃 컴포넌트
│   │   ├── Header.tsx
│   │   └── Sidebar.tsx
│   └── ui/                        # shadcn/ui 자동 생성 컴포넌트
│       └── button.tsx
│
├── features/                      # 도메인별 기능 모듈
│   ├── auth/
│   │   ├── api/                   # login · logout · refreshToken
│   │   ├── components/            # Form · FindPasswordForm · RoleBadge
│   │   ├── hooks/                 # useAuth (세션 타임아웃 15분)
│   │   └── types/                 # UserRole · LoginRequest · AuthResponse
│   ├── patient/
│   │   ├── api/                   # getList · getDetail
│   │   ├── components/            # Card · Table · WardFilter
│   │   ├── hooks/                 # usePatient
│   │   └── types/                 # Patient · PatientDetail · PatientQuery
│   └── record/
│       ├── api/                   # getTimeline · getSTTList · transfer · applyRAG
│       ├── components/            # Timeline · STTPanel · QuickFix · LockBadge · TransferModal
│       ├── hooks/                 # useRecord · useSTT
│       └── types/                 # NursingRecord · STTRecord
│
└── lib/
    ├── auth.ts                    # NextAuth 설정 (JWT · 세션 15분)
    ├── client.ts                  # Axios 인스턴스 (토큰 인터셉터 · 401 자동 로그아웃)
    └── utils.ts                   # 공통 유틸리티 (cn 등)
```

---

## 2. 기술 스택

### 2-1. 폰 앱

| 분류 | 라이브러리 | 용도 |
|------|-----------|------|
| UI | Jetpack Compose + Material3 | 화면 구성 |
| 내비게이션 | Navigation Compose | 화면 전환 |
| 상태 관리 | ViewModel + StateFlow | UI 상태 관리 |
| 비동기 | Coroutine | 비동기 처리 |
| 네트워크 | Retrofit + Gson | REST API 통신 |
| 직렬화 | Kotlin Serialization | JSON 파싱 |
| DI | Hilt | 의존성 주입 |
| 로컬 DB | Room | 오프라인 임시 저장 |
| 동기화 | WorkManager | 네트워크 재연결 시 자동 동기화 |
| 인증 | BiometricPrompt | 지문/Face ID 로그인 |
| 알림 | FCM | 푸시 알림 (오더, 특이사항) |
| 설정 저장 | DataStore | 토큰, 사용자 설정 저장 |
| 워치 연동 | Wearable DataLayer | 폰-워치 통신 |

### 2-2. 워치 앱

| 분류 | 라이브러리 | 용도 |
|------|-----------|------|
| UI | Wear Compose | 워치 화면 구성 |
| 내비게이션 | Wear Compose Navigation | 워치 화면 전환 |
| NFC | Android NFC API | 환자·약물 태깅 |
| 음성 | MediaRecorder | 음성 녹음 |
| 센서 | SensorManager | 제스처 감지 |
| 폰 통신 | Wearable DataLayer | 음성 파일·알림 전송 |
| DI | Hilt | 의존성 주입 |
| 암호화 | AES-256 | NFC 데이터 암호화 |

### 2-3. 웹

| 분류 | 기술 | 용도 |
|------|------|------|
| 프레임워크 | Next.js 16 (App Router) | 웹 UI |
| 언어 | TypeScript 5 | 타입 안전성 |
| 스타일링 | Tailwind CSS 4 | UI 스타일 |
| 서버 상태 | TanStack Query | 데이터 패칭·캐싱·동기화 |
| 클라이언트 상태 | Zustand | 전역 UI 상태 |
| 인증 | NextAuth v4 | 세션·JWT 관리 (15분 타임아웃) |
| HTTP 클라이언트 | Axios | REST API 통신 |
| UI 컴포넌트 | shadcn/ui, Radix UI | 접근성 기반 컴포넌트 |
| 아이콘 | Lucide React | 아이콘 |
| 유틸리티 | clsx, tailwind-merge, class-variance-authority | 클래스 조합 |
| STT | 온디바이스 RAG | 고유명사 자동 치환 |

---

## 3. 폴더별 역할 및 파일 추가 가이드

### 3-1. 새 화면 추가할 때 (폰 앱)

**예시: `OrderScreen` (의사 오더 화면) 추가**

**① 화면 폴더 생성**
```
presentation/screens/order/
├── OrderScreen.kt       ← Composable UI
└── OrderViewModel.kt    ← 상태·비즈니스 로직
```

**② NavGraph.kt에 라우트 추가**
```kotlin
// NavGraph.kt
composable("order/{patientId}") { backStackEntry ->
    val patientId = backStackEntry.arguments?.getString("patientId")
    OrderScreen(navController, patientId)
}
```

**③ 이동할 화면에서 navigate 호출**
```kotlin
navController.navigate("order/$patientId")
```

---

### 3-2. API 추가할 때

**① `HappyNurseApi.kt`에 엔드포인트 추가**
```kotlin
// data/remote/api/HappyNurseApi.kt
@GET("patients/{id}/orders")
suspend fun getOrders(@Path("id") patientId: String): List<OrderDto>
```

**② 필요 시 DTO 파일 추가**
```
data/remote/dto/
└── OrderDto.kt
```

**③ Repository가 필요하면 추가**
```
data/repository/
└── OrderRepository.kt
```

**④ `AppModule.kt`에 Repository 주입 추가**
```kotlin
// di/AppModule.kt
@Provides
@Singleton
fun provideOrderRepository(api: HappyNurseApi): OrderRepository {
    return OrderRepository(api)
}
```

---

### 3-3. Room DB (로컬 저장) 추가할 때

**파일 위치**
```
data/local/
├── AppDatabase.kt          ← RoomDatabase 정의
├── entity/
│   └── MemoEntity.kt       ← 테이블 정의
└── dao/
    └── MemoDao.kt          ← 쿼리 정의
```

**`AppModule.kt`에 DB 주입 추가**
```kotlin
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.databaseBuilder(context, AppDatabase::class.java, "happynurse.db").build()
}

@Provides
fun provideMemoDao(db: AppDatabase): MemoDao = db.memoDao()
```

---

### 3-4. WorkManager 작업 추가할 때 (자동 동기화)

**파일 위치**
```
data/work/
└── SyncWorker.kt
```

**Worker 기본 구조**
```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MemoRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            repository.syncToServer()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

---

### 3-5. 워치에 새 화면 추가할 때

**① 화면 폴더 생성**
```
wear/presentation/screens/<기능명>/
├── <기능명>Screen.kt
└── <기능명>ViewModel.kt
```

**② `WearNavGraph.kt`에 라우트 추가**
```kotlin
// WearRoute에 상수 추가
const val NEW_SCREEN = "new_screen"

// NavHost 블록에 추가
composable(WearRoute.NEW_SCREEN) { NewScreen(navController) }
```

---

### 3-6. 웹에 새 페이지 추가할 때

**예시: `/reports` 페이지 추가**

**① 라우트 파일 생성**
```
src/app/(web)/reports/page.tsx
```

**② 도메인 모듈이 필요하면 `features/` 아래에 생성**
```
src/features/report/
├── api/index.ts        ← Axios 호출 함수
├── components/         ← 해당 도메인 전용 컴포넌트
├── hooks/              ← TanStack Query 훅
└── types/index.ts      ← 타입 정의
```

**③ 공통 컴포넌트는 3회 이상 재사용될 때만 `components/common/`으로 분리**

---

### 3-7. 폰-워치 DataLayer 메시지 추가할 때

**폰 → 워치 (알림 전송)**
```kotlin
// 폰 앱에서 전송
messageClient.sendMessage(nodeId, "/notification/새_경로", data)
```

**워치에서 수신 처리**
```kotlin
// WearDataListenerService.kt의 onMessageReceived에 추가
"/notification/새_경로" -> handle새기능(messageEvent.data)
```

---

## 4. 코드 컨벤션

### 4-1. 파일 네이밍

| 종류 | 네이밍 규칙 | 예시 |
|------|-----------|------|
| Screen | `<기능명>Screen.kt` | `PatientListScreen.kt` |
| ViewModel | `<기능명>ViewModel.kt` | `PatientListViewModel.kt` |
| UI 상태 | `<기능명>UiState` (sealed class) | `PatientListUiState` |
| API 인터페이스 | `<프로젝트명>Api.kt` | `HappyNurseApi.kt` |
| DTO | `<기능명>Dto.kt` | `PatientDto.kt` |
| Entity | `<기능명>Entity.kt` | `MemoEntity.kt` |
| DAO | `<기능명>Dao.kt` | `MemoDao.kt` |
| Repository | `<기능명>Repository.kt` | `PatientRepository.kt` |
| Worker | `<기능명>Worker.kt` | `SyncWorker.kt` |
| DI Module | `<범위>Module.kt` | `AppModule.kt` |

### 4-2. UiState 패턴

모든 화면은 `sealed class`로 UiState를 정의합니다.

```kotlin
sealed class PatientListUiState {
    object Loading : PatientListUiState()
    data class Success(val patients: List<Patient>) : PatientListUiState()
    data class Error(val message: String) : PatientListUiState()
}
```

### 4-3. ViewModel 패턴

```kotlin
@HiltViewModel
class PatientListViewModel @Inject constructor(
    private val repository: PatientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PatientListUiState>(PatientListUiState.Loading)
    val uiState: StateFlow<PatientListUiState> = _uiState

    init {
        loadPatients()
    }

    private fun loadPatients() {
        viewModelScope.launch {
            _uiState.value = PatientListUiState.Loading
            runCatching { repository.getPatients() }
                .onSuccess { _uiState.value = PatientListUiState.Success(it) }
                .onFailure { _uiState.value = PatientListUiState.Error(it.message ?: "") }
        }
    }
}
```

### 4-4. Screen 패턴

```kotlin
@Composable
fun PatientListScreen(
    navController: NavController,
    viewModel: PatientListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is PatientListUiState.Loading -> { /* 로딩 UI */ }
        is PatientListUiState.Success -> { /* 성공 UI */ }
        is PatientListUiState.Error -> { /* 에러 UI */ }
    }
}
```

### 4-5. NavGraph 라우트 관리

라우트 문자열은 상수로 관리합니다.

```kotlin
// 폰 앱: presentation/navigation/NavGraph.kt 상단
object Route {
    const val LOGIN = "login"
    const val PATIENT_LIST = "patient_list"
    const val PATIENT_DETAIL = "patient_detail/{patientId}"
    // ...
}

// 워치: presentation/navigation/WearNavGraph.kt
object WearRoute {
    const val MAIN = "main"
    const val PATIENT_TAG = "patient_tag"
    // ...
}
```

### 4-6. Hilt 주입 규칙

- **Activity/Fragment**: `@AndroidEntryPoint`
- **ViewModel**: `@HiltViewModel` + `@Inject constructor`
- **싱글톤 서비스**: `@Singleton` + `@Provides`
- **WorkManager Worker**: `@HiltWorker` + `@AssistedInject`

### 4-7. 코루틴 규칙

- ViewModel에서만 `viewModelScope.launch` 사용
- Repository는 `suspend fun`으로 선언
- UI에서 직접 코루틴 실행 금지

### 4-8. DataStore vs Room 사용 기준

| DataStore | Room |
|-----------|------|
| 토큰, 사용자 설정, 알림 설정 등 단순 Key-Value | 환자 정보, 메모 등 구조화된 오프라인 데이터 |

---

## 5. 작업 유형별 체크리스트

### ✅ 새 화면 추가 (폰 앱)
- [ ] `presentation/screens/<기능명>/` 폴더 생성
- [ ] `<기능명>Screen.kt` 생성
- [ ] `<기능명>ViewModel.kt` 생성 (`@HiltViewModel`)
- [ ] `NavGraph.kt`에 라우트 추가
- [ ] 필요 시 `HappyNurseApi.kt`에 API 추가
- [ ] 필요 시 `AppModule.kt`에 의존성 추가

### ✅ 새 API 연동
- [ ] `HappyNurseApi.kt`에 `@GET/@POST/@PUT/@DELETE` 추가
- [ ] `data/remote/dto/` 에 DTO 추가
- [ ] Repository 생성 또는 기존 Repository에 메서드 추가
- [ ] `AppModule.kt`에 Repository `@Provides` 추가
- [ ] ViewModel에서 Repository 주입 후 사용

### ✅ 오프라인 저장 추가
- [ ] `data/local/entity/` 에 Entity 추가 (`@Entity`)
- [ ] `data/local/dao/` 에 DAO 추가 (`@Dao`)
- [ ] `AppDatabase.kt`에 Entity 및 DAO 등록
- [ ] `AppModule.kt`에 DAO `@Provides` 추가
- [ ] `data/work/SyncWorker.kt`에 동기화 로직 추가

### ✅ 새 워치 화면 추가
- [ ] `wear/presentation/screens/<기능명>/` 폴더 생성
- [ ] `<기능명>Screen.kt` 생성 (Wear Compose 사용)
- [ ] `<기능명>ViewModel.kt` 생성
- [ ] `WearNavGraph.kt`의 `WearRoute`에 상수 추가
- [ ] `WearNavGraph.kt`의 `SwipeDismissableNavHost`에 `composable` 추가

### ✅ 폰-워치 통신 추가
- [ ] DataLayer 메시지 경로 상수 정의 (`/기능/액션`)
- [ ] 폰 앱: 전송 측 코드 작성
- [ ] `WearDataListenerService.kt`: 수신 처리 추가
- [ ] 필요 시 워치 → 폰 방향도 `WearDataClient.kt`에 추가

### ✅ 새 웹 페이지 추가
- [ ] `src/app/(web)/<페이지명>/page.tsx` 생성
- [ ] 도메인 모듈 필요 시 `src/features/<도메인>/` 생성
  - [ ] `api/index.ts` — Axios 호출 함수
  - [ ] `types/index.ts` — 타입 정의
  - [ ] `hooks/` — TanStack Query 훅
  - [ ] `components/` — 전용 컴포넌트
- [ ] 공통 컴포넌트는 3회 이상 재사용 시에만 `components/common/`으로 분리

### ✅ 푸시 알림 추가 (FCM)
- [ ] FCM 콘솔에서 알림 채널 설정
- [ ] `AndroidManifest.xml`에 FCM 서비스 등록 확인
- [ ] 알림 수신 처리 로직 추가
- [ ] `NotificationSettingViewModel.kt`에 On/Off 항목 추가

---

## 빌드 명령어

```bash
# 전체 빌드
./gradlew build

# 폰 앱만 빌드
./gradlew :app:assembleDebug

# 워치 앱만 빌드
./gradlew :wear:assembleDebug

# 워치 앱 에뮬레이터에 설치
./gradlew :wear:installDebug

# 워치 앱 실행
adb shell am start -n com.happynurse.wear/.WearMainActivity
```
