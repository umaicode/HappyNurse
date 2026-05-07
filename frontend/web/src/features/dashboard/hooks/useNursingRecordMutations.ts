"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  confirmNursingNoteItem,
  createNursingRecord,
  deleteNursingNoteItem,
  updateSttNote,
} from "../api/nursing-note";
import type {
  NursingRecordManualCreateRequest,
  NursingRecordUpdateRequest,
} from "../types/nursing-note";

const invalidateNotesFor = (
  queryClient: ReturnType<typeof useQueryClient>,
  encounterId: number,
) => {
  queryClient.invalidateQueries({
    queryKey: ["encounter", encounterId, "nursing-notes"] as const,
  });
  // 사이드바 뱃지(unconfirmedNursingCount) 동기화 — wardPatients 응답에 카운트가 포함됨.
  queryClient.invalidateQueries({ queryKey: ["ward", "me", "patients"] });
};

export const useCreateNursingRecord = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: NursingRecordManualCreateRequest) =>
      createNursingRecord(request),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};

// STT 행 본문 / confirmedAt 수정.
export const useUpdateNursingRecord = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      nursingRecordId,
      request,
    }: {
      nursingRecordId: number;
      request: NursingRecordUpdateRequest;
    }) => updateSttNote(nursingRecordId, request),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};

// 통합 삭제 — itemId 는 STT 면 nursingRecordId, MEDICATION 이면 taggingId.
export const useDeleteNursingNoteItem = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (itemId: number | string) => deleteNursingNoteItem(itemId),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};

// 통합 확정 — itemId 는 STT 면 nursingRecordId, MEDICATION 이면 taggingId.
export const useConfirmNursingNoteItem = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (itemId: number | string) => confirmNursingNoteItem(itemId),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};
