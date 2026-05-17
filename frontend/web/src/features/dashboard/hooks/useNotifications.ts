"use client";

import { useQuery } from "@tanstack/react-query";
import {
  getMyNotifications,
  getWardNotifications,
} from "../api/notification";
import {
  HIDDEN_SOURCE_TYPES,
  type NotificationListResponse,
} from "../types/notification";

const DEFAULT_LIMIT = 50;

const filterHidden = (
  data: NotificationListResponse,
): NotificationListResponse => ({
  ...data,
  items: data.items.filter((item) => !HIDDEN_SOURCE_TYPES.has(item.sourceType)),
});

export const useMyNotifications = () =>
  useQuery({
    queryKey: ["notifications", "me"] as const,
    queryFn: () => getMyNotifications({ limit: DEFAULT_LIMIT }),
    select: filterHidden,
    staleTime: 0,
  });

export const useWardNotifications = (wardId: number | null) =>
  useQuery({
    queryKey: ["notifications", "ward", wardId] as const,
    queryFn: () => getWardNotifications(wardId as number, { limit: DEFAULT_LIMIT }),
    enabled: wardId !== null,
    select: filterHidden,
    staleTime: 0,
  });
