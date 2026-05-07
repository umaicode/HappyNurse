"use client";

import { useQuery } from "@tanstack/react-query";
import { getWardIvInfusions } from "../api/iv-infusion";
import type { IvStatus } from "../types/iv-infusion";

export const wardIvInfusionsQueryKey = (
  wardId: number | null,
  status?: IvStatus,
) => ["iv", "ward", wardId, status ?? "all"] as const;

/**
 * 병동 IV 목록 조회.
 *
 * - 30초 폴링: 새 IV 시작/속도 변경에는 SSE 트리거가 없어 (현재는 종료 5분 전·종료 시점만 발행) 폴링이 빈틈을 메운다.
 * - SSE 발행 sourceType (iv_alert) 이 도착하면 useNotificationStream 이 이 쿼리를 invalidate 한다.
 */
export const useWardIvInfusions = (wardId: number | null, status?: IvStatus) =>
  useQuery({
    queryKey: wardIvInfusionsQueryKey(wardId, status),
    queryFn: () => getWardIvInfusions(wardId as number, status),
    enabled: wardId !== null,
    staleTime: 10_000,
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
  });
