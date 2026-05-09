"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { openSse } from "@/lib/sse";
import { useAuthStore } from "@/features/auth/stores/auth";

/**
 * 개인/병동 SSE 구독 + 새 알림 도착 시 캐시 invalidate.
 *
 * 백엔드 SourceType 5개 (self_report, iv_alert, timer, order_change, vital_alert) 가 이벤트 이름.
 * 실 발행 (origin/develop 검증): self_report · iv_alert (수액 종료 5분 전/종료) · order_change.
 * timer · vital_alert 는 enum 만 정의되고 dispatch 호출은 0건 (백엔드 트리거 합의 필요).
 * data 는 NotificationEnvelope 직렬화. 현 구현은 envelope 자체를 캐시에 prepend 하지 않고
 * REST 재조회를 트리거한다 — 알림함 응답(NotificationListItemResponse) 과 envelope 의 형태가 달라서
 * 우선 데이터 정합성을 우선 (envelope.payload 안에 표시용 필드가 들어간다는 백엔드 답이 오면 prepend 로 전환).
 *
 * iv_alert 가 도착하면 IV 캐시(["iv","ward",wardId,...]) 도 같이 invalidate 해 카드를 즉시 갱신/제거.
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

type SourceEventName = (typeof SOURCE_EVENTS)[number];

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
    const handler = (eventName: SourceEventName) => {
      queryClient.invalidateQueries({ queryKey: ["notifications", "me"] });
      if (eventName === "iv_alert" && wardId !== null) {
        queryClient.invalidateQueries({ queryKey: ["iv", "ward", wardId] });
      }
    };
    const onEvent = Object.fromEntries(
      SOURCE_EVENTS.map((name) => [name, () => handler(name)]),
    );
    return openSse("/sse/subscribe", { onEvent });
  }, [isLoggedIn, wardId, queryClient]);

  // 병동 채널 — 같은 ward 의 모든 알림. IV 캐시 갱신은 개인 채널이 담당하므로
  // 여기서는 알림 카운트만 갱신한다 (한 이벤트로 두 채널이 동시에 IV 캐시를 invalidate
  // 하면 staleTime 통과 시 /iv 가 두 번 fetch 되는 문제 회피).
  useEffect(() => {
    if (!isLoggedIn || wardId === null) return;
    if (isLocalhostDevelopment()) return;
    const handler = () => {
      queryClient.invalidateQueries({ queryKey: ["notifications", "ward", wardId] });
    };
    const onEvent = Object.fromEntries(
      SOURCE_EVENTS.map((name) => [name, handler]),
    );
    return openSse("/sse/ward-subscribe", { onEvent });
  }, [isLoggedIn, wardId, queryClient]);
}
