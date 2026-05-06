"use client";

import { useQuery } from "@tanstack/react-query";
import {
  getMyNotifications,
  getWardNotifications,
} from "../api/notification";

const DEFAULT_LIMIT = 50;

export const useMyNotifications = () =>
  useQuery({
    queryKey: ["notifications", "me"] as const,
    queryFn: () => getMyNotifications({ limit: DEFAULT_LIMIT }),
  });

export const useWardNotifications = (wardId: number | null) =>
  useQuery({
    queryKey: ["notifications", "ward", wardId] as const,
    queryFn: () => getWardNotifications(wardId as number, { limit: DEFAULT_LIMIT }),
    enabled: wardId !== null,
  });
