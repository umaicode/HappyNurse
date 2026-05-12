'use client'

import { useMemo } from "react";
import { Info, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { PanelCard } from "./PanelCard";
import { useMyNotifications } from "../hooks/useNotifications";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import {
  PRIORITY_BORDER,
  PRIORITY_CHIP,
  PRIORITY_LABEL,
  SOURCE_TYPE_BORDER,
  SOURCE_TYPE_LABEL,
  SOURCE_TYPE_TONE,
  type NotificationListItem,
  type SourceType,
  type SymptomPriority,
} from "../types/notification";
import { formatRelativeTime } from "@/lib/time";

export function PatientAlerts() {
  const { data, isPending, isError } = useMyNotifications();
  const { data: wardPatients } = useWardPatients();

  // patientId → 호실-침대 join — slim 알림 응답엔 호실 정보 없음.
  const roomBedByPatientId = useMemo(() => {
    const map = new Map<number, string>();
    wardPatients?.forEach((patient) => {
      const roomBed = [
        patient.roomName.replace(/호$/, ""),
        patient.bedName,
      ]
        .filter(Boolean)
        .join("-");
      if (roomBed) map.set(patient.patientId, roomBed);
    });
    return map;
  }, [wardPatients]);

  // createdAt desc — 최신이 위. 정렬은 시간순 고정 (요구사항).
  const sorted = useMemo<NotificationListItem[]>(() => {
    const items = data?.items ?? [];
    return [...items].sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }, [data]);

  return (
    <div className="flex flex-col h-full bg-surface-base">
      <div className="flex-1 overflow-y-auto p-2 flex flex-col gap-2.5">
        {isPending ? (
          <LoadingState />
        ) : isError ? (
          <EmptyState>알림을 불러오지 못했습니다</EmptyState>
        ) : sorted.length === 0 ? (
          <EmptyState>표시할 알림 없음</EmptyState>
        ) : (
          sorted.map((alert) => (
            <NotificationCard
              key={alert.notificationId}
              alert={alert}
              roomBed={
                alert.patientId !== null
                  ? roomBedByPatientId.get(alert.patientId) ?? ""
                  : ""
              }
            />
          ))
        )}
      </div>
    </div>
  );
}

function NotificationCard({
  alert,
  roomBed,
}: {
  alert: NotificationListItem;
  roomBed: string;
}) {
  const sourceType = alert.sourceType as SourceType;
  const label = SOURCE_TYPE_LABEL[sourceType] ?? alert.sourceType;
  const tone = SOURCE_TYPE_TONE[sourceType] ?? "text-content-tertiary";
  // self_report 알림에만 priority 가 채워짐 — CRITICAL/HIGH 면 카드 좌측 강조 보더로 시선 끌기.
  const priority: SymptomPriority | null =
    sourceType === "self_report" ? alert.priority : null;
  // 강조 우선순위: priority(self_report) > sourceType=timer > 일반.
  // timer 카드는 환자명/호실 없이 body 만 있어 라벨/시각적 단서가 약함 → 카드 자체에 SOURCE_TYPE_BORDER.timer 적용해 식별성 보강.
  const accentBorderClass = priority
    ? PRIORITY_BORDER[priority]
    : sourceType === "timer"
      ? SOURCE_TYPE_BORDER.timer
      : undefined;
  // timer 알림은 title (예: "음성 메모 알림") 이 sourceType 라벨("타이머") 보다 더 구체적이라 title 도 함께 노출.
  const titleLine =
    sourceType === "timer" && alert.title && alert.title !== label
      ? alert.title
      : null;

  return (
    <PanelCard accentBorderClass={accentBorderClass}>
      {/* 1행: 라벨 + (선택) priority 칩 (좌) / 상대시간 (우) — 색은 라벨 글자색에만 */}
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 min-w-0">
          <span
            className={cn(
              "text-body-sm font-semibold tracking-tight shrink-0 leading-none",
              tone,
            )}
          >
            {label}
          </span>
          {priority && (
            <span
              className={cn(
                "px-1.5 py-0.5 rounded text-[11px] font-bold leading-none shrink-0",
                PRIORITY_CHIP[priority],
              )}
            >
              {PRIORITY_LABEL[priority]}
            </span>
          )}
        </div>
        <span className="text-body-xs font-medium text-content-tertiary shrink-0 leading-none">
          {formatRelativeTime(alert.createdAt)}
        </span>
      </div>

      {titleLine && (
        <span className="text-body-sm font-bold text-content-primary leading-tight">
          {titleLine}
        </span>
      )}

      {alert.patientName && (
        <div className="flex items-center gap-2 min-w-0">
          <span className="text-body-sm font-bold text-content-primary truncate leading-tight">
            {alert.patientName}
          </span>
          {roomBed && (
            <span className="px-1.5 py-0.5 rounded bg-[#F7F8FA] text-content-secondary text-[11px] font-bold leading-none shrink-0">
              {roomBed}
            </span>
          )}
        </div>
      )}
      {alert.body && (
        <p className="text-body-sm text-content-secondary leading-snug break-words">
          {alert.body}
        </p>
      )}
    </PanelCard>
  );
}

function EmptyState({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center py-10 text-content-muted gap-2 opacity-50">
      <Info className="w-6 h-6" />
      <p className="text-body-micro">{children}</p>
    </div>
  );
}

function LoadingState() {
  return (
    <div className="flex flex-col items-center justify-center py-10 text-content-muted gap-2">
      <Loader2 className="w-5 h-5 animate-spin" />
      <p className="text-body-micro">불러오는 중...</p>
    </div>
  );
}
