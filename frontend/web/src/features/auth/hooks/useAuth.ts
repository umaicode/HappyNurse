/**
 * 로그인 상태 · 역할 · 세션 타임아웃.
 * 15분 미사용 시 자동 로그아웃.
 */
'use client'

import { useState, useEffect, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import type { UserRole } from '../types'

const TIMEOUT_MS = 15 * 60 * 1000 // 15분

export function useAuth() {
  const router = useRouter()
  const [role, setRole] = useState<UserRole | null>(null)
  const [isLoggedIn, setIsLoggedIn] = useState(false)

  const logout = useCallback(() => {
    setIsLoggedIn(false)
    setRole(null)
    router.push('/login')
  }, [router])

  // 15분 미사용 시 자동 로그아웃
  useEffect(() => {
    const timer = setTimeout(logout, TIMEOUT_MS)
    return () => clearTimeout(timer)
  }, [logout])

  return { isLoggedIn, role, logout }
}
