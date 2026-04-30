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
  setUser: (user: AuthUser) => void
  reset: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  setUser: (user) => set({ user }),
  reset: () => set({ user: null }),
}))
