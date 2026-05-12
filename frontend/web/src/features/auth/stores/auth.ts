/**
 * 인증 세션 store (Zustand).
 *
 * - persist 미사용: localStorage 노출 회피, 새로고침 시엔 (web)/layout.tsx 의 /auth/refresh 로 복원
 * - user 가 null 이면 비로그인으로 간주
 * - expiresAt: setUser 호출 시점 + 15분. 카운트다운 표시 + 자동 로그아웃 타이머 공용. 화면 갱신용 tick 은
 *   표시 컴포넌트가 자체 setInterval 로 관리 (1초마다 store re-render 막기 위함).
 */
import { create } from 'zustand'
import type { AuthUser } from '../types'

export const SESSION_TIMEOUT_MS = 15 * 60 * 1000

interface AuthState {
  user: AuthUser | null
  // 한 번이라도 setUser 가 호출된 적 있으면 true. reset() 으론 false 로 돌아가지 않는다.
  // (web)/layout 의 useQuery 가 자동 로그아웃 직후 user falsy 만 보고 재발동하는 race 차단용.
  hasInitialized: boolean
  expiresAt: number | null
  setUser: (user: AuthUser) => void
  // 연장 API 추가되면 호출 측에서 사용. 현재 단계에선 사용처 없음 (placeholder 버튼 클릭 시 호출 안 함).
  refreshExpiresAt: () => void
  reset: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  hasInitialized: false,
  expiresAt: null,
  setUser: (user) =>
    set({
      user,
      hasInitialized: true,
      expiresAt: Date.now() + SESSION_TIMEOUT_MS,
    }),
  refreshExpiresAt: () => set({ expiresAt: Date.now() + SESSION_TIMEOUT_MS }),
  reset: () => set({ user: null, expiresAt: null }),
}))
