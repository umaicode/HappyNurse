'use client'

import { useMemo } from "react";
import {
  Activity,
  AlertCircle,
  Bell,
  ClipboardList,
  Clock,
  Droplet,
  Info,
  Loader2,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { PanelCard } from "./PanelCard";
import { useMyNotifications } from "../hooks/useNotifications";
import {
  SOURCE_TYPE_BORDER,
  SOURCE_TYPE_ICON_BG,
  SOURCE_TYPE_LABEL,
  SOURCE_TYPE_TONE,
  type NotificationListItem,
  type SourceType,
} from "../types/notification";
import { formatRelativeTime } from "@/lib/time";
// mockup: PatientAlerts 시연용. 실 응답 비었을 때 fallback. 검증 후 제거.
import { MOCK_NOTIFICATIONS } from "@/mockup/notifications";

const SOURCE_TYPE_ICON: Record<SourceType, LucideIcon> = {
  self_report: Bell,
  iv_alert: Droplet,
  timer: Clock,
  order_change: ClipboardList,
  vital_alert: Activity,
};

export function PatientAlerts() {
  const { data, isPending, isError } = useMyNotifications();

  // createdAt desc — 최신이 위. 정렬은 시간순 고정 (요구사항).
  // mockup 합치기 — 실 응답에 self_report 만 있어도 다른 sourceType 시연 위해 항상 합쳐 노출.
  // 검증 끝나면 MOCK_NOTIFICATIONS import 와 spread 만 제거.
  const sorted = useMemo<NotificationListItem[]>(() => {
    const real = data?.items ?? [];
    return [...real, ...MOCK_NOTIFICATIONS].sort(
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
            <NotificationCard key={alert.notificationId} alert={alert} />
          ))
        )}
      </div>
    </div>
  );
}

function NotificationCard({ alert }: { alert: NotificationListItem }) {
  const sourceType = alert.sourceType as SourceType;
  const Icon =
    sourceType in SOURCE_TYPE_ICON
      ? SOURCE_TYPE_ICON[sourceType]
      : AlertCircle;
  const label = SOURCE_TYPE_LABEL[sourceType] ?? alert.sourceType;
  const tone = SOURCE_TYPE_TONE[sourceType] ?? "text-content-tertiary";
  const borderClass = SOURCE_TYPE_BORDER[sourceType];
  const iconBg = SOURCE_TYPE_ICON_BG[sourceType] ?? "bg-surface-hover text-content-muted";

  return (
    <PanelCard accentBorderClass={borderClass}>
      {/* 1행: 아이콘 + 라벨 (좌) / 상대시간 (우) */}
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 min-w-0">
          <div
            className={cn(
              "flex items-center justify-center w-6 h-6 rounded-md shrink-0",
              iconBg,
            )}
          >
            <Icon className="w-3.5 h-3.5" />
          </div>
          <span
            className={cn(
              "text-body-sm font-semibold tracking-tight shrink-0 leading-none",
              tone,
            )}
          >
            {label}
          </span>
        </div>
        <span className="text-body-xs font-mono font-medium text-content-tertiary shrink-0 leading-none">
          {formatRelativeTime(alert.createdAt)}
        </span>
      </div>

      {alert.patientName && (
        <span className="text-body-sm font-bold text-content-primary truncate leading-tight">
          {alert.patientName}
        </span>
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
