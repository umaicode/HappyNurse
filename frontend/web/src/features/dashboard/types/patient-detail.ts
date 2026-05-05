/**
 * 환자 상세 (Patient + 현재 입원 Encounter) API 응답 타입.
 *
 * - [간호사용 웹] EMRGrid 헤더 데이터 소스
 *
 * 백엔드 응답 wrapper: { success, message, errorCode, data } — api 함수에서 data 만 추출.
 */
import type { Gender } from "@/features/patient/types/ward-patient";

export interface PatientDetailResponse {
  patientId: number;
  identifierValue: string;
  name: string;
  gender: Gender;
  birthDate: string;
  phone: string;
  address: string;

  // 현재 입원(Encounter) 정보
  encounterId: number;
  status: string;
  periodStart: string;
  diseaseName: string;
  chiefComplaint: string;
  surgeryName: string;
  departmentCode: string;
  wardName: string;
  roomName: string;
  bedName: string;

  // 담당 의사(주치의)
  attendingPhysicianId: number;
  attendingPhysicianName: string;
}
