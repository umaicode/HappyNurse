/**
 * 간호 노트 mockup — 투약 컬럼 시각 검증용.
 *
 * 사용 위치: NursingTab (실 응답이 빈 배열일 때 fallback 으로 노출).
 * 검증 끝나면 import 제거 후 이 파일도 삭제 가능.
 */
import type { NursingNoteItem } from "@/features/dashboard/types/nursing-note";

// 사이드바 카운트 뱃지 시연용 — "문현지" 환자의 unconfirmedNursingCount 를 이 값으로 override.
export const MOCK_UNCONFIRMED_PATIENT_NAME = "문현지";

export const MOCK_NURSING_NOTES: NursingNoteItem[] = [
  {
    type: "STT_NOTE",
    nursingRecordId: 8001,
    status: "confirmed",
    occurredAt: "2026-05-06T07:30:00",
    authorName: "🛠️ 김간호",
    authorPractitionerId: 101,
    editable: true,
    content:
      "🛠️ 환자 상태 안정. 활력 징후 정상 범위. 통증 호소 없음. 식이 양호.",
  },
  {
    type: "MEDICATION",
    taggingId: "tag-9001",
    status: "confirmed",
    occurredAt: "2026-05-06T08:00:00",
    authorName: "🛠️ 김간호",
    authorPractitionerId: 101,
    editable: true,
    nfcTagVerified: true,
    medications: [
      {
        medicationAdminId: 7001,
        productCode: "🛠️ A001",
        productName: "🛠️ Acetaminophen",
        dosageQuantity: 500,
        dosageUnit: "mg",
        frequency: 1,
        route: "PO",
      },
      {
        medicationAdminId: 7002,
        productCode: "🛠️ A002",
        productName: "🛠️ Tramadol",
        dosageQuantity: 50,
        dosageUnit: "mg",
        frequency: 1,
        route: "PO",
      },
    ],
  },
  {
    type: "STT_NOTE",
    nursingRecordId: 8002,
    status: "draft",
    occurredAt: "2026-05-06T09:15:00",
    authorName: "🛠️ 박간호",
    authorPractitionerId: 102,
    editable: true,
    content: "🛠️ 환자 두통 호소. 진통제 투여 후 경과 관찰 필요.",
  },
  {
    type: "STT_NOTE",
    nursingRecordId: 8003,
    status: "draft",
    occurredAt: "2026-05-06T11:40:00",
    authorName: "🛠️ 박간호",
    authorPractitionerId: 102,
    editable: true,
    content: "🛠️ V/S 체크 — BP 138/92, HR 88, T 37.3. 발열 경향, 추후 재측정 필요.",
  },
  {
    type: "STT_NOTE",
    nursingRecordId: 8004,
    status: "draft",
    occurredAt: "2026-05-06T13:20:00",
    authorName: "🛠️ 김간호",
    authorPractitionerId: 101,
    editable: true,
    content: "🛠️ 보호자 면회 후 환자 안정. 식사 60% 섭취.",
  },
  {
    type: "MEDICATION",
    taggingId: "tag-9002",
    status: "draft",
    occurredAt: "2026-05-06T10:00:00",
    authorName: "🛠️ 박간호",
    authorPractitionerId: 102,
    editable: true,
    nfcTagVerified: true,
    medications: [
      {
        medicationAdminId: 7003,
        productCode: "🛠️ B001",
        productName: "🛠️ N/S",
        dosageQuantity: 1000,
        dosageUnit: "mL",
        frequency: 1,
        route: "IV",
      },
    ],
  },
];

// STT_NOTE draft 만 카운트 — popover 가 STT_NOTE draft 만 노출하므로 일치시킴.
export const MOCK_UNCONFIRMED_COUNT = MOCK_NURSING_NOTES.filter(
  (note) => note.type === "STT_NOTE" && note.status === "draft",
).length;
