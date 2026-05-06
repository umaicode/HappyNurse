"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { openSse } from "@/lib/sse";
import { useAuthStore } from "@/features/auth/stores/auth";

/**
 * 개인/병동 SSE 구독 + 새 알림 도착 시 알림 캐시 invalidate.
 *
 * 백엔드 SourceType 5개 (self_report, iv_alert, timer, order_change, vital_alert) 가 이벤트 이름.
 * data 는 NotificationEnvelope 직렬화. 현 구현은 envelope 자체를 캐시에 prepend 하지 않고
 * REST 재조회를 트리거한다 — 알림함 응답(NotificationListItemResponse) 과 envelope 의 형태가 달라서
 * 우선 데이터 정합성을 우선 (envelope.payload 안에 표시용 필드가 들어간다는 백엔드 답이 오면 prepend 로 전환).
 *
 * localhost 에서는 SSE 구독을 skip 한다 — Next.js dev rewrite (/api-proxy) 가 SSE 같은 long-lived
 * chunked stream 을 적절히 프록시하지 못해 socket hang up 이 발생한다. 배포 환경(same-origin)에선 정상 동작.
 */

const SOURCE_EVENTS = [
  "self_report",
  "iv_alert",
  "timer",
  "order_change",
  "vital_alert",
] as const;

const isLocalhostDevelopment = () =>
  typeof window !== "undefined" &&
  window.location.hostname === "localhost";

export function useNotificationStream() {
  const queryClient = useQueryClient();
  const isLoggedIn = useAuthStore((state) => state.user !== null);
  const wardId = useAuthStore((state) => state.user?.wardId ?? null);

  // 개인 채널 — 본인 담당 환자에 대한 알림만.
  useEffect(() => {
    if (!isLoggedIn) return;
    if (isLocalhostDevelopment()) return;
    const handler = () => {
      queryClient.invalidateQueries({ queryKey: ["notifications", "me"] });
    };
    const onEvent = Object.fromEntries(SOURCE_EVENTS.map((name) => [name, handler]));
    return openSse("/sse/subscribe", { onEvent });
  }, [isLoggedIn, queryClient]);

  // 병동 채널 — 같은 ward 의 모든 알림.
  useEffect(() => {
    if (!isLoggedIn || wardId === null) return;
    if (isLocalhostDevelopment()) return;
    const handler = () => {
      queryClient.invalidateQueries({ queryKey: ["notifications", "ward", wardId] });
    };
    const onEvent = Object.fromEntries(SOURCE_EVENTS.map((name) => [name, handler]));
    return openSse("/sse/ward-subscribe", { onEvent });
  }, [isLoggedIn, wardId, queryClient]);
}
