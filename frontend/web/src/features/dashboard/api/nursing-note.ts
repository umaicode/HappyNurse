/**
 * 간호 노트 통합 조회 + 간호 기록 작성/수정 API.
 *
 * - [조회] getNursingNotes(encounterId) — GET /encounters/{encounterId}/nursing-notes
 *   응답은 occurredAt 내림차순 정렬되어 내려온다 (한 행 = STT_NOTE 또는 MEDICATION).
 * - [쓰기] create / update / delete / confirm — 간호 기록(NursingRecord) 도메인.
 *   MEDICATION 행 액션은 medication-administration.ts 에 분리.
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

export const createNursingRecord = (
  request: NursingRecordManualCreateRequest,
): Promise<NursingRecordWriteResponse> =>
  client.post(`/nursing-records`, request).then((response) => response.data);

export const updateNursingRecord = (
  nursingRecordId: number,
  request: NursingRecordUpdateRequest,
): Promise<NursingRecordWriteResponse> =>
  client
    .patch(`/nursing-records/${nursingRecordId}`, request)
    .then((response) => response.data);

export const deleteNursingRecord = (nursingRecordId: number): Promise<void> =>
  client.delete(`/nursing-records/${nursingRecordId}`).then(() => undefined);

export const confirmNursingRecord = (
  nursingRecordId: number,
): Promise<NursingRecordWriteResponse> =>
  client
    .post(`/nursing-records/${nursingRecordId}/confirm`)
    .then((response) => response.data);
