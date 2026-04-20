/**
 * 병동 · 호실 드롭다운 필터.
 * URL searchParams와 연동하여 대시보드 환자 목록 필터링.
 */
'use client'

import { useRouter, useSearchParams } from 'next/navigation'

export function WardFilter() {
  const router = useRouter()
  const searchParams = useSearchParams()

  const handleWardChange = (ward: string) => {
    const params = new URLSearchParams(searchParams.toString())
    if (ward) params.set('ward', ward)
    else params.delete('ward')
    router.push(`/dashboard?${params.toString()}`)
  }

  return (
    <div className="flex gap-2 mb-4">
      {/* 병동 드롭다운 */}
      <select
        onChange={(e) => handleWardChange(e.target.value)}
        defaultValue={searchParams.get('ward') ?? ''}
      >
        <option value="">전체 병동</option>
        {/* TODO: 병동 목록 API 연동 */}
      </select>
    </div>
  )
}
