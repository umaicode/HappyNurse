/**
 * AI 서버 퀵수정 (용어 교정 피드백) 타입.
 *
 * - [간호사용 웹] NursingTab STT 행 수정 모드 — 본문 분석 후 교정 후보 칩 표시 + 후보 선택 시 피드백 저장
 *
 * AI 서버는 FastAPI 라 응답이 snake_case (백엔드 client 의 wrapper unwrap 과 별개).
 * 필드명 그대로 받고, 사용처에서 직접 snake_case 로 참조.
 */

export type CorrectionType = "exact" | "fuzzy" | "original";

export interface CorrectionCandidate {
  word: string;
  confidence: number;
  type: CorrectionType;
}

// 본문 한 위치의 교정 후보 묶음. start/end 는 content 내 인덱스 (UTF-16 단위).
export interface CorrectionItem {
  original: string;
  start: number;
  end: number;
  candidates: CorrectionCandidate[];
}

// POST /api/correction/analyze
export interface QuickCorrectionAnalyzeRequest {
  nursing_record_id: number;
  content: string;
}

export interface QuickCorrectionAnalyzeResponse {
  success: boolean;
  nursing_record_id: number;
  correction_count: number;
  corrections: CorrectionItem[];
}

// POST /api/correction/apply
export interface CorrectionApplyRequest {
  nursing_record_id: number;
  original_word: string;
  replaced_word: string;
  // 클라에서 후보 선택 시 type 그대로 전달 (exact/fuzzy). 자유 입력 시 manual.
  correction_type?: "exact" | "fuzzy" | "manual";
  suggestion_id?: number;
}

export interface CorrectionApplyResponse {
  success: boolean;
  message: string;
  repeat_count: number;
  // 같은 교정이 5회 이상 반복되면 true (관리자 사전 등록 권장 신호 — 일반 UI 에선 무시)
  suggest_dictionary: boolean;
}
