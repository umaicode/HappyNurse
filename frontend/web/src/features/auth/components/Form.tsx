/**
 * 로그인 폼.
 * 병원코드 · 아이디 · 비밀번호 · 직급 선택.
 * → app/(auth)/login/page.tsx 에서 사용.
 */
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { login } from '../api'
import type { UserRole } from '../types'

export function Form() {
  const router = useRouter()
  const [hospitalCode, setHospitalCode] = useState('')
  const [userId, setUserId] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<UserRole>('NURSE')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    await login({ hospitalCode, userId, password, role })
    router.push('/dashboard')
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        value={hospitalCode}
        onChange={(e) => setHospitalCode(e.target.value)}
        placeholder="병원코드"
        required
      />
      <input
        value={userId}
        onChange={(e) => setUserId(e.target.value)}
        placeholder="아이디"
        required
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="비밀번호"
        required
      />
      <select value={role} onChange={(e) => setRole(e.target.value as UserRole)}>
        <option value="NURSE">일반간호사</option>
        <option value="HEAD_NURSE">수간호사</option>
        <option value="ADMIN">관리자</option>
      </select>
      <button type="submit">로그인</button>
      <a href="/find-password">비밀번호를 잊으셨나요?</a>
    </form>
  )
}
