/**
 * 투약 그룹(taggingId 단위) 액션 타입.
 *
 * - [간호사용 웹] NursingTab 의 MEDICATION 행에서 그룹 시각/용량 수정 · 확정 · 취소
 *
 * 작성(POST /drug/record)은 모바일 NFC 태깅 흐름. 웹은 수정/확정/삭제만.
 */
import type { RecordStatus } from "./nursing-note";

export interface MedicationDosageUpdate {
  medicationAdminId: number;
  dosageQuantity: number;
  dosageUnit: string;
}

export interface MedicationAdministrationUpdateRequest {
  // 그룹 일괄 적용 투약 시각
  effectiveDatetime?: string;
  // 약별 용량 변경 — 그룹 내 medicationAdminId 만 허용
  medications?: MedicationDosageUpdate[];
}

export interface MedicationAdministrationWriteResponse {
  taggingId: string;
  status: RecordStatus;
}
