/**
 * 인증 hook.
 *
 * - useAuthStore 의 user 를 읽어 isLoggedIn / roleCode 등 파생값을 노출
 * - 로그인 후 15분 경과 시 강제 세션 만료. 공용 데스크에서 자리 비움 시 자동 로그아웃이 의도라
 *   사용자 활동 추적은 하지 않음 (mousemove/keydown 으로 timer reset 하지 않는다).
 * - expiresAt 은 store 에서 노출 — 카운트다운 표시 컴포넌트가 자체 1초 tick 으로 갱신.
 * - 자동 로그아웃 타이머는 expiresAt 잔여 시간 기준으로 setTimeout 등록 — 새로고침 후 sessionStorage
 *   hydration / 사용자 명시 연장(refreshExpiresAt) 모두 동일 effect 가 반응. 고정 SESSION_TIMEOUT_MS
 *   setTimeout 은 새로고침으로 idle 을 우회하거나 연장 후에도 원래 시점에 firing 하던 회귀가 있어 제거.
 * - logout 함수는 useLogout 으로 분리 — 사이드바 버튼과 setTimeout 콜백이 동일 함수 공유.
 * - extendSession 호출은 PatientSidebar 가 features/auth/api 의 함수 + store.refreshExpiresAt 을 직접
 *   사용 — useAuth 가 따로 노출하지 않는다.
 */
'use client'

import { useEffect } from 'react'
import { useAuthStore } from '../stores/auth'
import { useLogout } from './useLogout'

export function useAuth() {
  const user = useAuthStore((state) => state.user)
  const expiresAt = useAuthStore((state) => state.expiresAt)
  const logout = useLogout()

  useEffect(() => {
    if (!user || expiresAt === null) return
    const remainingMs = expiresAt - Date.now()
    if (remainingMs <= 0) {
      logout()
      return
    }
    const timer = setTimeout(logout, remainingMs)
    return () => clearTimeout(timer)
  }, [user, expiresAt, logout])

  return {
    user,
    roleCode: user?.roleCode ?? null,
    isLoggedIn: !!user,
    expiresAt,
  }
}
