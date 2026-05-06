"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  confirmNursingRecord,
  createNursingRecord,
  deleteNursingRecord,
  updateNursingRecord,
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

export const useUpdateNursingRecord = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      nursingRecordId,
      request,
    }: {
      nursingRecordId: number;
      request: NursingRecordUpdateRequest;
    }) => updateNursingRecord(nursingRecordId, request),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};

export const useDeleteNursingRecord = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (nursingRecordId: number) =>
      deleteNursingRecord(nursingRecordId),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};

export const useConfirmNursingRecord = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (nursingRecordId: number) =>
      confirmNursingRecord(nursingRecordId),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};
