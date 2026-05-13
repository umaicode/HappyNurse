/**
 * 로그아웃 hook.
 *
 * - 서버 /auth/logout 호출 (실패해도 클라이언트 정리는 진행)
 * - TanStack Query 캐시 클리어 → store reset → DEV 토큰 정리 → /login 이동
 * - queryClient.clear() 를 reset() 보다 먼저 — user reset 직후 (web)/layout 의 useQuery
 *   (enabled: !user) 가 발동되어 만료된 쿠키로 getMe → refresh 도미노가 생기는 race 차단.
 *
 * useAuth 의 idle 자동 로그아웃 setTimeout 콜백과 사이드바 로그아웃 버튼이 동일 함수를 공유한다.
 */
'use client'

import { useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { logout as logoutApi } from '../api'
import { useAuthStore } from '../stores/auth'
import { devTokenStorage } from '@/lib/client'

export function useLogout() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const reset = useAuthStore((state) => state.reset)

  return useCallback(async () => {
    try {
      await logoutApi()
    } catch {
      // 서버 호출 실패해도 클라이언트 측 정리는 진행
    }
    queryClient.clear()
    reset()
    devTokenStorage.clear()
    router.push('/login')
  }, [reset, queryClient, router])
}
