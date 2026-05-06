"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  confirmMedicationGroup,
  deleteMedicationGroup,
  updateMedicationGroup,
} from "../api/medication-administration";
import type { MedicationAdministrationUpdateRequest } from "../types/medication-administration";

const invalidateNotesFor = (
  queryClient: ReturnType<typeof useQueryClient>,
  encounterId: number,
) => {
  queryClient.invalidateQueries({
    queryKey: ["encounter", encounterId, "nursing-notes"] as const,
  });
};

export const useUpdateMedicationGroup = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      taggingId,
      request,
    }: {
      taggingId: string;
      request: MedicationAdministrationUpdateRequest;
    }) => updateMedicationGroup(taggingId, request),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};

export const useDeleteMedicationGroup = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (taggingId: string) => deleteMedicationGroup(taggingId),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};

export const useConfirmMedicationGroup = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (taggingId: string) => confirmMedicationGroup(taggingId),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};
