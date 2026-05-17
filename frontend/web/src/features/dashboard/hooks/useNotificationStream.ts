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

// EMR 간호기록 갱신용 — Notification DB 미저장이라 알림함 카운트와 무관.
// BE 는 ward 채널로만 발사 (NursingRecordSseService / MedicationAdministrationSseService).
// 두 이벤트 모두 동일한 NursingNoteItemResponse payload (type=STT_NOTE | MEDICATION) 라 같은 invalidate 로직을 공유한다.
const NURSING_EVENT = "nursing_record";
const MEDICATION_ADMIN_EVENT = "medication_admin";

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
  //
  // nursing_record / medication_admin 은 의미가 다르다 — 알림함이 아니라 EMR 간호기록 그리드 갱신용.
  // BE 는 Notification DB 에 저장하지 않고 ward 채널로만 발사한다 (notificationId: null).
  useEffect(() => {
    if (!isLoggedIn || wardId === null) return;
    if (isLocalhostDevelopment()) return;

    const notificationHandler = () => {
      queryClient.invalidateQueries({ queryKey: ["notifications", "ward", wardId] });
    };

    // useNursingNotes 의 queryKey: ["encounter", encounterId, "nursing-notes", date]
    // SSE payload 에 encounterId 가 없어 predicate 로 prefix 매칭한다.
    // useDraftNursingNotes (["encounter", id, "nursing-notes", "drafts"]) /
    // useMonthNursingCounts (날짜별 동일 prefix) 도 함께 갱신됨.
    const nursingHandler = () => {
      queryClient.invalidateQueries({
        predicate: (query) =>
          query.queryKey[0] === "encounter" && query.queryKey[2] === "nursing-notes",
      });
    };

    // useOrders 의 queryKey: ["encounter", encounterId, "orders"]
    // EMRGrid 의 OrderTab + RightPanel 의 STTPanel(사이드바 의사오더 탭) 이 같은 캐시 공유 → 한 번에 갱신됨.
    // ward 채널에서만 처리 — 개인 채널에서도 invalidate 하면 한 이벤트로 두 번 fetch 됨.
    const orderHandler = () => {
      queryClient.invalidateQueries({
        predicate: (query) =>
          query.queryKey[0] === "encounter" && query.queryKey[2] === "orders",
      });
    };

    // order_change 는 알림 카운트(notificationHandler) + 오더 캐시(orderHandler) 동시 갱신.
    const orderChangeHandler = () => {
      notificationHandler();
      orderHandler();
    };

    const onEvent = {
      ...Object.fromEntries(SOURCE_EVENTS.map((name) => [name, notificationHandler])),
      order_change: orderChangeHandler,
      [NURSING_EVENT]: nursingHandler,
      [MEDICATION_ADMIN_EVENT]: nursingHandler,
    };

    return openSse("/sse/ward-subscribe", { onEvent });
  }, [isLoggedIn, wardId, queryClient]);
}
