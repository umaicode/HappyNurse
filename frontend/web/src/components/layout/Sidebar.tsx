/**
 * 좌측 사이드바.
 * 병동 → 호실 → 환자 목록 선택.
 * 환자 클릭 시 /dashboard?id=[id] 이동.
 * → app/(web)/layout.tsx 에서 사용.
 * 현재 목업 사용중 추후 삭제 예정
 */
'use client'

import { useRouter, usePathname, useSearchParams } from 'next/navigation'
import { useState } from 'react'
import { usePatient } from '@/features/patient/hooks/usePatient'

const WARDS = ['내과3병동', '외과1병동', 'ICU', 'CCU']
const ROOMS: Record<string, string[]> = {
  '내과3병동': ['301호', '302호', '303호', '304호'],
  '외과1병동': ['401호', '402호'],
  ICU: ['ICU-1', 'ICU-2'],
  CCU: ['CCU-1'],
}

const STATUS_COLOR: Record<string, string> = {
  urgent: '#D63031',
  warning: '#F39C12',
  normal: '#1A6B4F',
}

export function Sidebar() {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()

  const [selectedWard, setSelectedWard] = useState(WARDS[0])
  const [selectedRoom, setSelectedRoom] = useState(ROOMS[WARDS[0]][0])

  const { patients } = usePatient({ ward: selectedWard, room: selectedRoom })

  const handleWardChange = (ward: string) => {
    const firstRoom = ROOMS[ward]?.[0] ?? ''
    setSelectedWard(ward)
    setSelectedRoom(firstRoom)
  }

  return (
    <nav className="w-64 min-h-full border-r bg-white flex flex-col shrink-0">
      {/* 병동 선택 */}
      <div className="px-4 pt-4 pb-2 border-b">
        <p className="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-2">
          🛠️ 병동 선택
        </p>
        <div className="flex flex-wrap gap-1.5">
          {WARDS.map((w) => (
            <button
              key={w}
              onClick={() => handleWardChange(w)}
              className={`px-2.5 py-1 text-[11px] rounded-md border transition-colors ${
                selectedWard === w
                  ? 'border-[#1A6B4F] bg-[#E8F5EF] text-[#1A6B4F] font-bold'
                  : 'border-gray-200 text-gray-500 hover:bg-gray-50'
              }`}
            >
              {w}
            </button>
          ))}
        </div>
      </div>

      {/* 호실 선택 */}
      <div className="px-4 py-3 border-b">
        <p className="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-2">
          🛠️ 호실
        </p>
        <div className="space-y-0.5">
          {(ROOMS[selectedWard] ?? []).map((r) => (
            <button
              key={r}
              onClick={() => setSelectedRoom(r)}
              className={`w-full text-left px-3 py-2 rounded-md text-[13px] transition-colors ${
                selectedRoom === r
                  ? 'bg-[#E8F5EF] text-[#1A6B4F] font-semibold'
                  : 'text-gray-800 hover:bg-gray-50'
              }`}
            >
              {r}
            </button>
          ))}
        </div>
      </div>

      {/* 환자 목록 */}
      <div className="flex-1 overflow-y-auto px-4 py-3">
        <p className="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-2">
          🛠️ 환자 목록 — {selectedRoom}
        </p>
        {patients.length === 0 ? (
          <p className="text-[12px] text-gray-400 mt-4 text-center">환자가 없습니다</p>
        ) : (
          <div className="space-y-2">
            {patients.map((p) => {
              const isActive = pathname === '/dashboard' && searchParams.get('id') === String(p.id)
              return (
                <button
                  key={p.id}
                  onClick={() => router.push(`/dashboard?id=${p.id}`)}
                  className={`w-full text-left border rounded-lg px-3 py-2.5 transition-colors ${
                    isActive
                      ? 'border-[#1A6B4F] bg-[#E8F5EF]'
                      : 'border-gray-200 bg-white hover:border-[#1A6B4F] hover:bg-[#E8F5EF]'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span
                        className="w-2 h-2 rounded-full shrink-0"
                        style={{
                          background:
                            p.pendingSTTCount > 1
                              ? STATUS_COLOR.urgent
                              : p.pendingSTTCount > 0
                                ? STATUS_COLOR.warning
                                : STATUS_COLOR.normal,
                        }}
                      />
                      <span className="text-[13px] font-semibold text-gray-900">{p.name}</span>
                      <span className="text-[11px] text-gray-400">{p.age}세</span>
                    </div>
                    <span className="text-[11px] text-gray-500 bg-gray-100 px-1.5 py-0.5 rounded">
                      {p.bedNo}
                    </span>
                  </div>
                  <p className="text-[11px] text-gray-500 mt-1 ml-4 truncate">{p.diagnosis}</p>
                  {p.pendingSTTCount > 0 && (
                    <div className="mt-1.5 ml-4">
                      <span className="text-[10px] text-[#E8722A] bg-[#FEF0E6] px-1.5 py-0.5 rounded">
                        미이관 {p.pendingSTTCount}
                      </span>
                    </div>
                  )}
                </button>
              )
            })}
          </div>
        )}
      </div>
    </nav>
  )
}
