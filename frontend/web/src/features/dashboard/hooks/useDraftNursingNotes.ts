"use client";

import { useQuery } from "@tanstack/react-query";
import { getDraftNursingNotes } from "../api/nursing-note";

export const useDraftNursingNotes = (encounterId: number | null) =>
  useQuery({
    queryKey: ["encounter", encounterId, "nursing-notes", "drafts"] as const,
    queryFn: () => getDraftNursingNotes(encounterId as number),
    enabled: encounterId !== null,
  });
