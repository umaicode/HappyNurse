"use client";

import { useQuery } from "@tanstack/react-query";
import { getNursingNotes } from "../api/nursing-note";

export const useNursingNotes = (
  encounterId: number | null,
  // ISO date (yyyy-MM-dd) — 백엔드 필수 파라미터
  date: string | null,
) =>
  useQuery({
    queryKey: ["encounter", encounterId, "nursing-notes", date] as const,
    queryFn: () => getNursingNotes(encounterId as number, date as string),
    enabled: encounterId !== null && date !== null,
  });
