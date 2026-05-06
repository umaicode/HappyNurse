/**
 * 알림함 API.
 *
 * - [간호사용 웹] getMyNotifications(params) — GET /notifications/me  (본인 수신 알림)
 * - [간호사용 웹] getWardNotifications(wardId, params) — GET /notifications  (병동 전체 알림)
 *
 * cursor 페이지네이션:
 *   - since: 이 이후 (ISO datetime, 새 알림 폴링)
 *   - before: 이 PK 이전 (이전 페이지 더 받기)
 *   - limit: 1~100 (default 20)
 */
import { client } from "@/lib/client";
import type { NotificationListResponse } from "../types/notification";

export interface NotificationQueryParams {
  since?: string;
  before?: number;
  limit?: number;
}

export const getMyNotifications = (
  params: NotificationQueryParams = {},
): Promise<NotificationListResponse> =>
  client.get("/notifications/me", { params }).then((response) => response.data);

export const getWardNotifications = (
  wardId: number,
  params: NotificationQueryParams = {},
): Promise<NotificationListResponse> =>
  client
    .get("/notifications", { params: { wardId, ...params } })
    .then((response) => response.data);
