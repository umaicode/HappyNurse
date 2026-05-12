/**
 * 인증 hook.
 *
 * - useAuthStore 의 user 를 읽어 isLoggedIn / roleCode 등 파생값을 노출
 * - logout(): 서버 /auth/logout 호출 → store reset → DEV 토큰 정리 → TanStack Query 캐시 클리어 → /login 이동
 * - 로그인 후 15분 경과 시 강제 세션 만료. 공용 데스크에서 자리 비움 시 자동 로그아웃이 의도라
 *   사용자 활동 추적은 하지 않음 (mousemove/keydown 으로 timer reset 하지 않는다).
 * - expiresAt 은 store 에서 노출 — 카운트다운 표시 컴포넌트가 자체 1초 tick 으로 갱신.
 * - 연장 API 미구현 — extendSession 은 현재 단계에서 no-op (UI placeholder 만).
 */
'use client'

import { useCallback, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { logout as logoutApi } from '../api'
import { SESSION_TIMEOUT_MS, useAuthStore } from '../stores/auth'
import { devTokenStorage } from '@/lib/client'

export function useAuth() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const expiresAt = useAuthStore((state) => state.expiresAt)
  const reset = useAuthStore((state) => state.reset)

  const logout = useCallback(async () => {
    try {
      await logoutApi()
    } catch {
      // 서버 호출 실패해도 클라이언트 측 정리는 진행
    }
    // queryClient.clear() 를 reset() 보다 먼저 — user reset 직후 (web)/layout 의 useQuery
    // (enabled: !user) 가 발동되어 만료된 쿠키로 getMe → refresh 도미노가 생기는 race 차단.
    queryClient.clear()
    reset()
    devTokenStorage.clear()
    router.push('/login')
  }, [reset, queryClient, router])

  useEffect(() => {
    if (!user) return
    const timer = setTimeout(logout, SESSION_TIMEOUT_MS)
    return () => clearTimeout(timer)
  }, [user, logout])

  // 연장 버튼 placeholder — 현재 단계에선 클릭해도 timer/expiresAt 변화 없음.
  // 백엔드 연장 API 추가되면 useAuthStore.refreshExpiresAt() 호출 + setTimeout 재등록 로직 추가.
  const extendSession = useCallback(() => {
    // intentionally no-op for now.
  }, [])

  return {
    user,
    roleCode: user?.roleCode ?? null,
    isLoggedIn: !!user,
    logout,
    expiresAt,
    extendSession,
  }
}
