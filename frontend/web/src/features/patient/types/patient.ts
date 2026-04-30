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

// [환자용 웹앱] NFC 진입 응답
export interface PatientNfc {
  patientId: number;
  patientName: string;
  roomName: string;
}
