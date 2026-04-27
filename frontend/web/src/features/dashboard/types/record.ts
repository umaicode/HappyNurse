// NFC 태깅으로 들어온 약물 기록의 구조화된 값
export type DrugInfo = {
  code: string;
  name: string;
  dose: string;
  unit: string;
  frequency: string;
  method: string;
};

// 간호 기록 단일 행 타입 (STT 줄글 + NFC 구조화 기록 공용)
export type NursingRecord = {
  id: number;
  time: string;
  category: string;
  content: string;
  status: string;
  writer: string;
  isConfirmed: boolean;
  isHandover?: boolean;
  isAISuggested?: boolean;
  patientId?: string;
  source?: string;
  drug?: DrugInfo;
};
