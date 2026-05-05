/**
 * 환자별 증상 요청(self-report) 조회 API.
 *
 * - [간호사용 웹] getSymptomReports(patientId) — GET /patients/{patientId}/symptoms
 *   해당 환자의 in_progress 입원 동안 제출된 모든 요청을 submittedAt DESC 로 반환.
 */
import { client } from "@/lib/client";
import type { SymptomReportListResponse } from "../types/symptom-report";

export const getSymptomReports = (
  patientId: number,
): Promise<SymptomReportListResponse> =>
  client
    .get(`/patients/${patientId}/symptoms`)
    .then((response) => response.data);
