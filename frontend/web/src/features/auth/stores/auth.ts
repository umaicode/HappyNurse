/**
 * 인증 세션 store (Zustand).
 *
 * - `expiresAt` 만 sessionStorage 에 persist — 새로고침 시 카운트다운이 처음부터(15분) 다시 시작되어
 *   idle 시간을 우회 연장하던 문제 fix. user 자체는 persist 안 함 (보안 — 토큰/사용자 정보는 메모리만,
 *   새로고침 시엔 (web)/layout.tsx 의 /practitioners/me 호출로 복원).
 * - user 가 null 이면 비로그인으로 간주.
 * - expiresAt 의미: setUser 호출 시점 + 15분 (단, 기존 expiresAt 이 미래면 보존 — 새로고침 직후 복원 케이스).
 *   카운트다운 표시 + 자동 로그아웃 타이머 공용. 화면 갱신용 tick 은 표시 컴포넌트가 자체 setInterval 로 관리.
 */
import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'
import type { AuthUser } from '../types'

export const SESSION_TIMEOUT_MS = 15 * 60 * 1000

interface AuthState {
  user: AuthUser | null
  // 한 번이라도 setUser 가 호출된 적 있으면 true. reset() 으론 false 로 돌아가지 않는다.
  // (web)/layout 의 useQuery 가 자동 로그아웃 직후 user falsy 만 보고 재발동하는 race 차단용.
  hasInitialized: boolean
  expiresAt: number | null
  setUser: (user: AuthUser) => void
  // 사용자 명시 연장(/auth/extend) 또는 기타 연장 트리거에서 호출.
  refreshExpiresAt: () => void
  reset: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      hasInitialized: false,
      expiresAt: null,
      setUser: (user) =>
        set((state) => ({
          user,
          hasInitialized: true,
          // 기존 expiresAt 이 미래면 유지 (sessionStorage 에서 hydrate 된 값 보존).
          // null 또는 과거면 새로 계산 — 첫 로그인 / 세션 만료 후 재로그인 케이스.
          expiresAt:
            state.expiresAt !== null && state.expiresAt > Date.now()
              ? state.expiresAt
              : Date.now() + SESSION_TIMEOUT_MS,
        })),
      refreshExpiresAt: () => set({ expiresAt: Date.now() + SESSION_TIMEOUT_MS }),
      reset: () => set({ user: null, expiresAt: null }),
    }),
    {
      name: 'happy-nurse-auth',
      // 탭 닫으면 만료시각도 사라짐 (localStorage 대신 sessionStorage). 다중 탭에서도 각 탭이 독립.
      storage: createJSONStorage(() => sessionStorage),
      // expiresAt 만 보존 — user/hasInitialized 는 새 탭/새로고침마다 (web)/layout 의 getMe 로 새로 복원.
      partialize: (state) => ({ expiresAt: state.expiresAt }),
    },
  ),
)
