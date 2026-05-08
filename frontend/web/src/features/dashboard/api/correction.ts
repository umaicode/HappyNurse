/**
 * 퀵수정 API (AI 서버 — FastAPI).
 *
 * - 분석: POST /api/correction/analyze — content → 교정 후보 (start/end/candidates)
 * - 피드백: POST /api/correction/apply — 사용자 후보 선택 이력 저장
 */
import { aiClient } from "@/lib/ai-client";
import type {
  CorrectionApplyRequest,
  CorrectionApplyResponse,
  QuickCorrectionAnalyzeRequest,
  QuickCorrectionAnalyzeResponse,
} from "../types/correction";

export const analyzeCorrections = (
  request: QuickCorrectionAnalyzeRequest,
): Promise<QuickCorrectionAnalyzeResponse> =>
  aiClient
    .post(`/api/correction/analyze`, request)
    .then((response) => response.data);

export const applyCorrection = (
  request: CorrectionApplyRequest,
): Promise<CorrectionApplyResponse> =>
  aiClient
    .post(`/api/correction/apply`, request)
    .then((response) => response.data);
