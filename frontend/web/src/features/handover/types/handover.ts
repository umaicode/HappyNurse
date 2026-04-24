export type HandoverVitalStatus = "normal" | "abnormal";

export interface HandoverVitals {
  status: HandoverVitalStatus;
  detail: string;
}

export interface HandoverPatient {
  id: string;
  name: string;
  patientNo: string;
  birthDate: string;
  room: string;
  mainSymptom: string;
  assignedNurse: string;
  recentVitals: HandoverVitals;
}
