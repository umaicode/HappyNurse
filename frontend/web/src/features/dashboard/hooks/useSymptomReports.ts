"use client";

import { useQuery } from "@tanstack/react-query";
import { getSymptomReports } from "../api/symptom-report";

export const useSymptomReports = (patientId: number | null) =>
  useQuery({
    queryKey: ["patient", patientId, "symptoms"] as const,
    queryFn: () => getSymptomReports(patientId as number),
    enabled: patientId !== null,
  });
