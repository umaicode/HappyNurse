"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  generateHandover,
  getHandoverDetail,
  getRosterSummary,
} from "../api/handover";
import { openSse } from "@/lib/sse";
import type {
  HandoverDetailResponse,
  HandoverJobResponse,
  RosterSummary,
  SseCompletePayload,
  SseErrorPayload,
  SseRosterSummaryPayload,
  SseStartedPayload,
} from "../types/handover";

export const HANDOVER_ROSTER_KEY = ["handover", "roster-summary"] as const;
export const handoverDetailKey = (handoverId: string | null) =>
  ["handover", "detail", handoverId] as const;

/**
 * 진입 시 즉석 narrative + 환자 brief.
 * BE 측 30분 캐시가 있어 staleTime 길게 둬도 부담 없음. 강제 갱신은 invalidate.
 */
export const useRosterSummary = () =>
  useQuery<RosterSummary>({
    queryKey: HANDOVER_ROSTER_KEY,
    queryFn: getRosterSummary,
    staleTime: 60_000,
  });

/**
 * 환자 카드 클릭 시 PASS-BAR 풀 페이로드. handoverId 없으면 비활성.
 */
export const useHandoverDetail = (handoverId: string | null) =>
  useQuery<HandoverDetailResponse>({
    queryKey: handoverDetailKey(handoverId),
    queryFn: () => getHandoverDetail(handoverId as string),
    enabled: handoverId !== null,
    staleTime: 60_000,
  });

/**
 * "리포트 생성" 버튼.
 * 응답으로 job_id 받음. 호출 측이 job_id 로 useHandoverStream 구독.
 */
export const useGenerateHandover = () =>
  useMutation<HandoverJobResponse, Error, void>({
    mutationFn: generateHandover,
  });

/**
 * 인수인계 generate 진행 SSE 추적.
 *
 * 이벤트:
 *   - started               { encounter_id }
 *   - complete              { encounter_id, handover_id, verification_summary }
 *   - error                 { encounter_id, reason, fallback_handover_id }
 *   - roster_summary        RosterSummary 구조
 *   - job_done              빈 데이터
 *
 * job_done 도착 시 자동으로 roster-summary 캐시 invalidate (BE 캐시도 generate 시 무효화됨).
 */
export interface HandoverProgress {
  // 시작된 환자 수
  startedCount: number;
  // 완료된 환자 수
  completeCount: number;
  // 에러 환자 수
  errorCount: number;
  // 마지막 도착한 roster_summary (BE 가 emit 한 최종 결과)
  rosterSummary: SseRosterSummaryPayload | null;
  // 작업 완료 여부
  done: boolean;
  // 연결 자체 에러 (네트워크/서버)
  connectionError: boolean;
}

const INITIAL_PROGRESS: HandoverProgress = {
  startedCount: 0,
  completeCount: 0,
  errorCount: 0,
  rosterSummary: null,
  done: false,
  connectionError: false,
};

export const useHandoverStream = (jobId: string | null) => {
  const [progress, setProgress] = useState<HandoverProgress>(INITIAL_PROGRESS);
  const queryClient = useQueryClient();

  useEffect(() => {
    if (jobId === null) return;
    // 새 job 시작 — progress 초기화. jobId deps 가 변할 때만 effect 가 다시 마운트되므로
    // 여기서 set 해도 cascading 이 아니라 새 cycle 의 초기화에 해당.
    /* eslint-disable-next-line react-hooks/set-state-in-effect */
    setProgress(INITIAL_PROGRESS);

    const cleanup = openSse(`/api/handover/stream/${jobId}`, {
      variant: "ai",
      onEvent: {
        started: (event) => {
          parseEvent<SseStartedPayload>(event.data, () => {
            setProgress((previous) => ({
              ...previous,
              startedCount: previous.startedCount + 1,
            }));
          });
        },
        complete: (event) => {
          parseEvent<SseCompletePayload>(event.data, () => {
            setProgress((previous) => ({
              ...previous,
              completeCount: previous.completeCount + 1,
            }));
          });
        },
        error: (event) => {
          parseEvent<SseErrorPayload>(event.data, () => {
            setProgress((previous) => ({
              ...previous,
              errorCount: previous.errorCount + 1,
            }));
          });
        },
        roster_summary: (event) => {
          parseEvent<SseRosterSummaryPayload>(event.data, (data) => {
            setProgress((previous) => ({
              ...previous,
              rosterSummary: data,
            }));
            // 즉시 화면에 반영 — useQuery 의 다음 fetch 가 새 narrative 를 받아온다.
            queryClient.setQueryData<RosterSummary>(HANDOVER_ROSTER_KEY, data);
          });
        },
        job_done: () => {
          setProgress((previous) => ({ ...previous, done: true }));
          queryClient.invalidateQueries({ queryKey: HANDOVER_ROSTER_KEY });
        },
      },
      onError: () => {
        setProgress((previous) => ({ ...previous, connectionError: true }));
      },
    });

    return cleanup;
  }, [jobId, queryClient]);

  return progress;
};

// 함수 이름이 'use' 로 시작하면 React Hook 으로 오해되어 lint 에러. parseEvent 로.
const parseEvent = <T,>(raw: string, apply: (data: T) => void) => {
  try {
    apply(JSON.parse(raw) as T);
  } catch {
    // 파싱 실패는 무시 — BE 가 빈 데이터 ("") 보낼 수 있다.
  }
};
