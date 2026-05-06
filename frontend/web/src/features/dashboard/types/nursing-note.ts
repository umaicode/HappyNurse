/**
 * 간호 노트 통합 조회 타입.
 *
 * - [간호사용 웹] EMRGrid 의 "간호 기록" 탭 (NursingTab)
 *
 * 백엔드는 한 행이 STT_NOTE (간호 기록 본문) 또는 MEDICATION (NFC 태깅 묶음 — N개 약물) 두 종류로 내려온다.
 * type 필드로 분기한다. 백엔드 응답 wrapper: { success, message, errorCode, data } — 인터셉터가 평탄화.
 */

export type NursingNoteType = "STT_NOTE" | "MEDICATION";
export type RecordStatus = "draft" | "confirmed" | "amended";

export interface MedicationItem {
  medicationAdminId: number;
  productCode: string;
  productName: string;
  dosageQuantity: number;
  dosageUnit: string;
  frequency: number;
  route: string;
}

interface NursingNoteCommon {
  status: RecordStatus;
  // ISO datetime — DESC 정렬 기준
  occurredAt: string;
  authorName: string;
  authorPractitionerId: number;
  // 작성자 == 본인일 때만 true (서버 판단)
  editable: boolean;
}

export interface SttNoteItem extends NursingNoteCommon {
  type: "STT_NOTE";
  nursingRecordId: number;
  // status 가 draft 면 editContent, confirmed/amended 면 finalContent — 서버가 알아서 골라 내려줌
  content: string;
}

export interface MedicationNoteItem extends NursingNoteCommon {
  type: "MEDICATION";
  taggingId: string;
  nfcTagVerified: boolean;
  medications: MedicationItem[];
}

export type NursingNoteItem = SttNoteItem | MedicationNoteItem;

// type 한글 라벨 (UI 표시용).
export const NOTE_TYPE_LABEL: Record<NursingNoteType, string> = {
  STT_NOTE: "음성",
  MEDICATION: "투약",
};

export const NOTE_TYPE_TONE: Record<NursingNoteType, string> = {
  STT_NOTE: "text-content-tertiary",
  MEDICATION: "text-brand-primary",
};

// status 한글 라벨 (UI 룰: 라벨 매핑은 도메인 types 에).
export const RECORD_STATUS_LABEL: Record<RecordStatus, string> = {
  draft: "임시",
  confirmed: "확정",
  amended: "수정됨",
};

export const RECORD_STATUS_TONE: Record<RecordStatus, string> = {
  draft: "text-status-neutral",
  confirmed: "text-status-success",
  amended: "text-status-warning",
};

// ── 간호 기록 작성/수정 (NursingRecord 도메인 — 노트 조회와 같은 entity, path 만 다름) ────────

export interface NursingRecordManualCreateRequest {
  encounterId: number;
  content: string;
  // 미지정 시 서버 현재 시각으로 저장. 기록 사이에 끼워 넣을 때는 prev/next 사이 값을 계산해 전달.
  confirmedAt?: string;
}

export interface NursingRecordUpdateRequest {
  content?: string;
  confirmedAt?: string;
}

export interface NursingRecordWriteResponse {
  nursingRecordId: number;
  status: RecordStatus;
  content: string;
  confirmedAt: string | null;
}
