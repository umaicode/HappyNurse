export interface PatientAlert {
  id: number;
  patientId: string;
  patientName: string;
  room: string;
  time: string;
  severity: "critical" | "warning" | "info";
  category: string;
  message: string;
  status: "unread" | "acknowledged" | "resolved";
}

export const SEVERITY_LABEL: Record<PatientAlert["severity"], string> = {
  critical: "긴급",
  warning: "경고",
  info: "안내",
};

export const STATUS_LABEL: Record<PatientAlert["status"], string> = {
  unread: "미확인",
  acknowledged: "확인",
  resolved: "해결",
};
