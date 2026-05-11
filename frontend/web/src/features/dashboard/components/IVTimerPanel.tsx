"use client";

import { useEffect, useMemo, useState } from "react";
import { isSameDay } from "date-fns";
import { cn } from "@/lib/utils";
import { formatHHmm, formatMonthDayHHmm } from "@/lib/time";
import { useAuthStore } from "@/features/auth/stores/auth";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import { useWardIvInfusions } from "../hooks/useIvInfusions";
import { DROP_SET_LABEL, type IvInfusionListItem } from "../types/iv-infusion";
import { PanelCard } from "./PanelCard";

const TICK_INTERVAL_MS = 60_000;
// 잔여 5분 미만이면 게이지 색을 danger 로 override — iv_alert SSE 5분 전 발행 시점과 align.
const CRITICAL_REMAINING_MS = 5 * 60 * 1000;

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

interface RoomBedInfo {
  roomBed: string;
  isMyPatient: boolean;
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

  // patientId → 호실-침대 + 담당여부 join. slim 응답엔 호실/담당 정보 없어 wardPatients 기준.
  // 호실 표시는 EMRGrid 헤더의 buildHeaderFromApi 와 동일한 "{roomName-호}-{bedName}" 패턴으로 통일.
  const infoByPatientId = useMemo(() => {
    const map = new Map<number, RoomBedInfo>();
    wardPatients?.forEach((patient) => {
      const roomBed = [
        patient.roomName.replace(/호$/, ""),
        patient.bedName,
      ]
        .filter(Boolean)
        .join("-");
      map.set(patient.patientId, {
        roomBed,
        isMyPatient: patient.isMyPatient,
      });
    });
    return map;
  }, [wardPatients]);

  // 내 담당 환자만 표시 — wardPatients 의 isMyPatient flag 로 필터.
  // wardPatients 가 아직 안 들어왔으면 IV 도 보이지 않게 (정합성 우선) — 짧은 깜빡임 정도라 사용성에 문제 없음.
  const sortedItems = useMemo<IvInfusionListItem[]>(
    () =>
      [...(ivInfusions ?? [])]
        .filter((iv) => infoByPatientId.get(iv.patientId)?.isMyPatient)
        .sort((a, b) => {
          const aEnd = new Date(a.expectedEndAt).getTime();
          const bEnd = new Date(b.expectedEndAt).getTime();
          return aEnd - bEnd;
        }),
    [ivInfusions, infoByPatientId],
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
              "flex flex-col items-center justify-center py-10",
              emptyState.subtle
                ? "text-content-muted opacity-30"
                : "text-status-danger",
            )}
          >
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
          const totalMs = Math.max(1, endMs - startedAtMs);
          const elapsedMs = Math.max(0, now - startedAtMs);
          const progressPercent = Math.min(100, (elapsedMs / totalMs) * 100);

          // 게이지 색 — 모바일 IVTimerCard.kt 와 동일한 경과율 기준 50%/80% 3단계.
          // 단 잔여 5분 미만이면 무조건 danger override (iv_alert SSE 발행 시점과 align).
          // 텍스트는 의사오더/알림 카드와 통일된 차분 톤으로 — 사용자 안전 강조는 게이지가 담당.
          const isCritical = remainingMs < CRITICAL_REMAINING_MS;
          const barColorClass = isCritical
            ? "bg-status-danger"
            : progressPercent < 50
              ? "bg-status-active"
              : progressPercent < 80
                ? "bg-status-warning"
                : "bg-status-danger";

          const info = infoByPatientId.get(item.patientId);
          const roomBed = info?.roomBed ?? "";
          const fluidLabel = item.medicationNames.join(" + ");

          // 종료 시각 — 같은 날이면 시간만, 다음 날 넘어가면 "M/D HH:mm".
          const endIsSameDay = isSameDay(new Date(item.expectedEndAt), now);
          const endLabel = endIsSameDay
            ? formatHHmm(item.expectedEndAt)
            : formatMonthDayHHmm(item.expectedEndAt);

          return (
            <PanelCard key={item.ivInfusionId}>
              {/* 1행: 환자명 + 호실-침대 칩 (좌) | 종료 시각 (우) — 모바일 BarCard 정렬 */}
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2 min-w-0">
                  <span className="text-body-sm font-bold text-content-primary leading-none break-words">
                    {item.patientName}
                  </span>
                  {roomBed && (
                    <span className="px-1.5 py-0.5 rounded bg-[#F7F8FA] text-content-secondary text-[11px] font-bold leading-none shrink-0">
                      {roomBed}
                    </span>
                  )}
                </div>
                {/* STTPanel/PatientAlerts 카드의 시간 표시와 동일 — text-body-xs font-medium text-content-tertiary.
                    잔여시간/진행률 강조는 4행 잔여시간 텍스트 + 게이지 색이 담당. */}
                <span className="text-body-xs font-medium text-content-tertiary shrink-0 leading-none">
                  종료 {endLabel}
                </span>
              </div>

              {/* 2행: 수액 이름 — 길면 다음 줄로 (truncate 금지) */}
              <span className="text-body-sm font-bold text-content-primary leading-snug break-words">
                {fluidLabel}
              </span>

              {/* 2.5행: 주입 속도 + 세트 — BE 5/11 IvInfusionListItemResponse 확장분 (rateGttPerMin · dropSet).
                  마이그레이션 누락 row 는 둘 다 null 가능 → mL/hr 만 노출. */}
              <span className="text-body-micro font-medium text-content-tertiary leading-none">
                <span className="font-mono">
                  {item.currentRateMlPerHr}
                </span>{" "}
                mL/hr
                {item.rateGttPerMin !== null && (
                  <>
                    {" · "}
                    <span className="font-mono">{item.rateGttPerMin}</span>
                    {" gtt/min"}
                  </>
                )}
                {item.dropSet && (
                  <>
                    {" · "}
                    {DROP_SET_LABEL[item.dropSet]}
                  </>
                )}
              </span>

              {/* 3행: 진행 막대바 — startedAt → expectedEndAt 구간 기준 elapsed/total */}
              <div className="h-1.5 w-full rounded-full bg-surface-hover overflow-hidden">
                <div
                  className={cn("h-full rounded-full transition-all", barColorClass)}
                  style={{ width: `${progressPercent}%` }}
                />
              </div>

              {/* 4행: 잔여 시간 — 종료시각과 동일하게 차분한 톤. 사용자 안전 강조는 게이지 색 + 1행 종료시각이 담당. */}
              <span className="text-body-xs font-medium text-content-tertiary leading-none">
                남은 시간{" "}
                <span className="font-mono">{formatDuration(remainingMs)}</span>
              </span>
            </PanelCard>
          );
        })}
      </div>
    </div>
  );
}
