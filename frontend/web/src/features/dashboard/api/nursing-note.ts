/**
 * 간호 노트 통합 조회 + 간호 기록 작성/수정 + 통합 확정/삭제 API.
 *
 * - 조회
 *   - getNursingNotes(encounterId, date) — GET /encounters/{id}/nursing-notes (occurredAt DESC)
 *   - getDraftNursingNotes(encounterId) — GET /encounters/{id}/nursing-notes/drafts
 *
 * - 작성 / 수정 (STT 도메인)
 *   - createNursingRecord — POST /nursing-records (수동 작성)
 *   - updateSttNote — PATCH /nursing-notes/stt/{nursingRecordId} (본문 / confirmedAt)
 *
 * - 확정 / 삭제 (STT/MEDICATION 통합 — itemId 가 nursingRecordId(숫자) 또는 taggingId(UUID) 자동 분기)
 *   - confirmNursingNoteItem — POST /nursing-notes/{itemId}/confirm
 *   - deleteNursingNoteItem — DELETE /nursing-notes/{itemId}
 */
import { client } from "@/lib/client";
import type {
  NursingNoteItem,
  NursingRecordManualCreateRequest,
  NursingRecordUpdateRequest,
  NursingRecordWriteResponse,
} from "../types/nursing-note";

export const getNursingNotes = (
  encounterId: number,
  // ISO date (yyyy-MM-dd) — 백엔드 필수 파라미터 (없으면 500)
  date: string,
): Promise<NursingNoteItem[]> =>
  client
    .get(`/encounters/${encounterId}/nursing-notes`, { params: { date } })
    .then((response) => response.data);

// 한 입원의 status=draft 항목만 통합 반환 (날짜 무관). STT 는 nursingRecordId, 투약은 taggingId 포함.
export const getDraftNursingNotes = (
  encounterId: number,
): Promise<NursingNoteItem[]> =>
  client
    .get(`/encounters/${encounterId}/nursing-notes/drafts`)
    .then((response) => response.data);

export const createNursingRecord = (
  request: NursingRecordManualCreateRequest,
): Promise<NursingRecordWriteResponse> =>
  client.post(`/nursing-records`, request).then((response) => response.data);

export const updateSttNote = (
  nursingRecordId: number,
  request: NursingRecordUpdateRequest,
): Promise<NursingNoteItem> =>
  client
    .patch(`/nursing-notes/stt/${nursingRecordId}`, request)
    .then((response) => response.data);

// 통합 확정/삭제 — itemId 는 STT 면 nursingRecordId(number), MEDICATION 이면 taggingId(string UUID).
export const confirmNursingNoteItem = (
  itemId: number | string,
): Promise<NursingNoteItem> =>
  client
    .post(`/nursing-notes/${itemId}/confirm`)
    .then((response) => response.data);

export const deleteNursingNoteItem = (
  itemId: number | string,
): Promise<void> =>
  client.delete(`/nursing-notes/${itemId}`).then(() => undefined);
