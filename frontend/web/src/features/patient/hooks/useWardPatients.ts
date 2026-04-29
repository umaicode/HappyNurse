"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { assignMyPatients, getWardPatients } from "../api/ward-patient";

export const wardPatientsQueryKey = ["ward", "me", "patients"] as const;

export const useWardPatients = () =>
  useQuery({
    queryKey: wardPatientsQueryKey,
    queryFn: getWardPatients,
  });

export const useAssignMyPatients = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: assignMyPatients,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: wardPatientsQueryKey });
    },
  });
};
