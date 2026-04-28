/**
 * 환자 도메인 서버 API 응답 타입 (스캐폴드).
 * 백엔드 연동 시 사용. UI 표시용 Patient/Room/Ward는 ./patient.ts 참고.
 */

export interface Patient {
  id: string
  name: string
  age: number
  bedNo: string
  roomNo: string
  diagnosis: string
  pendingSTTCount: number
}

export interface PatientDetail extends Patient {
  admittedAt: string
  nurseId: string
}

export interface PatientQuery {
  ward?: string
  room?: string
  search?: string
}
