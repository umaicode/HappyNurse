/**
 * 인증 hook.
 *
 * - useAuthStore 의 user 를 읽어 isLoggedIn / roleCode 등 파생값을 노출
 * - logout(): 서버 /auth/logout 호출 → store reset → DEV 토큰 정리 → TanStack Query 캐시 클리어 → /login 이동
 * - 로그인 후 15분 경과 시 강제 세션 만료. 공용 데스크에서 자리 비움 시 자동 로그아웃이 의도라
 *   사용자 활동 추적은 하지 않음 (mousemove/keydown 으로 timer reset 하지 않는다).
 */
'use client'

import { useCallback, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { logout as logoutApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { devTokenStorage } from '@/lib/client'

const SESSION_TIMEOUT_MS = 15 * 60 * 1000

export function useAuth() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const reset = useAuthStore((state) => state.reset)

  const logout = useCallback(async () => {
    try {
      await logoutApi()
    } catch {
      // 서버 호출 실패해도 클라이언트 측 정리는 진행
    }
    reset()
    devTokenStorage.clear()
    queryClient.clear()
    router.push('/login')
  }, [reset, queryClient, router])

  useEffect(() => {
    if (!user) return
    const timer = setTimeout(logout, SESSION_TIMEOUT_MS)
    return () => clearTimeout(timer)
  }, [user, logout])

  return {
    user,
    roleCode: user?.roleCode ?? null,
    isLoggedIn: !!user,
    logout,
  }
}
