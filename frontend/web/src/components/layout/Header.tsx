/**
 * 상단 헤더.
 * 로고 · 간호사 이름 · 근무시간대 · RoleBadge.
 * → app/(web)/layout.tsx 에서 사용.
 * 하드코딩 삭제 예정
 */
import Image from 'next/image'
import { RoleBadge } from '@/features/auth/components/RoleBadge'

export function Header() {
  return (
    <header className="header flex items-center justify-between px-3 py-3 border-b bg-white">
      <div className="logo">
        <Image src="/image.png" alt="Happy Nurse" height={36} width={200} priority />
      </div>
      {/* 간호사 이름 · 근무시간대 · RoleBadge */}
      <div className="flex items-center gap-3 text-gray-600">
        <span className="text-sm">🛠️근무시간대</span>
        <span className="font-medium">홍길동</span>
        <RoleBadge role="NURSE" />
      </div>
    </header>
  )
}
