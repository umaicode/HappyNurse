"use client";

import { useQuery } from "@tanstack/react-query";
import { getOrganizations, getWards } from "../api/organization";

export const useOrganizations = () =>
  useQuery({
    queryKey: ["organizations"],
    queryFn: getOrganizations,
    staleTime: Infinity,
  });

export const useWards = (organizationId: number | null) =>
  useQuery({
    queryKey: ["organizations", organizationId, "wards"],
    queryFn: () => getWards(organizationId as number),
    enabled: organizationId !== null,
    staleTime: Infinity,
  });
