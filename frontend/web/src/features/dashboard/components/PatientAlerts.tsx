'use client'

import { useEffect, useMemo, useRef } from "react";
import { Info } from "lucide-react";
import { INITIAL_PATIENT_ALERTS } from "@/mockup/emr-data";
import type { PatientAlert } from "@/features/dashboard/types/alert";

function parseTimeToMinutes(time: string): number {
  const [hours, minutes] = time.split(":").map(Number);
  if (Number.isNaN(hours) || Number.isNaN(minutes)) return 0;
  return hours * 60 + minutes;
}

function getCurrentMinutes(): number {
  const now = new Date();
  return now.getHours() * 60 + now.getMinutes();
}

export function PatientAlerts() {
  const sorted = useMemo<PatientAlert[]>(
    () =>
      [...INITIAL_PATIENT_ALERTS].sort(
        (a, b) => parseTimeToMinutes(a.time) - parseTimeToMinutes(b.time),
      ),
    [],
  );

  const itemRefs = useRef<Map<number, HTMLDivElement | null>>(new Map());

  useEffect(() => {
    if (sorted.length === 0) return;
    const nowMinutes = getCurrentMinutes();
    const closest = sorted.reduce((best, alert) => {
      const distance = Math.abs(parseTimeToMinutes(alert.time) - nowMinutes);
      const bestDistance = Math.abs(parseTimeToMinutes(best.time) - nowMinutes);
      return distance < bestDistance ? alert : best;
    }, sorted[0]);
    const element = itemRefs.current.get(closest.id);
    if (element) {
      element.scrollIntoView({ block: "center" });
    }
  }, [sorted]);

  return (
    <div className="flex flex-col h-full bg-[var(--color-surface-base)]">
      <div className="flex-1 overflow-y-auto p-2 flex flex-col gap-2.5">
        {sorted.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-10 text-content-muted gap-2 opacity-30">
            <Info className="w-6 h-6" />
            <p className="text-[11px] font-medium">표시할 알림 없음</p>
          </div>
        ) : (
          sorted.map((alert) => (
            <div
              key={alert.id}
              ref={(element) => {
                if (element) itemRefs.current.set(alert.id, element);
                else itemRefs.current.delete(alert.id);
              }}
              className="relative bg-white rounded-xl border border-border-base shadow-sm p-3 flex flex-col gap-2 transition-all hover:border-[var(--color-brand-primary)]/30"
            >
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 min-w-0">
                  <span className="text-[13px] font-black text-content-primary truncate">
                    {alert.patientName}
                  </span>
                  <span className="text-[11px] font-mono font-bold text-content-muted shrink-0">
                    {alert.room}
                  </span>
                </div>
                <span className="text-[11px] font-mono font-bold text-content-muted shrink-0">
                  {alert.time}
                </span>
              </div>

              <div className="flex items-center gap-1.5">
                <span className="text-[12px] font-bold text-content-tertiary">
                  {alert.category}
                </span>
              </div>

              <p className="text-[13px] leading-relaxed text-content-secondary break-words">
                {alert.message}
              </p>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
