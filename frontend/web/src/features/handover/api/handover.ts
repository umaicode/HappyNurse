/**
 * 인수인계 AI API.
 *
 * - generateHandover() — POST /api/handover/generate (비동기 생성, job_id 반환)
 * - getRosterSummary() — GET  /api/handover/roster-summary (진입 시 narrative + brief)
 * - getHandoverDetail(handoverId) — GET /api/handover/{handover_id} (PASS-BAR 풀)
 *
 * 미사용 (D 섹션 참고):
 *   - /stream/{job_id} — useHandoverStream hook 에서 EventSource 로 직접 구독
 *   - /freshness — RosterPatientItem.freshness 에 이미 포함
 *   - GET /api/handover?encounter_id= — handover_id 단건 조회로 대체
 *   - /regenerate — Phase 2
 */
import { client } from "@/lib/client";
import { aiClient } from "@/lib/ai-client";
import type {
  HandoverChecksPatch,
  HandoverChecksResponse,
  HandoverDetailResponse,
  HandoverJobResponse,
  HandoverPayload,
  RosterSummary,
  WardEventsResponse,
} from "../types/handover";

export const generateHandover = (): Promise<HandoverJobResponse> =>
  aiClient
    .post("/api/handover/generate")
    .then((response) => ({ jobId: String(response.data?.job_id ?? "") }));

export const getRosterSummary = (): Promise<RosterSummary> =>
  aiClient.get("/api/handover/roster-summary").then((response) => response.data);

// auto_summary_json 은 jsonb 라 BE 가 string 또는 object 로 내려줄 수 있다 — 한 번 normalize.
const normalizeAutoSummaryJson = (
  value: unknown,
): HandoverPayload | null => {
  if (value === null || value === undefined) return null;
  if (typeof value === "string") {
    try {
      return JSON.parse(value) as HandoverPayload;
    } catch {
      return null;
    }
  }
  return value as HandoverPayload;
};

export const getHandoverDetail = (
  handoverId: string,
): Promise<HandoverDetailResponse> =>
  aiClient
    .get(`/api/handover/${handoverId}`)
    .then((response) => {
      const data = response.data ?? {};
      return {
        handoverId: String(data.handover_id ?? handoverId),
        encounterId: String(data.encounter_id ?? ""),
        autoSummary: data.auto_summary ?? null,
        autoSummaryJson: normalizeAutoSummaryJson(data.auto_summary_json),
        createdAt: String(data.created_at ?? ""),
      };
    });

// 체크리스트 상태 조회 — BE (`/handover/{handoverId}`). PASS-BAR 풀 본체는 위 AI detail 이 담당.
export const getHandoverChecks = (
  handoverId: string,
): Promise<HandoverChecksResponse> =>
  client.get(`/handover/${handoverId}`).then((response) => {
    const data = response.data ?? {};
    return {
      handoverId: String(data.handoverId ?? handoverId),
      checkedItemsJson:
        (data.checkedItemsJson as HandoverChecksResponse["checkedItemsJson"]) ??
        {},
    };
  });

// 체크 토글 — 델타 방식. body 에 안 담은 키는 BE 가 그대로 둠.
export const patchHandoverChecks = (
  handoverId: string,
  checks: HandoverChecksPatch,
): Promise<void> =>
  client
    .patch(`/handover/${handoverId}/checks`, { checks })
    .then(() => undefined);

// 오늘 우리 병동의 입퇴원 환자 목록 (BE). 인수인계 화면 상단 "교대 통합 요약" 박스 자리 대체.
export const getWardEvents = (): Promise<WardEventsResponse> =>
  client.get("/handover/ward-events").then((response) => {
    const data = response.data ?? {};
    return {
      admissions: data.admissions ?? [],
      discharges: data.discharges ?? [],
    };
  });
