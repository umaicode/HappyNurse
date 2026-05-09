# Frontend Web 리팩터 계획

브랜치: `refactor/frontend-cleanup`
대상: 간호사용 웹(`frontend/web/`)
PR 분할 안 함 — 커밋 단위로 나눠 한 PR 에 묶음.

커밋 메시지 컨벤션: `[FE] type: 내용` (type ∈ `feat` · `fix` · `refact` · `chore` · `remove`).

---

## 결정된 사항

| 항목 | 결정 |
| --- | --- |
| HandoverView 토큰 치환 | **이번 브랜치 제외** — 인수인계 API 연동 시 같이 처리 |
| 콜백 약어 정리 범위 | `(p)` · `(e)` 모두 풀어쓰기 (`patient` · `event`) |
| useAuth 의도 | **고정 15분 세션 타임아웃** — 변수명만 정리 (공용 데스크 사용 환경, 자리 비움 시 자동 로그아웃이 의도) |

---

## 커밋 순서 (총 8개)

회귀 위험 낮은 순부터. 각 커밋은 독립적으로 빌드/타입 통과해야 함.

### 1. `[FE] refact: SSE iv_alert 중복 invalidate 제거`

**파일**: `src/features/dashboard/hooks/useNotificationStream.ts`

**변경**: 병동 채널 effect 의 `iv_alert` 분기 제거. 개인 채널만 IV 캐시 갱신 책임.

**근거**: 본인 담당 환자 iv_alert 는 개인 채널에 도달. 한 이벤트로 두 채널이 모두 invalidate 하면 `staleTime: 10s` 통과 시 `/iv?wardId=...` 가 두 번 fetch 됨. 책임 분리 = 개인 채널은 IV 카드, 병동 채널은 알림 카운트만.

**범위**: `useEffect` 한 블록의 핸들러 내부 if 분기 한 줄 제거.

**검증**:
- 수액 종료 5분 전 알림 도착 시 IVTimerPanel 카드 갱신 정상
- 네트워크 탭 `/iv?wardId=...` 호출 1회만

---

### 2. `[FE] refact: DashboardView 자동 모달 오픈 effect 로 이동`

**파일**: `src/features/dashboard/components/DashboardView.tsx`

**변경**: render 본문의 `setHasCheckedAssignment` · `setIsAssignOpen` 호출을 `useEffect` 로 이동.

**Before** (52~61):
```tsx
if (!hasCheckedAssignment && !wardPatientsQuery.isPending && wardPatientsQuery.isSuccess) {
  setHasCheckedAssignment(true);
  if (patients.length > 0 && !patients.some((p) => p.isMyPatient)) {
    setIsAssignOpen(true);
  }
}
```

**After**:
```tsx
useEffect(() => {
  if (hasCheckedAssignment || !wardPatientsQuery.isSuccess) return;
  setHasCheckedAssignment(true);
  if (patients.length > 0 && !patients.some((patient) => patient.isMyPatient)) {
    setIsAssignOpen(true);
  }
}, [hasCheckedAssignment, wardPatientsQuery.isSuccess, patients]);
```

**근거**: render 중 다른 state 의 부수효과(setIsAssignOpen) 트리거는 `useEffect` 가 의도가 명확하고 React 19 Strict Mode 친화적.

**검증**:
- 첫 진입 + 담당 환자 0명 → 모달 자동 오픈
- 모달 닫고 환자 할당 후 다시 새로고침 → 모달 다시 열리지 않음
- 페이지 내 navigation (담당 환자 클릭 등) 으로 모달 다시 안 뜸

---

### 3. `[FE] refact: 빈 환자 배열 reference 안정화`

**파일**: `src/features/dashboard/components/DashboardView.tsx`

**변경**:
```tsx
const EMPTY_WARD_PATIENTS: readonly WardPatient[] = [];

// 컴포넌트 내부
const patients = wardPatientsQuery.data ?? EMPTY_WARD_PATIENTS;
```

**근거**: `?? []` 는 매 렌더 새 빈 배열 생성 → `selectedPatientId` `useMemo` deps 변동 → 매 렌더 재계산. 모듈 스코프 freeze 배열로 reference 고정.

**범위**: `import type { WardPatient }` 추가, 상수 1개 선언, 1줄 수정.

---

### 4. `[FE] chore: useAuth 변수명 SESSION_TIMEOUT_MS 로 정리`

**파일**: `src/features/auth/hooks/useAuth.ts`

**변경**: `IDLE_TIMEOUT_MS` → `SESSION_TIMEOUT_MS`. 주석도 "15분 미사용 시 자동 로그아웃" → "15분 후 강제 세션 만료 (공용 데스크 환경 — 자리 비움 시 자동 로그아웃)" 로.

**근거**: 현재 구현은 사용자 활동을 추적하지 않는 고정 타이머라 "idle" 표현이 오해 소지. 의도가 정확히 변수명·주석에 드러나야 함.

**범위**: 상수명 1개, 주석 1~2줄.

---

### 5. `[FE] chore: 임의 var 색상 → 토큰 클래스 치환`

**파일** (HandoverView 제외 — 인계 API 연동 시 처리):
- `src/components/layout/DashboardLayout.tsx`
- `src/features/auth/components/DevSignupModal.tsx`
- `src/features/patient/components/PatientSidebar.tsx`
- `src/features/dashboard/components/EMRGrid.tsx`
- `src/features/dashboard/components/AssignPatientModal.tsx`
- `src/features/dashboard/components/RightPanel.tsx`

**제외**:
- `src/components/ui/*` — shadcn 자동 생성, 보존
- `src/features/handover/components/HandoverView.tsx` — 다음 작업

**치환 매핑**:
```
text-[var(--color-brand-primary)]      → text-brand-primary
text-[var(--color-content-primary)]    → text-content-primary
text-[var(--color-content-muted)]      → text-content-muted
text-[var(--color-content-secondary)]  → text-content-secondary
text-[var(--color-content-tertiary)]   → text-content-tertiary
text-[var(--color-sub-primary)]        → text-sub-primary
bg-[var(--color-surface-base)]         → bg-surface-base
bg-[var(--color-surface-card)]         → bg-surface-card
bg-[var(--color-surface-hover)]        → bg-surface-hover
bg-[var(--color-brand-surface)]        → bg-brand-surface
bg-[var(--color-brand-primary)]        → bg-brand-primary
border-[var(--color-border-base)]      → border-border-base
border-[var(--color-border-subtle)]    → border-border-subtle
border-[var(--color-brand-primary)]    → border-brand-primary
```

**진행 방식**: 파일별 `Edit` 의 `replace_all`. 빌드/타입 통과 + 시각 회귀 없음을 매 파일마다 확인.

**검증**:
- `pnpm build` 성공
- 사이드바 / 모달 / EMR 헤더 / RightPanel 시각 변화 없음 (스크린샷 비교는 사용자가 수행)

---

### 6. `[FE] chore: 콜백 약어 풀어쓰기 ((p) · (e) → patient · event)`

**파일 + 위치**:
- `src/components/common/Modal.tsx:23` — `(e)` → `(event)`
- `src/features/dashboard/components/AssignPatientModal.tsx:52, 73, 84, 88, 90, 132` — `(p)` · `(e)` 다수
- `src/features/dashboard/components/DashboardView.tsx:31, 40, 58` — `(p)` 3건 (커밋 2 적용 후 라인 변동 있을 수 있음)
- `src/features/patient/components/PatientSidebar.tsx:59, 67, 73, 101` — `(acc, p)` · `(p)` · `(e)`
- `src/features/handover/components/HandoverView.tsx:181` — `(e)`

**참고**: HandoverView 의 `(e)` 는 토큰 치환과 별개로 이번 커밋에서 함께 처리 (단순 변수명 변경이라 인계 작업과 충돌 없음).

**검증**:
- `pnpm lint` 통과
- 동작 변화 없음

---

### 7. `[FE] chore: HandoverView mockup 표시에 🛠️ 접두사`

**파일**: `src/features/handover/components/HandoverView.tsx`

**변경**: mockup 데이터를 화면에 표시하는 위치(환자명·요약 텍스트 등)에 `🛠️ ` 접두사 추가. CLAUDE.md 의 "화면에 표시되는 mockup 텍스트 앞에 🛠️" 룰 적용.

**근거**: 데모/시연 시 mockup 인지 즉시 식별. 인계 API 연동 시 일괄 제거됨.

**범위**: HandoverView 안의 환자 카드 / 요약 / overview 텍스트 렌더 부분 — 토큰 치환은 안 건드리고 텍스트 prefix 만.

**검증**: `/handover` 진입 시 환자 이름·요약 앞에 🛠️ 보임.

---

### 8. `[FE] refact: NursingTab mutation hook 부모로 이동`

**파일**: `src/features/dashboard/components/NursingTab.tsx` (단일 파일, 변경량 큼)

**현재 구조**:
```
NursingTab
└── NoteRow (행마다 4개 mutation hook)
    ├── useUpdateNursingRecord
    ├── useUpdateMedicationGroup
    └── SttNoteActions / MedicationActions
        ├── useConfirmNursingNoteItem
        └── useDeleteNursingNoteItem
```

50행이면 mutation 인스턴스 약 200개 → 부모 1세트로 축소.

**목표 구조**:
```
NursingTab
├── useUpdateNursingRecord (1)
├── useUpdateMedicationGroup (1)
├── useConfirmNursingNoteItem (1)
├── useDeleteNursingNoteItem (1)
└── NoteRow (props: 콜백 + pendingItemId)
```

**진행 단계**:

1. NursingTab 본문에 mutation 4개 선언.
2. NoteRow 의 props 시그니처 확장:
   ```ts
   onUpdateStt: (id: number, request: NursingRecordUpdateRequest) => void
   onUpdateMedication: (taggingId: string, request: NursingNoteMedicationEditRequest) => void
   onConfirm: (itemId: number | string) => void
   onDelete: (itemId: number | string) => void
   pendingConfirmId: number | string | null
   pendingDeleteId: number | string | null
   pendingUpdateId: number | string | null
   ```
3. NursingTab 에서 mutation `mutate(...)` 를 콜백으로 감싸 NoteRow 에 전달.
   ```ts
   const pendingConfirmId = confirmMutation.isPending ? confirmMutation.variables ?? null : null
   ```
4. SttNoteActions / MedicationActions / NoteRow 의 mutation hook 호출 모두 제거.
5. 행 내부 isPending / disabled 판정을 props 의 pending\*Id 비교로 전환.

**회귀 검증 체크리스트**:
- [ ] STT 행 본문 수정 → 저장 → 갱신
- [ ] STT 행 시각(HH:mm) 수정 → 저장 → 갱신
- [ ] STT 행 확정 (draft → confirmed)
- [ ] STT 행 삭제
- [ ] MEDICATION 행 dosage 수정 → 저장
- [ ] MEDICATION 행 시각 수정 → 저장
- [ ] MEDICATION 행 확정
- [ ] MEDICATION 행 삭제
- [ ] 두 행 동시 확정 클릭 — 각자의 isPending 표시 정확
- [ ] mutation 진행 중 다른 행 사용 가능 (전역 lock 아님)
- [ ] 인라인 추가 폼 (`+ 새 기록`) 정상 동작
- [ ] 퀵수정 패널 정상 동작 (수정 모드 진입/후보 적용)
- [ ] 사이드바 `unconfirmedNursingCount` 뱃지 동기화 (확정/삭제 후)

**위험**: 가장 큰 회귀 위험. 마지막 커밋에 두고 충분히 테스트.

---

## 백엔드 의존 항목 (이번 브랜치 범위 외)

> 코드 수정만으로 해결 불가. 별도 트래킹.

### A. PatientSidebar N+1 — `unconfirmedNursingCount` 정합성

담당 환자마다 `useDraftNursingNotes(patient.encounterId)` 호출. 5명이면 5번 fetch.

**원인**: `WardPatient.unconfirmedNursingCount` 가 실제 `/drafts` 응답과 어긋나는 케이스가 있어 우회 호출 중 (코드 주석).

**액션**: 백엔드에 정합성 디버그 요청 → 일치 확인 후 PatientItem 의 `useDraftNursingNotes` 제거 (popover 내부 fetch 만 유지).

### B. IV 폴링 30s 제거 검토

백엔드가 IV `IN_PROGRESS` 진입/속도 변경 시점에도 SSE 발행하면 `refetchInterval: 30_000` 제거 가능. 발행 정책 합의 필요.

### C. 응답 wrapper 가드 강화

`'success' in body && 'data' in body` 가드가 약함. 백엔드가 모든 응답을 wrapper 로 감싼다는 명세 확약 받으면 유지, 일부 endpoint 가 raw 응답이면 `'errorCode' in body` 추가.

### D. HandoverView 토큰 치환 + mockup 제거

인수인계 API endpoint 합의 후 한 번에 처리. 임의 var 23건 + mockup import 4개 일괄 제거.

---

## 작업 환경

- 브랜치: `refactor/frontend-cleanup` 신규 생성 (base: `front-dev`)
- 매 커밋 후 `pnpm build` 통과 확인
- 커밋 8 (NursingTab) 이후엔 직접 브라우저에서 `/dashboard` 회귀 테스트 필수

## 작업 후

- 머지 시점에 본 파일(`REFACTOR_PLAN.md`) 삭제 또는 `docs/` 로 이동
- CLAUDE.md 의 mutation hook 구조 설명은 자연스럽게 "부모 NursingTab 에서 단일 인스턴스 운용" 으로 갱신 필요 (별도 커밋 또는 본 PR 마지막 커밋에 포함)
