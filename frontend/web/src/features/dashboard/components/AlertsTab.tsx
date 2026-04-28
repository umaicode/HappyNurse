'use client'

import { Info } from "lucide-react";
import type { PatientAlert } from "@/features/dashboard/types/alert";

type AlertsTabProps = {
  alerts: PatientAlert[];
  patientId: string;
};

export function AlertsTab({ alerts, patientId }: AlertsTabProps) {
  const filtered = alerts.filter((alert) => alert.patientId === patientId);

  return (
    <div className="flex-1 overflow-auto bg-[var(--color-surface-card)] min-h-0 relative text-body-base">
      <div className="min-w-[700px] flex flex-col h-full">
        <div className="grid grid-cols-[80px_120px_1fr] gap-2 px-4 py-1.5 bg-[var(--color-surface-hover)] border-b border-[var(--color-border-base)] text-body-sm font-extrabold text-[var(--color-content-secondary)] sticky top-0 z-20 tracking-tight shadow-sm">
          <div className="text-center border-r border-[var(--color-border-base)]/50">시간</div>
          <div className="text-center border-r border-[var(--color-border-base)]/50">구분</div>
          <div className="pl-2">내용</div>
        </div>

        <div className="flex flex-col flex-1 pb-10">
          {filtered.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center py-16 gap-2 text-[var(--color-content-muted)]">
              <Info className="size-6" />
              <p className="text-body-sm font-medium">표시할 호출이 없습니다.</p>
            </div>
          ) : (
            filtered.map((alert) => (
              <div
                key={alert.id}
                className="grid grid-cols-[80px_120px_1fr] gap-2 px-4 py-3 border-b border-[var(--color-border-base)]/50 items-center hover:bg-[var(--color-surface-hover)]/30 transition-all text-body-sm text-[var(--color-content-secondary)]"
              >
                <div className="text-center font-mono font-bold text-[var(--color-content-primary)]">
                  {alert.time}
                </div>
                <div className="text-center font-bold text-[var(--color-content-tertiary)]">
                  {alert.category}
                </div>
                <div className="font-medium text-[var(--color-content-secondary)] pl-2 whitespace-pre-wrap">
                  {alert.message}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
