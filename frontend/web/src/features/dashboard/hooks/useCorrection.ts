"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { analyzeCorrections, applyCorrection } from "../api/correction";
import type {
  CorrectionApplyRequest,
  QuickCorrectionAnalyzeResponse,
} from "../types/correction";

/**
 * STT 행 본문에 대한 교정 후보 분석.
 *
 * - 수정 모드 진입 시 1회 호출 (사용자 결정 — 가벼운 정책).
 * - content 가 변해도 자동 재분석은 안 함. 사용자가 textarea 입력 후 다시 분석을 원하면 별도 트리거 필요.
 * - staleTime 길게 — 같은 본문은 재호출 의미 없음.
 */
export const useAnalyzeCorrections = (
  nursingRecordId: number | null,
  content: string,
  enabled: boolean,
) =>
  useQuery<QuickCorrectionAnalyzeResponse>({
    queryKey: ["correction", "analyze", nursingRecordId, content] as const,
    queryFn: () =>
      analyzeCorrections({
        nursing_record_id: nursingRecordId as number,
        content,
      }),
    enabled: enabled && nursingRecordId !== null && content.trim().length > 0,
    staleTime: Infinity,
    gcTime: 5 * 60_000,
    retry: false,
  });

/**
 * 사용자가 후보를 선택했을 때 피드백 저장.
 * 캐시 invalidate 안 함 — 본문 갱신은 호출 측 로컬 state 가 처리.
 */
export const useApplyCorrection = () =>
  useMutation({
    mutationFn: (request: CorrectionApplyRequest) => applyCorrection(request),
  });
