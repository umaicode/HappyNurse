/**
 * 투약 그룹(taggingId 단위) 수정 API.
 *
 * - update — `PATCH /nursing-notes/medication/{taggingId}` (백엔드 통합 라우터)
 * - 그룹 단위 확정 / 삭제 — `nursing-note.ts` 의 통합 helper(confirm/deleteNursingNoteItem) 사용
 */
import { client } from "@/lib/client";
import type { NursingNoteMedicationEditRequest } from "../types/medication-administration";
import type { NursingNoteItem } from "../types/nursing-note";

export const updateMedicationGroup = (
  taggingId: string,
  request: NursingNoteMedicationEditRequest,
): Promise<NursingNoteItem> =>
  client
    .patch(`/nursing-notes/medication/${taggingId}`, request)
    .then((response) => response.data);
