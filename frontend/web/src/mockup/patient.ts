// 환자용 웹 화면 목업 데이터
// Scope: src/app/patient/**

export const patientMock = {
  id: 'P-0301',
  name: '김가민',
  room: '301호실',
  birthDate: '010429',
}

export const nurseMock = {
  id: 'N-017',
  name: '문현지',
  role: '담당 간호사',
}

export const symptomsMock = [
  { id: 'pain', label: '통증 및 약물요청' },
  { id: 'toilet', label: '화장실 도움' },
  { id: 'dressing', label: '드레싱 교체' },
  { id: 'iv', label: '수액 확인' },
  { id: 'position', label: '체위 변경' },
  { id: 'breathing', label: '호흡 불편' },
] as const

export const sendMock = {
  requestedSymptomLabel: '드레싱 교체',
  sentAt: '09:41',
}
