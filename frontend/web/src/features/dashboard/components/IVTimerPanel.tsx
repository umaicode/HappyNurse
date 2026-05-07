"use client";

import { useEffect, useMemo, useState } from "react";
import { Droplet } from "lucide-react";
import { cn } from "@/lib/utils";
import { formatHHmm } from "@/lib/time";
import { useAuthStore } from "@/features/auth/stores/auth";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import { useWardIvInfusions } from "../hooks/useIvInfusions";
import type { IvInfusionListItem } from "../types/iv-infusion";
import { PanelCard } from "./PanelCard";

const TICK_INTERVAL_MS = 60_000;
const WARNING_REMAINING_MS = 60 * 60 * 1000;

function formatDuration(milliseconds: number): string {
  const totalMinutes = Math.max(0, Math.floor(milliseconds / 60_000));
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  if (hours === 0) return `${minutes}분`;
  if (minutes === 0) return `${hours}시간`;
  return `${hours}시간 ${minutes}분`;
}

function useNowTick(intervalMs: number): number {
  const [now, setNow] = useState<number>(() => Date.now());
  useEffect(() => {
    const handle = setInterval(() => setNow(Date.now()), intervalMs);
    return () => clearInterval(handle);
  }, [intervalMs]);
  return now;
}

export function IVTimerPanel() {
  const wardId = useAuthStore((state) => state.user?.wardId ?? null);
  const {
    data: ivInfusions,
    isPending,
    isError,
    error,
    fetchStatus,
  } = useWardIvInfusions(wardId, "IN_PROGRESS");
  const { data: wardPatients } = useWardPatients();
  const now = useNowTick(TICK_INTERVAL_MS);

  // patientId → roomName join (slim 응답엔 호실 정보 없음. 백엔드 합의 시 응답에 추가되면 이 join 제거)
  const roomByPatientId = useMemo(() => {
    const map = new Map<number, string>();
    wardPatients?.forEach((patient) => {
      map.set(patient.patientId, patient.roomName);
    });
    return map;
  }, [wardPatients]);

  // 임박 순 (남은 시간 짧은 게 위로)
  const sortedItems = useMemo<IvInfusionListItem[]>(
    () =>
      [...(ivInfusions ?? [])].sort((a, b) => {
        const aEnd = new Date(a.expectedEndAt).getTime();
        const bEnd = new Date(b.expectedEndAt).getTime();
        return aEnd - bEnd;
      }),
    [ivInfusions],
  );

  // 빈/로딩/오류 상태 분기 — 어디서 막혔는지 즉시 화면으로 식별 가능하도록.
  // 우선순위: wardId 없음 > 에러 > 로딩 > 빈 응답.
  const emptyState = (() => {
    if (wardId === null) {
      return { label: "병동 정보 없음", subtle: true };
    }
    if (isError) {
      return {
        label: `조회 실패${error instanceof Error ? ` — ${error.message}` : ""}`,
        subtle: false,
      };
    }
    // fetchStatus === 'idle' 인데 isPending 이면 enabled 가 false 인 경우. wardId !== null 가드 후엔 발생 X.
    if (isPending && fetchStatus === "fetching") {
      return { label: "수액 목록 조회 중...", subtle: true };
    }
    if (sortedItems.length === 0) {
      return { label: "진행 중인 수액 없음", subtle: true };
    }
    return null;
  })();

  if (emptyState) {
    return (
      <div className="flex flex-col h-full bg-surface-base">
        <div className="flex-1 overflow-y-auto p-2 flex flex-col gap-2.5">
          <div
            className={cn(
              "flex flex-col items-center justify-center py-10 gap-2",
              emptyState.subtle
                ? "text-content-muted opacity-30"
                : "text-status-danger",
            )}
          >
            <Droplet className="w-6 h-6" />
            <p className="text-[11px]">{emptyState.label}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-surface-base">
      <div className="flex-1 overflow-y-auto p-2 flex flex-col gap-2.5">
        {sortedItems.map((item) => {
          const startedAtMs = new Date(item.startedAt).getTime();
          const endMs = new Date(item.expectedEndAt).getTime();
          const remainingMs = Math.max(0, endMs - now);
          const isWarning = remainingMs <= WARNING_REMAINING_MS;
          const totalMs = Math.max(1, endMs - startedAtMs);
          const elapsedMs = Math.max(0, now - startedAtMs);
          const progressPercent = Math.min(100, (elapsedMs / totalMs) * 100);

          const room = roomByPatientId.get(item.patientId) ?? "";
          const fluidLabel = item.medicationNames.join(" + ");

          return (
            <PanelCard key={item.ivInfusionId}>
              {/* 1행: 환자명 + 호실 (좌) | 교체 임박 칩 (우) */}
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-baseline gap-2 min-w-0">
                  <span className="text-body-sm font-bold text-content-primary truncate leading-none">
                    {item.patientName}
                  </span>
                  {room && (
                    <span className="text-body-micro font-mono font-medium text-content-tertiary shrink-0 leading-none">
                      {room}
                    </span>
                  )}
                </div>
                {isWarning && (
                  <span className="px-1.5 py-0.5 rounded bg-status-warning text-white text-body-micro font-bold leading-none shrink-0">
                    교체 임박
                  </span>
                )}
              </div>

              {/* 수액 이름 (혼합 시 medicationNames join) */}
              <span className="text-body-sm font-bold text-content-primary truncate">
                {fluidLabel}
              </span>

              {/* 진행 막대바 — startedAt → expectedEndAt 구간 기준 elapsed/total */}
              <div className="h-1.5 w-full rounded-full bg-surface-hover overflow-hidden">
                <div
                  className={cn(
                    "h-full rounded-full transition-all",
                    isWarning ? "bg-status-warning" : "bg-status-active",
                  )}
                  style={{ width: `${progressPercent}%` }}
                />
              </div>

              {/* 시간 정보 — 남은 시간 + 종료 예정 */}
              <div className="flex flex-col gap-0.5 text-body-sm">
                <div
                  className={cn(
                    isWarning ? "text-status-warning" : "text-content-secondary",
                  )}
                >
                  남은 시간{" "}
                  <span className="font-medium">{formatDuration(remainingMs)}</span>
                </div>
                <div
                  className={cn(
                    isWarning ? "text-status-warning" : "text-content-tertiary",
                  )}
                >
                  종료 예정{" "}
                  <span className="font-mono font-medium">
                    {formatHHmm(item.expectedEndAt)}
                  </span>
                </div>
              </div>
            </PanelCard>
          );
        })}
      </div>
    </div>
  );
}
