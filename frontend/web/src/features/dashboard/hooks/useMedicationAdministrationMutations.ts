"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { updateMedicationGroup } from "../api/medication-administration";
import type { NursingNoteMedicationEditRequest } from "../types/medication-administration";

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

// MEDICATION 그룹 수정 — body shape 가 STT 와 달라 별도 hook 유지.
// 확정/삭제는 useConfirmNursingNoteItem / useDeleteNursingNoteItem (통합 라우터) 사용.
export const useUpdateMedicationGroup = (encounterId: number | null) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      taggingId,
      request,
    }: {
      taggingId: string;
      request: NursingNoteMedicationEditRequest;
    }) => updateMedicationGroup(taggingId, request),
    onSuccess: () => {
      if (encounterId !== null) invalidateNotesFor(queryClient, encounterId);
    },
  });
};
