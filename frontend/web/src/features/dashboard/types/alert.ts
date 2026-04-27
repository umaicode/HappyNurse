export interface PatientAlert {
  id: number;
  patientId: string;
  patientName: string;
  room: string;
  time: string;
  category: string;
  message: string;
}
