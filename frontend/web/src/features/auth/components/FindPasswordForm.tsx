/**
 * 비밀번호 찾기 폼.
 * 병원코드 · 아이디 입력 후 임시 비밀번호 발급.
 * → app/(auth)/find-password/page.tsx 에서 사용.
 */
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'

export function FindPasswordForm() {
  const router = useRouter()
  const [hospitalCode, setHospitalCode] = useState('')
  const [userId, setUserId] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    // TODO: 임시 비밀번호 발급 API 연동
    router.push('/login')
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
      <button type="submit">임시 비밀번호 발급</button>
      <button type="button" onClick={() => router.push('/login')}>
        로그인으로 돌아가기
      </button>
    </form>
  )
}
