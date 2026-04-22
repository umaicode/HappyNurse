# Frontend CLAUDE.md

## 기술 스택

| 분류            | 기술                                           |
| --------------- | ---------------------------------------------- |
| 프레임워크      | Next.js 16 (App Router)                        |
| 언어            | TypeScript 5                                   |
| 스타일링        | Tailwind CSS 4                                 |
| 상태 관리       | Zustand                                        |
| 서버 상태 관리  | TanStack Query                                 |
| 인증            | NextAuth v4                                    |
| HTTP 클라이언트 | Axios                                          |
| UI 컴포넌트     | shadcn/ui, Radix UI                            |
| 아이콘          | Lucide React                                   |
| 유틸리티        | clsx, tailwind-merge, class-variance-authority |

---

## 폴더 구조

```text
src/
├── app/                        # Next.js App Router 페이지
│   ├── (auth)/                 # 인증 관련 라우트 그룹
│   │   ├── find-password/
│   │   │   └── page.tsx
│   │   └── login/
│   │       └── page.tsx
│   ├── (web)/                  # 인증 후 메인 라우트 그룹
│   │   ├── dashboard/
│   │   │   └── page.tsx        # 환자 미선택: 안내 화면 / 선택: 간호기록 뷰 (?id=)
│   │   └── layout.tsx
│   ├── globals.css
│   ├── layout.tsx
│   └── page.tsx
├── components/                 # 공통 컴포넌트
│   ├── common/                 # 범용 공통 컴포넌트
│   │   ├── Button.tsx
│   │   ├── Modal.tsx
│   │   ├── Spinner.tsx
│   │   └── ImageWithFallback.tsx
│   ├── layout/                 # 레이아웃 컴포넌트
│   │   └── DashboardLayout.tsx # Header.tsx, Sidebar.tsx 는 사용하지 않으므로 생성 금지
│   └── ui/                     # shadcn/ui 자동 생성 컴포넌트
│       └── button.tsx
├── features/                   # 도메인별 기능 모듈
│   ├── auth/
│   │   ├── api/
│   │   ├── components/
│   │   ├── hooks/
│   │   └── types/
│   ├── patient/
│   │   ├── api/
│   │   ├── components/
│   │   ├── hooks/
│   │   └── types/
│   └── record/
│       ├── api/
│       ├── components/
│       ├── hooks/
│       └── types/
└── lib/                        # 공통 유틸리티 및 설정
    ├── auth.ts
    ├── client.ts
    └── utils.ts
```

---

## 코딩 규칙

### 스타일링

- CSS는 Tailwind CSS 위주로 구현

### 네이밍

- **약어 사용을 금지한다.** 변수명, 함수명, 타입명, 파일명 모두 전체 단어를 사용한다.
  - 금지 예시: `usr` → `user`, `req` → `request`, `res` → `response`
  - 금지 예시: `idx` → `index`, `val` → `value`, `err` → `error`

### 서버 상태 관리 (TanStack Query)

- 서버 데이터 패칭, 캐싱, 동기화는 **TanStack Query**를 사용한다.

### 클라이언트 상태 관리 (Zustand)

- UI 상태, 전역 클라이언트 상태는 **Zustand**를 사용한다.
- 스토어는 도메인별로 분리하여 `features/{도메인}/stores/` 또는 `lib/stores/`에 위치한다.

### 재사용성 기준

- **3회 이상** 사용되거나 여러 도메인에서 공유되는 코드는 공통으로 분리한다.
- **1~2회** 사용에 그치는 코드는 해당 도메인 내에 그대로 구현한다.

### 목업 데이터

- 목업 데이터를 화면에 표시할 때 해당 **텍스트 앞에 🛠️ 아이콘을 붙여** 실제 데이터와 구분한다.
  - 예시: `'병동 선택'` → `'🛠️ 병동 선택'`
  - 예시: `'환자 목록'` → `'🛠️ 환자 목록'`

### 코드 작성 시 금지 사항

- 코드 및 주석에 이모지를 사용하지 않는다.
