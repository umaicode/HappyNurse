/**
 * 환자 API 함수.
 *
 * client 응답 인터셉터가 { success, data, ... } wrapper 의 data 만 평탄화하여 내려준다.
 *
 * - [간호사용 웹] getList(params) · getDetail(id)  ─ 목업 (mockup/nurse-patients.ts)
 * - [환자용 웹앱] getNfcEntry · getButtons · submitSymptom
 */
import { client } from "@/lib/client";
import {
  INITIAL_PATIENTS,
  INITIAL_PATIENT_DETAIL,
} from "@/mockup/nurse-patients";
import type { PatientNfc } from "@/features/auth/types";
import type {
  Patient,
  PatientDetail,
  PatientQuery,
  SymptomButton,
  SymptomSubmitRequest,
  SymptomSubmitResponse,
} from "../types/patient";

// [간호사용 웹] 환자 목록 / 상세
// TODO: 백엔드 연동 시 mockup 의존 제거 후 실제 API 호출로 교체

export const getList = (_params: PatientQuery): Promise<Patient[]> =>
  Promise.resolve(INITIAL_PATIENTS);

export const getDetail = (_id: string): Promise<PatientDetail> =>
  Promise.resolve(INITIAL_PATIENT_DETAIL);

// [환자용 웹앱] NFC 진입

export const getNfcEntry = (patientId: number): Promise<PatientNfc> =>
  client
    .get(`/nfc/patients/${patientId}/entry`)
    .then((response) => response.data);

// [환자용 웹앱] 증상 버튼 목록

export const getButtons = (): Promise<SymptomButton[]> =>
  client.get("/symptoms/buttons").then((response) => response.data);

// [환자용 웹앱] 증상 제출

export const submitSymptom = (
  patientId: number,
  body: SymptomSubmitRequest,
): Promise<SymptomSubmitResponse> =>
  client
    .post(`/patients/${patientId}/symptoms`, body)
    .then((response) => response.data);
