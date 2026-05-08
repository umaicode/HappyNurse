/**
 * 투약 그룹(taggingId 단위) 수정 요청 타입.
 *
 * - [간호사용 웹] NursingTab 의 MEDICATION 행에서 그룹 시각/약별 1회 투여량 수정
 *
 * 작성(POST /drug/record)은 모바일 NFC 태깅 흐름. 웹은 수정만 (확정/삭제는 통합 라우터로 흡수됨).
 * 응답은 통합 라우터(`/nursing-notes/medication/{taggingId}`) 가 NursingNoteItem 으로 반환.
 */

export interface MedicationDosageEditItem {
  medicationAdminId: number;
  dosageQuantity: number;
}

export interface NursingNoteMedicationEditRequest {
  // 그룹 일괄 적용 기록 시각. 같은 taggingId 의 모든 row 가 함께 갱신된다.
  confirmedAt?: string;
  // 그룹 내 약별 1회 투여량 변경 — 그룹 내 medicationAdminId 만 허용.
  medications?: MedicationDosageEditItem[];
}
