'use client'

import { useLayoutEffect, useMemo, useRef, useState } from "react";
import { Info, Loader2, Siren } from "lucide-react";
import { cn } from "@/lib/utils";
import { PanelCard } from "./PanelCard";
import { useMyNotifications } from "../hooks/useNotifications";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import {
  PRIORITY_CHIP,
  PRIORITY_LABEL,
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
  // self_report 알림 priority — 카드 보더 강조는 제거됨 (사이드바 카드 보더 전체 제거 정책), 라벨 옆 칩으로만 표시.
  const priority: SymptomPriority | null =
    sourceType === "self_report" ? alert.priority : null;

  // body 3줄 초과 시 카드 자체 클릭으로 펼치기/접기. hasOverflow 는 NotificationBody 가 측정 후 보고.
  const [expanded, setExpanded] = useState(false);
  const [hasOverflow, setHasOverflow] = useState(false);
  const isInteractive = !!alert.body && hasOverflow;
  const toggle = () => setExpanded((prev) => !prev);

  return (
    <PanelCard
      onClick={isInteractive ? toggle : undefined}
      role={isInteractive ? "button" : undefined}
      tabIndex={isInteractive ? 0 : undefined}
      onKeyDown={
        isInteractive
          ? (event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                toggle();
              }
            }
          : undefined
      }
      className={isInteractive ? "cursor-pointer" : undefined}
    >
      {/* 1행: 라벨 + (선택) priority 칩 (좌) / 상대시간 (우) — 색은 라벨 글자색에만 */}
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 min-w-0">
          <span
            className={cn(
              "text-body-sm font-bold tracking-tight shrink-0 leading-none",
              tone,
            )}
          >
            {label}
          </span>
          {priority && (
            <span
              className={cn(
                "flex items-center gap-0.5 px-1.5 py-1 rounded text-[14px] font-bold leading-none shrink-0",
                PRIORITY_CHIP[priority],
              )}
            >
              {priority === "CRITICAL" && <Siren className="size-3.5 shrink-0" />}
              {PRIORITY_LABEL[priority]}
            </span>
          )}
        </div>
        <span className="text-body-xs font-medium text-content-tertiary shrink-0 leading-none">
          {formatRelativeTime(alert.createdAt)}
        </span>
      </div>

      {alert.patientName && (
        <div className="flex items-center gap-2 min-w-0">
          <span className="text-body-sm font-semibold text-content-primary truncate leading-tight">
            {alert.patientName}
          </span>
          {roomBed && (
            <span className=" text-content-secondary text-[12px] font-medium">
              {roomBed}
            </span>
          )}
        </div>
      )}
      {alert.body && (
        <NotificationBody
          body={alert.body}
          expanded={expanded}
          onOverflowChange={setHasOverflow}
        />
      )}
    </PanelCard>
  );
}

// 3줄 초과 여부만 부모에 보고 — 토글 UI/상태는 부모(NotificationCard) 가 가짐.
// 펼친 상태에선 clientHeight === scrollHeight 가 되어 측정값이 false 로 잘못 갱신되므로 측정 skip.
function NotificationBody({
  body,
  expanded,
  onOverflowChange,
}: {
  body: string;
  expanded: boolean;
  onOverflowChange: (hasOverflow: boolean) => void;
}) {
  const bodyRef = useRef<HTMLParagraphElement>(null);

  useLayoutEffect(() => {
    const element = bodyRef.current;
    if (!element) return;
    if (expanded) return;
    onOverflowChange(element.scrollHeight > element.clientHeight + 1);
  }, [body, expanded, onOverflowChange]);

  return (
    <p
      ref={bodyRef}
      className={cn(
        "text-body-sm text-content-secondary leading-snug break-words",
        !expanded && "line-clamp-3",
      )}
    >
      {body}
    </p>
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
