/**
 * 환자 카드.
 * 이름 · 나이 · 침대번호 · 진단명 · 미이관 STT 건수.
 */
import type { Patient } from '../types/patient'

export function Card({ patient }: { patient: Patient }) {
  return (
    <div className="card rounded-lg border p-4">
      <h3 className="font-bold">{patient.name}</h3>
      <p>나이: {patient.age}세</p>
      <p>침대: {patient.bedNo}</p>
      <p>진단: {patient.diagnosis}</p>
      <p>미이관 STT: {patient.pendingSTTCount}건</p>
    </div>
  )
}
