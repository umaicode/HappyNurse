/**
 * 환자 목록 테이블.
 * 호실 · 환자명 · 입원일 · 상태. 행 클릭 → /patients/[id]
 */
'use client'

import { useRouter } from 'next/navigation'
import type { Patient } from '../types'

export function Table({ data }: { data: Patient[] }) {
  const router = useRouter()

  return (
    <table className="w-full">
      <thead>
        <tr>
          <th>호실</th>
          <th>침대</th>
          <th>환자명</th>
          <th>진단명</th>
          <th>미이관 STT</th>
        </tr>
      </thead>
      <tbody>
        {data.map((patient) => (
          <tr
            key={patient.id}
            onClick={() => router.push(`/patients/${patient.id}`)}
            className="cursor-pointer hover:bg-gray-50"
          >
            <td>{patient.roomNo}</td>
            <td>{patient.bedNo}</td>
            <td>{patient.name}</td>
            <td>{patient.diagnosis}</td>
            <td>{patient.pendingSTTCount}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
