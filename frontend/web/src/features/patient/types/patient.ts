export interface Patient {
  id: string;
  name: string;
  age: number;
  gender: string;
  birthday: string;
  assignedNurse: string;
  unconfirmedCount: number;
  bedNo?: string;
  roomNo?: string;
  room?: string;
  diagnosis?: string;
  pendingSTTCount?: number;
}

export interface PatientDetail extends Patient {
  admittedAt: string;
  nurseId: string;
}

export interface PatientQuery {
  ward?: string;
  room?: string;
  search?: string;
}

export interface Room {
  id: string;
  name: string;
  capacity: number;
  patients: Patient[];
}

export interface Ward {
  id: string;
  name: string;
  rooms: Room[];
}

// [환자용 웹앱] 증상 버튼 (GET /symptoms/buttons)
export interface SymptomButton {
  buttonId: number;
  label: string;
  description: string;
  displayOrder: number;
}

// [환자용 웹앱] 증상 제출 (POST /patients/{patientId}/symptoms)
// buttonId 와 symptomText 중 하나만 채워서 전송한다.
export interface SymptomSubmitRequest {
  buttonId?: number;
  symptomText?: string;
}

export interface SymptomSubmitResponse {
  selfReportId: number;
  submittedAt: string;
}
