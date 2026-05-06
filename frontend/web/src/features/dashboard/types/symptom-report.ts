/**
 * 환자 증상 요청(self-report) 조회 타입.
 *
 * - [간호사용 웹] AlertsTab (EMR "환자 호출" 탭) — 선택된 환자의 in_progress 입원 동안 제출된
 *   모든 증상 요청을 submittedAt DESC 로 조회.
 *
 * 백엔드 응답 wrapper: { success, message, errorCode, data } — 인터셉터가 평탄화.
 */

export type InputMethod = "stt" | "quick_button" | "text";

export interface SymptomReportItem {
  selfReportId: number;
  inputMethod: InputMethod;
  // 버튼 선택 시에만 채워짐. text 입력은 null.
  buttonLabel: string | null;
  symptomText: string;
  // ISO datetime
  submittedAt: string;
}

export interface SymptomReportListResponse {
  patientId: number;
  patientName: string;
  totalCount: number;
  symptoms: SymptomReportItem[];
}

// UI 라벨
export const INPUT_METHOD_LABEL: Record<InputMethod, string> = {
  stt: "음성",
  quick_button: "버튼",
  text: "직접 입력",
};
