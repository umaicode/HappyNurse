/**
 * 인증 세션 store (Zustand).
 *
 * - persist 미사용: localStorage 노출 회피, 새로고침 시엔 (web)/layout.tsx 의 /auth/refresh 로 복원
 * - user 가 null 이면 비로그인으로 간주
 */
import { create } from 'zustand'
import type { AuthUser } from '../types'

interface AuthState {
  user: AuthUser | null
  // 한 번이라도 setUser 가 호출된 적 있으면 true. reset() 으론 false 로 돌아가지 않는다.
  // (web)/layout 의 useQuery 가 자동 로그아웃 직후 user falsy 만 보고 재발동하는 race 차단용.
  hasInitialized: boolean
  setUser: (user: AuthUser) => void
  reset: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  hasInitialized: false,
  setUser: (user) => set({ user, hasInitialized: true }),
  reset: () => set({ user: null }),
}))
