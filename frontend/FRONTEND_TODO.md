# HappyNurse 프론트 작업 분담 (UI 퍼블리싱 후)

UI 퍼블리싱은 끝난 상태로 보고, 이후 API 연동 / 로직 채우기 / 정리 작업을 분담한다.

## 역할 요약

| 영역       | 팀원                                                                       | 나                                                       |
| ---------- | -------------------------------------------------------------------------- | -------------------------------------------------------- |
| Web        | 환자용 (`/patient/*`)                                                      | 간호사용 전체 (`/(auth)/login`, `/(web)/*`)              |
| Mobile App | 조회(로그인, 환자 목록·상세, 인계, 일지, 의사 오더, 알림 설정, 마이페이지) | NFC 태깅(환자/약물/수액), 수액 타이머, FCM, 폰-워치 통신 |
| Wear OS    | 알림 목록, 음성 녹음(STT/타이머), 제스쳐, 환자 요구사항 알림               | 수액 알림                                                |

---

## 폴더 구조

````

### `frontend/mobile/app/` (Android)

```text
java/com/happynurse/
├── HappyNurseApplication.kt                       # 공통
├── MainActivity.kt                                # 공통
├── NfcEntryActivity.kt                            # 나 — App Links 진입점
├── di/AppModule.kt                                # 공통
├── data/
│   ├── nfc/NfcReaderManager.kt                    # 나
│   ├── remote/
│   │   ├── api/HappyNurseApi.kt                   # 공통 (도메인별로 분할 가능)
│   │   ├── api/NfcTokenApi.kt                     # 나
│   │   ├── api/FcmTokenApi.kt                     # 나
│   │   └── fcm/                                   # 나 — FCM 서비스/토큰/채널
│   └── wearable/                                  # 나 — 폰-워치 메시징
└── presentation/
    ├── components/                                # 공통 — BottomNavBar/JournalTimelineItem/OrderCard/PatientTabBar/TagChip
    ├── navigation/NavGraph.kt                     # 공통
    ├── ui/theme/                                  # 공통
    └── screens/
        ├── login/                                 # 팀원 — Login{Screen,ViewModel}
        ├── patient/                               # 팀원 — PatientList/Detail {Screen,ViewModel}
        ├── handover/                              # 팀원 — Handover{Screen,ViewModel}
        ├── journal/                               # 팀원 — Journal{Screen,ViewModel}
        ├── order/DoctorOrderScreen.kt             # 팀원
        ├── notification/                          # 팀원 — NotificationSetting{Screen,ViewModel}
        ├── mypage/MyPageScreen.kt                 # 팀원
        └── nfc/                                   # 나 — NfcEntry/NfcWrite
````

### `frontend/mobile/wear/` (Wear OS)

```text
java/com/happynurse/wear/
├── WearApplication.kt                             # 공통
├── WearMainActivity.kt                            # 공통
├── di/WearAppModule.kt                            # 공통
├── data/
│   ├── audio/AudioRecorder.kt                     # 팀원 — 타이머 STT 용 raw audio 녹음 (서버 업로드 전제)
│   ├── timer/CountDownTimerManager.kt             # 팀원 — 워치 카운트다운
│   ├── sensor/GestureDetector.kt                  # 팀원 — 제스쳐 트리거
│   ├── notification/                              # 공통 — WearNotification(+Payload), WearEventBus
│   ├── haptic/HapticFeedback.kt                   # 공통 (수액/타이머/환자 알림 모두 사용)
│   └── remote/                                    # 공통 — WearDataClient/Listener/MessagePaths
└── presentation/
    ├── navigation/WearNavGraph.kt                 # 공통 — MAIN 단일 라우트
    ├── theme/Theme.kt                             # 공통
    └── screens/
        ├── main/                                  # 공통 — MainScreen(HorizontalPager), MainViewModel, NotificationListPage(3탭)
        └── recording/                             # 팀원 — 타이머 STT 녹음 (raw audio 폰 송신, HorizontalPager 의 1번 페이지)
```

---

## 기술 스택 (라이브러리 버전 포함)

### Mobile (Android, `app/`)

| 분류                     | 라이브러리                                                               | 버전                           |
| ------------------------ | ------------------------------------------------------------------------ | ------------------------------ |
| Kotlin / KSP             | kotlin / ksp                                                             | 2.1.21 / 2.1.21-2.0.1          |
| AGP                      | com.android.application                                                  | 9.1.1                          |
| SDK                      | compileSdk / minSdk / targetSdk                                          | 35 / 26 / 36                   |
| Java                     | source/target                                                            | VERSION_11                     |
| Compose                  | Compose BOM                                                              | 2026.02.01                     |
| Compose                  | androidx.compose.material3                                               | (BOM 관리)                     |
| Compose                  | material-icons-extended                                                  | (BOM 관리)                     |
| Activity                 | androidx.activity.activity-compose                                       | 1.8.0                          |
| Lifecycle                | lifecycle-runtime-ktx                                                    | 2.6.1                          |
| Lifecycle                | lifecycle-viewmodel-compose                                              | 2.6.2                          |
| Navigation               | navigation-compose                                                       | 2.7.5                          |
| DI                       | Hilt / hilt-navigation-compose                                           | 2.59.2 / 1.2.0                 |
| 비동기                   | kotlinx-coroutines-android                                               | 1.7.3                          |
| 네트워크                 | retrofit / converter-gson                                                | 2.9.0 / 2.9.0                  |
| 직렬화                   | kotlinx-serialization-json                                               | 1.6.0                          |
| FCM                      | firebase-messaging                                                       | 23.3.1                         |
| Google Services 플러그인 | google-services                                                          | 4.4.2                          |
| 저장소                   | datastore-preferences                                                    | 1.0.0                          |
| 보안                     | androidx.biometric                                                       | 1.1.0                          |
| 워치 통신                | play-services-wearable                                                   | 18.1.0                         |
| Core                     | androidx.core-ktx                                                        | 1.10.1                         |
| 테스트                   | junit / androidx.test.ext.junit / espresso-core / compose-ui-test-junit4 | 4.13.2 / 1.1.5 / 3.5.1 / (BOM) |

### Wear (`wear/`)

| 분류          | 라이브러리                                         | 버전                  |
| ------------- | -------------------------------------------------- | --------------------- |
| Kotlin / KSP  | kotlin / ksp                                       | 2.1.21 / 2.1.21-2.0.1 |
| AGP           | com.android.application                            | 9.1.1                 |
| SDK           | compileSdk / minSdk / targetSdk                    | 35 / 31 / 35          |
| Java          | source/target                                      | VERSION_11            |
| Compose       | Compose BOM                                        | 2026.02.01            |
| Wear Compose  | wear.compose-material / foundation / navigation    | 1.4.0 / 1.4.0 / 1.4.0 |
| Wear Core     | androidx.wear:wear                                 | 1.3.0                 |
| Wear 인터랙션 | wear-remote-interactions / wear-phone-interactions | 1.1.0 / 1.1.0         |
| 워치 통신     | play-services-wearable                             | 18.1.0                |
| DI            | Hilt / hilt-navigation-compose                     | 2.59.2 / 1.2.0        |
| 비동기        | kotlinx-coroutines-android / play-services         | 1.7.3 / 1.7.3         |
| 직렬화        | kotlinx-serialization-json                         | 1.6.0                 |
| Lifecycle     | lifecycle-viewmodel-compose                        | 2.6.2                 |

### Mobile 빌드 BASE_URL

- debug: `https://k14e101.p.ssafy.io/dev/api/`
- release: `https://k14e101.p.ssafy.io/api/`
- 에뮬레이터: `AppModule`이 자동으로 `http://10.0.2.2:8080/` override

---

## UI 퍼블리싱 후 — 팀원이 해야 할 것

### Mobile App (조회 흐름)

- `presentation/screens/login/Login{Screen,ViewModel}.kt` — 토큰 저장 / 자동 로그인
- `presentation/screens/patient/PatientList{Screen,ViewModel}.kt`
- `presentation/screens/patient/PatientDetail{Screen,ViewModel}.kt`
- `presentation/screens/handover/Handover{Screen,ViewModel}.kt`
- `presentation/screens/journal/Journal{Screen,ViewModel}.kt`
- `presentation/screens/order/DoctorOrderScreen.kt`
- `presentation/screens/notification/NotificationSetting{Screen,ViewModel}.kt`
- `presentation/screens/mypage/MyPageScreen.kt`
- 위 화면들이 호출하는 `data/remote/api/HappyNurseApi.kt` 엔드포인트 분할 / 추가

### Wear OS

- `presentation/screens/main/Main{Screen,ViewModel}.kt` — 알림 목록
- `presentation/screens/recording/Recording{Screen,ViewModel}.kt` — 음성 녹음
- `presentation/screens/notification/Notification{Screen,ViewModel}.kt` — 환자 호출 / 환자 웹앱 요구사항 등록 알림 (수액 알림 분기는 내가 처리)
- `data/audio/AudioRecorder.kt`
- `data/stt/SpeechRecognizerWrapper.kt`
- `data/timer/CountDownTimerManager.kt` — 녹음 카운트다운
- `data/sensor/GestureDetector.kt` — 제스쳐
- 환자 웹앱 요구사항 알림 수신: 백엔드 → FCM(폰) → Wearable Data Layer(워치) 흐름 검증

---

## UI 퍼블리싱 후 — 내가 해야 할 것

### Mobile App (NFC 태깅 + 수액 타이머 + 알림)

- `NfcEntryActivity.kt` — App Links 진입, NFC NDEF 직접 진입 모두 처리
- `presentation/screens/nfc/NfcEntry{Screen,ViewModel}.kt`
- `presentation/screens/nfc/NfcWriteScreen.kt` — 환자/약물/수액 NFC 쓰기
- `data/nfc/NfcReaderManager.kt`
- `data/remote/api/NfcTokenApi.kt`
- 수액 타이머 화면 — **신규 작성 필요** (현재 모바일 측에 화면 없음, 워치 수액 알림과 연동되는 폰 측 카운트다운/리마인더)
- `data/wearable/{PhoneDataClient,PhoneDataListenerService,WearableMessagePaths}.kt` — 수액/태깅 데이터 송수신
- FCM 일체:
  - `data/remote/fcm/HappyNurseFirebaseMessagingService.kt`
  - `data/remote/fcm/FcmTokenRegistrar.kt`
  - `data/remote/fcm/NotificationChannels.kt`
  - `data/remote/api/FcmTokenApi.kt`
- `app/build.gradle.kts` 의 BASE_URL / google-services 플러그인 — release keystore 추가 시 `INFRA_REQUEST_ASSETLINKS.md` 갱신해서 인프라에 SHA-256 추가 요청
- `frontend/fcm-mobile-integration.md` 검토 후 정리

### Wear (수액 알림)

- `presentation/screens/notification/` 의 수액 알림 분기 — 환자 호출/요구사항 알림(팀원)과 같은 화면을 공유한다면 데이터 모델/네비게이션만 분리
- `data/haptic/HapticFeedback.kt` — 수액 알림 햅틱 패턴
- `data/remote/{WearDataClient,WearDataListenerService,WearableMessagePaths}.kt` — 폰-워치 통신 (메시지 path 협의 필요)

---

## 정리 / 클린업 항목 (공통)

### 정리 완료 (다음 커밋에 포함)

- 워치 NFC 일체 제거 — 워치는 NFC 하드웨어 미보유, 폰이 NFC 처리 후 워치에 결과 표시도 안 하는 정책으로 확정. 아래 항목 모두 삭제 / 수정 완료:
  - 삭제: `wear/.../data/nfc/NfcManager.kt`
  - 삭제: `wear/.../presentation/screens/nfc/{Patient,Medication}Tag{Screen,ViewModel}.kt` (디렉토리째)
  - 수정: `wear/.../presentation/navigation/WearNavGraph.kt` — `PATIENT_TAG`, `MEDICATION_TAG` 라우트 제거
  - 수정: `wear/.../data/remote/WearableMessagePaths.kt` & `app/.../data/wearable/WearableMessagePaths.kt` — `NFC_PATIENT_RESULT`, `NFC_MEDICATION_RESULT`, `MODE_MEDICATION` 제거 (양쪽 단일 출처 동기화)
  - 수정: `wear/.../presentation/screens/main/MainScreen.kt` — "환자 태깅", "약물 태깅" 칩 2개 제거 (음성 녹음 칩만 남음)
  - 수정: `wear/.../presentation/screens/main/MainViewModel.kt` — 호출처 사라진 `onMedicationModeSelected()` 제거
  - 수정: `wear/.../data/remote/WearDataClient.kt` — 호출처 사라진 `sendModeMedication()` 제거
  - 수정: `app/.../data/wearable/PhoneDataListenerService.kt` — `MODE_MEDICATION` 분기 + `handleModeMedication()` 제거
- Android Studio 자동 생성 더미 테스트 제거:
  - 삭제: `app/src/test/java/com/happynurse/ExampleUnitTest.kt`
  - 삭제: `app/src/androidTest/java/com/happynurse/ExampleInstrumentedTest.kt`
- 워치 STT 흐름 재정렬 — STT 처리 = 서버. 폰=STT 간호기록(워치 미관여), 워치=STT 타이머(raw audio 폰 송신 → 서버 STT → 폰이 워치에 시간 회신):
  - 삭제: `wear/.../data/stt/SpeechRecognizerWrapper.kt` (디렉토리째) — 워치 on-device STT 폐기
  - 삭제: `wear/.../presentation/screens/main/MainViewModel.kt` — STT 모드 통지 메서드 외 보유 상태 없음, 홈 3탭 재설계 시 신규 작성
  - 수정: 양쪽 `WearableMessagePaths.kt` — `MODE_STT`, `MODE_STT_TIMER`, `STT_RESULT`, `PATIENT_INFO`, `MEDICATION_PROMPT`, `SAVE_COMPLETE`, `NOTIFICATION_FORWARD` 제거. `AUDIO_TIMER` 신규(`/audio/timer`). `IV_ALERT`/`TIMER_ALARM`/`SESSION_LOGOUT` 상수화 (기존 하드코딩 정리). `TIMER_START` 유지(폰→워치 시간 회신용)
  - 수정: `wear/.../data/remote/WearDataClient.kt` — 도메인 메서드(`sendModeStt`, `sendModeSttTimer`, `sendSttResult`) 제거하고 generic `send(path, payload)` 만 노출
  - 수정: `wear/.../recording/RecordingViewModel.kt` — UI 상태 `Processing` → `Uploading`. raw audio 송신 path 하드코딩 → `WearableMessagePaths.AUDIO_TIMER`
  - 수정: `wear/.../recording/RecordingScreen.kt` — `Processing` → `Uploading` 상태 라벨
  - 수정: `wear/.../data/remote/WearDataListenerService.kt` — 하드코딩 path 상수화 + `TIMER_START` 분기 신규(서버 STT 응답 millis 수신 → CountDownTimerManager 시작 자리)
  - 수정: `app/.../data/wearable/PhoneDataListenerService.kt` — STT 관련 분기 전부 제거하고 `AUDIO_TIMER` 분기 신규(서버 업로드 + 응답 millis 를 `TIMER_START` 로 워치에 회신)
  - 수정: `wear/.../presentation/screens/main/MainScreen.kt` — `MainViewModel` 의존 제거, "음성 녹음" 칩만 남김(홈 재설계 전 임시)

### 워치 홈 화면 재설계 (완료)

- 메인 페이지에 3탭(수액/타이머/환자알림) + 옆 스와이프 시 녹음 페이지로 전환 — 구조 적용 완료. 다음 항목들이 정리됨:
  - 신규: `wear/.../data/notification/{WearNotification,WearNotificationPayload,WearEventBus}.kt` — 데이터 모델 + 직렬화 페이로드 + Service↔ViewModel SharedFlow 브리지(`tryEmit` 비차단)
  - 신규: `wear/.../presentation/screens/main/MainViewModel.kt` — 탭 상태 + 카테고리별 알림 리스트, `WearEventBus.notifications` collect
  - 신규: `wear/.../presentation/screens/main/NotificationListPage.kt` — 상단 3탭(수액/타이머/환자) + `ScalingLazyColumn` 알림 리스트
  - 수정: `wear/.../presentation/screens/main/MainScreen.kt` — `androidx.compose.foundation.pager.HorizontalPager` 로 재작성 (page 0 = NotificationListPage, page 1 = RecordingScreen)
  - 수정: `wear/.../presentation/navigation/WearNavGraph.kt` — `RECORDING`, `NOTIFICATION` 라우트 제거하고 MAIN 단일 라우트로 통합
  - 삭제: `wear/.../presentation/screens/notification/{NotificationScreen,NotificationViewModel}.kt` (디렉토리째)
  - 수정: `wear/.../presentation/screens/recording/RecordingScreen.kt` — `NavHostController` 의존 제거 (스와이프로만 페이지 전환)
  - 수정: 양쪽 `WearableMessagePaths.kt` — `PATIENT_CALL = "/notification/patient_call"` 추가
  - 수정: `wear/.../data/remote/WearDataListenerService.kt` — `@Inject WearEventBus` + IV/TIMER/PATIENT 알림은 `WearNotificationPayload` JSON 디코드 후 `emitNotification`, `TIMER_START` 는 8바이트 Long → `emitTimerStart`

### 미연결 (백엔드/통신 통합 단계)

- `app/.../data/wearable/PhoneDataListenerService.kt:handleAudioTimer` — 서버 STT 업로드 + 응답 millis 를 `TIMER_START` 로 워치 회신
- 폰 측 알림 전송 — FCM 수신 시 `WearNotificationPayload` 직렬화 → `IV_ALERT`/`TIMER_ALARM`/`PATIENT_CALL` path 로 워치에 전송 (현재 폰에 송신 코드 없음)
- 워치 `WearDataListenerService.handleTimerStart` 후속 — `emitTimerStart` 만 했을 뿐, `CountDownTimerManager.start` 호출 + 타이머 UI/햅틱 연동은 ViewModel/Composable 측에서 `eventBus.timerStart` collect 하는 코드 추가 필요
- `WearAppModule.kt` 정리 검토 — `GestureDetector`/`AudioRecorder`/`WearDataClient` 가 모두 `@Singleton @Inject constructor` 라 모듈의 `@Provides` 메서드들이 redundant (지금은 무해)

### API 연동 완료 후 삭제 대상

- 모바일/워치 ViewModel 안 더미 데이터/`TODO` 마커들 — 다음 파일에 `TODO`/임시 코드가 남아있음:
  - `app/.../data/nfc/NfcReaderManager.kt`
  - `app/.../data/remote/fcm/FcmTokenRegistrar.kt`
  - `app/.../data/wearable/PhoneDataListenerService.kt`
  - `app/.../presentation/screens/{handover,journal,login,nfc,notification,patient,mypage}/...`
  - `wear/.../data/{audio,remote,sensor}/...`
  - `wear/.../presentation/screens/recording/...`
  - `app/.../data/wearable/PhoneDataListenerService.kt:handleAudioTimer` — 서버 STT 업로드 + 응답 millis 를 `TIMER_START` 로 워치 회신
  - `wear/.../data/remote/WearDataListenerService.kt:handleTimerStart` — 수신한 millis 로 `CountDownTimerManager.start` 호출 (브리지 필요)

### 인증 / 라우팅 (CLAUDE.md 명시 이슈)

- `proxy.ts` 의 matcher: `['/(web)/:path*']` → 실제 URL에 `(web)` 그룹이 안 붙음. `['/dashboard/:path*', '/handover/:path*']` 로 수정
- `proxy.ts`(쿠키) ↔ `lib/client.ts`(localStorage) ↔ `lib/auth.ts`(NextAuth) 토큰 저장 위치 통일
- `lib/auth.ts` 의 `authorize` TODO 채우기 (현재 null 반환 = 로그인 불가)
