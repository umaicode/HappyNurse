/**
 * SSE 연결 헬퍼.
 *
 * Next.js rewrite (`/api-proxy`) 또는 절대 URL 로 EventSource 를 열어 same-origin 쿠키 인증을 그대로 쓴다.
 * (lib/client.ts 의 baseURL 분기와 동일 규칙)
 *
 * 백엔드 KeyedEmitterRegistry.send() 형식:
 *   event: <SourceType.name()>      (또는 'heartbeat')
 *   id:    <notificationId>          (영속화 후 채워짐, 없으면 빈 문자열)
 *   data:  <NotificationEnvelope JSON>  ('heartbeat' 면 "ping")
 */

const resolveSseUrl = (path: string): string => {
  if (typeof window === "undefined") return path;
  if (window.location.hostname === "localhost") {
    return `/api-proxy${path}`;
  }
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";
  return `${base}${path}`;
};

// AI 서버 (FastAPI) SSE 용 — baseURL 다름. lib/ai-client.ts 의 분기 규칙과 동일.
const resolveAiSseUrl = (path: string): string => {
  if (typeof window === "undefined") return path;
  if (window.location.hostname === "localhost") {
    return `/ai-proxy${path}`;
  }
  const base = process.env.NEXT_PUBLIC_AI_BASE_URL ?? "";
  return `${base}${path}`;
};

export type SseEventHandler = (event: MessageEvent<string>) => void;

export interface OpenSseOptions {
  // sourceType 별 핸들러 (예: { self_report: handler })
  onEvent: Record<string, SseEventHandler>;
  // 연결 자체 에러 (서버 종료 / 네트워크). 브라우저가 자동 재연결을 시도한다.
  onError?: (event: Event) => void;
}

/**
 * SSE 채널 구독. cleanup 함수를 반환한다 (useEffect 의 return 에 그대로 둘 것).
 *
 * heartbeat 는 자동 무시 — 호출자는 onEvent 에 'heartbeat' 키를 둘 필요 없다.
 *
 * variant:
 *   - "backend" (기본): 백엔드 (`/api`) SSE — sourceType 별 알림 채널
 *   - "ai": AI 서버 (`/ai`) SSE — 인수인계 generate 진행 추적 등
 */
export function openSse(
  path: string,
  options: OpenSseOptions & { variant?: "backend" | "ai" } = { onEvent: {} },
): () => void {
  const variant = options.variant ?? "backend";
  const url = variant === "ai" ? resolveAiSseUrl(path) : resolveSseUrl(path);
  const source = new EventSource(url, {
    withCredentials: true,
  });

  // heartbeat 는 항상 무시 (좀비 정리/keepalive 용).
  const heartbeatListener: SseEventHandler = () => {};
  source.addEventListener("heartbeat", heartbeatListener as EventListener);

  // 등록된 sourceType 별 listener 부착.
  const registered: Array<[string, EventListener]> = [];
  for (const [eventName, handler] of Object.entries(options.onEvent)) {
    if (eventName === "heartbeat") continue;
    const listener = handler as EventListener;
    source.addEventListener(eventName, listener);
    registered.push([eventName, listener]);
  }

  if (options.onError) {
    source.addEventListener("error", options.onError);
  }

  return () => {
    source.removeEventListener("heartbeat", heartbeatListener as EventListener);
    for (const [eventName, listener] of registered) {
      source.removeEventListener(eventName, listener);
    }
    if (options.onError) {
      source.removeEventListener("error", options.onError);
    }
    source.close();
  };
}
