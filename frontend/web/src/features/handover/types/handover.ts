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

export interface HandoverShiftOverview {
  summary: string;               // AI 생성 narrative (교대 전체 환자 통합 요약)
  generatedAt: string;           // "YYYY-MM-DD HH:MM"
  model?: string;
}

export interface HandoverSummary {
  patientId: string;
  headline: string;              // 1~2줄 한줄 요약
  keyIssues: string[];           // 근무 중 주요 경과/이슈
  watchPoints: string[];         // 다음 교대 확인/주의 사항
  vitalsNote?: string;           // 최근 바이탈 트렌드 요약
  sourceRecordIds?: number[];    // 요약 근거가 된 원본 기록 id
  generatedAt: string;           // "YYYY-MM-DD HH:MM"
  model?: string;                // 요약 엔진 표시용
}
