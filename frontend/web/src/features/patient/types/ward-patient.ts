/**
 * 병동 입원 환자 (Encounter 기반) 타입.
 *
 * - [간호사용 웹] 사이드바 / 담당 환자 모달 / 인계 환자 목록 데이터 소스
 *
 * 백엔드 응답 wrapper: { success, message, errorCode, data } — api 함수에서 data 만 추출.
 */

export type Gender = "male" | "female" | "other" | "unknown";

export interface WardPatient {
  encounterId: number;
  patientId: number;
  name: string;
  gender: Gender;
  // ISO date 문자열, 예: "1999-05-20"
  birthDate: string;
  roomName: string;
  bedName: string;
  unconfirmedNursingCount: number;
  // Redis 저장 기준, 시프트 교대 후에도 보존
  isMyPatient: boolean;
  // 담당 간호사 — 본인 외 다른 간호사가 담당이면 그 정보, 미배정이면 null.
  assignedNurseId: number | null;
  assignedNurseName: string | null;
  // 입원 시 주 증상 / 수술명 — 인수인계 리포트 미생성 시 fallback 으로 표시.
  chiefComplaint: string | null;
  surgeryName: string | null;
}

export interface AssignMyPatientsResponse {
  assignedEncounterIds: number[];
  releasedEncounterIds: number[];
  overwroteFromOthersCount: number;
}
