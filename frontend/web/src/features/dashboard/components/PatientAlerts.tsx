'use client'

import { useMemo } from "react";
import { Info, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { PanelCard } from "./PanelCard";
import { useMyNotifications } from "../hooks/useNotifications";
import {
  SOURCE_TYPE_LABEL,
  SOURCE_TYPE_TONE,
  type NotificationListItem,
  type SourceType,
} from "../types/notification";
import { formatMonthDayHHmm } from "@/lib/time";

export function PatientAlerts() {
  const { data, isPending, isError } = useMyNotifications();

  // createdAt desc — 최신이 위. 자동 스크롤 없음.
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
          sorted.map((alert) => {
            const tone =
              SOURCE_TYPE_TONE[alert.sourceType as SourceType] ??
              "text-content-tertiary";
            const label =
              SOURCE_TYPE_LABEL[alert.sourceType as SourceType] ??
              alert.sourceType;
            return (
              <PanelCard key={alert.notificationId}>
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-baseline gap-2 min-w-0">
                    <span className="text-[15px] text-content-primary leading-none truncate">
                      {alert.patientName ?? "환자 미지정"}
                    </span>
                    <span className={cn("text-body-micro shrink-0", tone)}>
                      {label}
                    </span>
                  </div>
                  <span className="text-body-base font-mono text-content-primary shrink-0 leading-none">
                    {formatMonthDayHHmm(alert.createdAt)}
                  </span>
                </div>

                {alert.body && (
                  <p className="text-[15px] leading-relaxed text-content-primary break-words">
                    {alert.body}
                  </p>
                )}
              </PanelCard>
            );
          })
        )}
      </div>
    </div>
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
