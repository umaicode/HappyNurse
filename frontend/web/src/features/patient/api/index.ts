/**
 * 환자 API 함수.
 * getList(params) · getDetail(id)
 *
 * TODO: 백엔드 연동 시 목업 데이터 제거 후 아래 주석 해제
 */
import type { Patient, PatientDetail, PatientQuery } from '../types/patient'

// ─── 목업 데이터 ───────────────────────────────────────────
const MOCK_PATIENTS: Patient[] = [
  { id: '1', name: '김영수', age: 68, bedNo: '01A', roomNo: '302호', diagnosis: 'I50.0 울혈성 심부전', pendingSTTCount: 2 },
  { id: '2', name: '박미선', age: 55, bedNo: '02B', roomNo: '302호', diagnosis: 'J18.9 폐렴', pendingSTTCount: 0 },
  { id: '3', name: '이철호', age: 72, bedNo: '03A', roomNo: '302호', diagnosis: 'N18.3 만성 신장병 3기', pendingSTTCount: 1 },
  { id: '4', name: '정은지', age: 45, bedNo: '04A', roomNo: '302호', diagnosis: 'E11.9 당뇨병', pendingSTTCount: 0 },
]

const MOCK_DETAIL: PatientDetail = {
  id: '1', name: '김영수', age: 68, bedNo: '01A', roomNo: '302호',
  diagnosis: 'I50.0 울혈성 심부전', pendingSTTCount: 2,
  admittedAt: '2025-04-01', nurseId: 'nurse-01',
}
// ──────────────────────────────────────────────────────────

export const getList = (_params: PatientQuery): Promise<Patient[]> =>
  Promise.resolve(MOCK_PATIENTS)

export const getDetail = (_id: string): Promise<PatientDetail> =>
  Promise.resolve(MOCK_DETAIL)

// ─── 백엔드 연동 시 아래로 교체 ────────────────────────────
// import { client } from '@/lib/client'
// export const getList = (params: PatientQuery) =>
//   client.get<Patient[]>('/patients', { params }).then((r) => r.data)
// export const getDetail = (id: string) =>
//   client.get<PatientDetail>(`/patients/${id}`).then((r) => r.data)
