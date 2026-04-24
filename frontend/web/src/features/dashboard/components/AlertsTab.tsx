'use client'

import { AlertCircle, AlertTriangle, Info } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  SEVERITY_LABEL,
  STATUS_LABEL,
  type PatientAlert,
} from "@/features/dashboard/types/alert";

type AlertsTabProps = {
  alerts: PatientAlert[];
  patientId: string;
};

function SeverityIcon({ severity }: { severity: PatientAlert["severity"] }) {
  if (severity === "critical") {
    return <AlertCircle className="size-4 text-rose-500" />;
  }
  if (severity === "warning") {
    return <AlertTriangle className="size-4 text-amber-500" />;
  }
  return <Info className="size-4 text-sky-500" />;
}

export function AlertsTab({ alerts, patientId }: AlertsTabProps) {
  const filtered = alerts.filter((alert) => alert.patientId === patientId);

  return (
    <div className="flex-1 overflow-auto bg-[var(--color-surface-card)] min-h-0 relative text-body-base">
      <div className="min-w-[900px] flex flex-col h-full">
        <div className="grid grid-cols-[80px_100px_100px_1fr_100px] gap-2 px-4 py-1.5 bg-[var(--color-surface-hover)] border-b border-[var(--color-border-base)] text-body-sm font-extrabold text-[var(--color-content-secondary)] sticky top-0 z-20 tracking-tight shadow-sm">
          <div className="text-center border-r border-[var(--color-border-base)]/50">시간</div>
          <div className="text-center border-r border-[var(--color-border-base)]/50">중요도</div>
          <div className="text-center border-r border-[var(--color-border-base)]/50">구분</div>
          <div className="border-r border-[var(--color-border-base)]/50 pl-2">내용</div>
          <div className="text-center">상태</div>
        </div>

        <div className="flex flex-col flex-1 pb-10">
          {filtered.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center py-16 gap-2 text-[var(--color-content-muted)]">
              <Info className="size-6" />
              <p className="text-body-sm font-medium">표시할 알림이 없습니다.</p>
            </div>
          ) : (
            filtered.map((alert) => (
              <div
                key={alert.id}
                className="grid grid-cols-[80px_100px_100px_1fr_100px] gap-2 px-4 py-3 border-b border-[var(--color-border-base)]/50 items-center hover:bg-[var(--color-surface-hover)]/30 transition-all text-body-sm text-[var(--color-content-secondary)]"
              >
                <div className="text-center font-mono font-bold text-[var(--color-content-primary)]">
                  {alert.time}
                </div>
                <div className="flex items-center justify-center gap-1.5">
                  <SeverityIcon severity={alert.severity} />
                  <span
                    className={cn(
                      "text-[12px] font-semibold",
                      alert.severity === "critical" && "text-rose-600",
                      alert.severity === "warning" && "text-amber-600",
                      alert.severity === "info" && "text-sky-600",
                    )}
                  >
                    {SEVERITY_LABEL[alert.severity]}
                  </span>
                </div>
                <div className="text-center font-bold text-[var(--color-content-tertiary)]">
                  {alert.category}
                </div>
                <div className="font-medium text-[var(--color-content-secondary)] pl-2 whitespace-pre-wrap">
                  {alert.message}
                </div>
                <div className="flex justify-center">
                  <span
                    className={cn(
                      "text-[12px] font-semibold px-2 py-0.5 rounded",
                      alert.status === "unread" &&
                        "bg-rose-50 text-rose-600",
                      alert.status === "acknowledged" &&
                        "bg-amber-50 text-amber-600",
                      alert.status === "resolved" &&
                        "bg-slate-100 text-slate-500",
                    )}
                  >
                    {STATUS_LABEL[alert.status]}
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
