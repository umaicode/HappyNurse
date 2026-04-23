'use client'

import { useMemo, useState } from "react";
import { Search, Info, AlertCircle, AlertTriangle } from "lucide-react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import {
  INITIAL_PATIENT_ALERTS,
  type PatientAlert,
} from "@/mockup/emr-data";

const SEVERITY_LABEL: Record<PatientAlert["severity"], string> = {
  critical: "긴급",
  warning: "경고",
  info: "안내",
};

const STATUS_LABEL: Record<PatientAlert["status"], string> = {
  unread: "미확인",
  acknowledged: "확인",
  resolved: "해결",
};

function SeverityIcon({ severity }: { severity: PatientAlert["severity"] }) {
  if (severity === "critical") {
    return <AlertCircle className="size-4 text-rose-500 shrink-0" />;
  }
  if (severity === "warning") {
    return <AlertTriangle className="size-4 text-amber-500 shrink-0" />;
  }
  return <Info className="size-4 text-sky-500 shrink-0" />;
}

export function PatientAlerts() {
  const [searchQuery, setSearchQuery] = useState("");

  const filtered = useMemo(() => {
    const query = searchQuery.trim();
    if (!query) return INITIAL_PATIENT_ALERTS;
    return INITIAL_PATIENT_ALERTS.filter((alert) =>
      [
        alert.patientName,
        alert.room,
        alert.category,
        alert.message,
      ].some((field) => field.includes(query)),
    );
  }, [searchQuery]);

  return (
    <div className="flex flex-col h-full bg-[var(--color-surface-base)]">
      {/* Search Header */}
      <div className="bg-white/95 backdrop-blur-md sticky top-0 z-30 flex flex-col gap-2 p-3 border-b border-border-base">
        <div className="relative group px-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted group-focus-within:text-[var(--color-brand-primary)] transition-colors z-10" />
          <Input
            type="text"
            placeholder="알림 검색..."
            className="pl-9 bg-[var(--color-surface-base)] border-border-base h-10 text-[13px] focus-visible:ring-1 focus-visible:ring-[var(--color-brand-primary)] rounded-md"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Alerts List */}
      <div className="flex-1 overflow-y-auto p-2 flex flex-col gap-2.5">
        {filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-10 text-content-muted gap-2 opacity-30">
            <Info className="w-6 h-6" />
            <p className="text-[11px] font-medium">표시할 알림 없음</p>
          </div>
        ) : (
          filtered.map((alert) => (
            <div
              key={alert.id}
              className={cn(
                "relative bg-white rounded-xl border border-border-base shadow-sm p-3 flex flex-col gap-2 transition-all hover:border-[var(--color-brand-primary)]/30",
                alert.severity === "critical" &&
                  "border-l-4 border-l-rose-500 border-rose-500/20",
                alert.severity === "warning" &&
                  "border-l-4 border-l-amber-500 border-amber-500/20",
                alert.severity === "info" &&
                  "border-l-4 border-l-sky-500 border-sky-500/20",
                alert.status === "resolved" && "opacity-70 bg-slate-50/50",
              )}
            >
              {/* Patient Identifier Row */}
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 min-w-0">
                  <span className="text-[13px] font-black text-content-primary truncate">
                    {alert.patientName}
                  </span>
                  <span className="text-[11px] font-mono font-bold text-content-muted shrink-0">
                    {alert.room}
                  </span>
                </div>
                <span
                  className={cn(
                    "text-[11px] font-semibold px-2 py-0.5 rounded shrink-0",
                    alert.status === "unread" && "bg-rose-50 text-rose-600",
                    alert.status === "acknowledged" &&
                      "bg-amber-50 text-amber-600",
                    alert.status === "resolved" &&
                      "bg-slate-100 text-slate-500",
                  )}
                >
                  {STATUS_LABEL[alert.status]}
                </span>
              </div>

              {/* Severity + Category + Time */}
              <div className="flex items-center gap-1.5">
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
                <span className="text-content-muted text-[11px]">·</span>
                <span className="text-[12px] font-bold text-content-tertiary">
                  {alert.category}
                </span>
                <span className="ml-auto text-[11px] font-mono font-bold text-content-muted">
                  {alert.time}
                </span>
              </div>

              {/* Message */}
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
