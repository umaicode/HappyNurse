"use client";

import { useQuery } from "@tanstack/react-query";
import { getPatientDetail } from "../api/patient-detail";

export const usePatientDetail = (patientId: number | null) =>
  useQuery({
    queryKey: ["patient", patientId] as const,
    queryFn: () => getPatientDetail(patientId as number),
    enabled: patientId !== null,
  });
