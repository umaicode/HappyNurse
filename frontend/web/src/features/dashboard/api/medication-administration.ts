/**
 * 투약 그룹(taggingId 단위) 액션 API.
 *
 * - update / delete / confirm — 그룹 단위 작업. 그룹 생성은 모바일 NFC 흐름.
 */
import { client } from "@/lib/client";
import type {
  MedicationAdministrationUpdateRequest,
  MedicationAdministrationWriteResponse,
} from "../types/medication-administration";

export const updateMedicationGroup = (
  taggingId: string,
  request: MedicationAdministrationUpdateRequest,
): Promise<MedicationAdministrationWriteResponse> =>
  client
    .patch(`/medication-administrations/tagging/${taggingId}`, request)
    .then((response) => response.data);

export const deleteMedicationGroup = (taggingId: string): Promise<void> =>
  client
    .delete(`/medication-administrations/tagging/${taggingId}`)
    .then(() => undefined);

export const confirmMedicationGroup = (
  taggingId: string,
): Promise<MedicationAdministrationWriteResponse> =>
  client
    .post(`/medication-administrations/tagging/${taggingId}/confirm`)
    .then((response) => response.data);
