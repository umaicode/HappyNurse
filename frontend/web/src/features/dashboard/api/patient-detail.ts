/**
 * 환자 상세 API.
 *
 * - [간호사용 웹] getPatientDetail(patientId) — GET /patient/{patientId}
 */
import { client } from "@/lib/client";
import type { PatientDetailResponse } from "../types/patient-detail";

export const getPatientDetail = (
  patientId: number,
): Promise<PatientDetailResponse> =>
  client
    .get(`/patient/${patientId}`)
    .then((response) => response.data?.data ?? response.data);
