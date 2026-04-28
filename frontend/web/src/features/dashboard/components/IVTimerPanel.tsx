'use client'

import { useEffect, useState } from "react";
import { Droplet, Clock, AlertTriangle } from "lucide-react";
import { cn } from "@/lib/utils";

type IVTimerItem = {
  id: number;
  patientName: string;
  room: string;
  fluidName: string;
  startedAt: string;
  endsAt: string;
};

const MOCK_IV_TIMERS: IVTimerItem[] = [
  {
    id: 1,
    patientName: "🛠️ 김가민",
    room: "7101호",
    fluidName: "🛠️ N/S 1L",
    startedAt: "08:00",
    endsAt: "20:30",
  },
  {
    id: 2,
    patientName: "🛠️ 박영희",
    room: "7101호",
    fluidName: "🛠️ 5DW 500mL",
    startedAt: "10:15",
    endsAt: "18:35",
  },
  {
    id: 3,
    patientName: "🛠️ 최민호",
    room: "7101호",
    fluidName: "🛠️ Hartmann 1L",
    startedAt: "07:30",
    endsAt: "17:30",
  },
  {
    id: 4,
    patientName: "🛠️ 한지민",
    room: "7102호",
    fluidName: "🛠️ N/S 500mL + KCl 20mEq",
    startedAt: "12:00",
    endsAt: "22:00",
  },
  {
    id: 5,
    patientName: "🛠️ 이도현",
    room: "7102호",
    fluidName: "🛠️ 10DW 500mL",
    startedAt: "14:00",
    endsAt: "20:15",
  },
];

function parseTimeToMinutes(time: string): number {
  const [hours, minutes] = time.split(":").map(Number);
  if (Number.isNaN(hours) || Number.isNaN(minutes)) return 0;
  return hours * 60 + minutes;
}

function getCurrentMinutes(): number {
  const now = new Date();
  return now.getHours() * 60 + now.getMinutes();
}

function formatDuration(minutes: number): string {
  const safeMinutes = Math.max(0, minutes);
  const hours = Math.floor(safeMinutes / 60);
  const remainder = safeMinutes % 60;
  if (hours === 0) return `${remainder}분`;
  if (remainder === 0) return `${hours}시간`;
  return `${hours}시간 ${remainder}분`;
}

export function IVTimerPanel() {
  const [nowMinutes, setNowMinutes] = useState<number>(() => getCurrentMinutes());

  useEffect(() => {
    const interval = setInterval(() => {
      setNowMinutes(getCurrentMinutes());
    }, 60_000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="flex flex-col h-full bg-[var(--color-surface-base)]">
      <div className="flex-1 overflow-y-auto p-2 flex flex-col gap-2.5">
        {MOCK_IV_TIMERS.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-10 text-content-muted gap-2 opacity-30">
            <Droplet className="w-6 h-6" />
            <p className="text-[11px] font-medium">진행 중인 수액 없음</p>
          </div>
        ) : (
          MOCK_IV_TIMERS.map((item) => {
            const startMinutes = parseTimeToMinutes(item.startedAt);
            const endMinutes = parseTimeToMinutes(item.endsAt);
            const totalMinutes = Math.max(1, endMinutes - startMinutes);
            const elapsedMinutes = Math.max(0, nowMinutes - startMinutes);
            const remainingMinutes = endMinutes - nowMinutes;
            const progressPercent = Math.max(
              0,
              Math.min(100, (elapsedMinutes / totalMinutes) * 100),
            );
            const isWarning = remainingMinutes <= 60;

            return (
              <div
                key={item.id}
                className={cn(
                  "relative bg-white rounded-xl border shadow-sm p-3 flex flex-col gap-2 transition-all",
                  isWarning
                    ? "border-l-4 border-l-amber-500 border-amber-500/30"
                    : "border-border-base",
                )}
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5 min-w-0">
                    <span className="text-[13px] font-black text-content-primary truncate">
                      {item.patientName}
                    </span>
                    <span className="text-[11px] font-mono font-bold text-content-muted shrink-0">
                      {item.room}
                    </span>
                  </div>
                  {isWarning && (
                    <span className="flex items-center gap-1 text-[11px] font-semibold text-amber-600 shrink-0">
                      <AlertTriangle className="w-3 h-3" />
                      교체 임박
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-1.5">
                  <Droplet className="w-3.5 h-3.5 text-sky-500 shrink-0" />
                  <span className="text-[12px] font-semibold text-content-secondary truncate">
                    {item.fluidName}
                  </span>
                </div>

                <div className="h-1.5 w-full rounded-full bg-slate-100 overflow-hidden">
                  <div
                    className={cn(
                      "h-full rounded-full transition-all",
                      isWarning ? "bg-amber-500" : "bg-sky-500",
                    )}
                    style={{ width: `${progressPercent}%` }}
                  />
                </div>

                <div className="flex flex-col gap-0.5 text-[12px]">
                  <div className="flex items-center gap-1 text-content-secondary">
                    <Clock
                      className={cn(
                        "w-3.5 h-3.5",
                        isWarning ? "text-amber-600" : "text-content-muted",
                      )}
                    />
                    <span className="font-medium">
                      <span className="font-mono font-bold">{item.startedAt}</span>
                      {" ~ 현재 "}
                      <span className="font-bold">{formatDuration(elapsedMinutes)}</span>
                      {" 경과"}
                    </span>
                  </div>
                  <div
                    className={cn(
                      "pl-[18px] font-medium",
                      isWarning ? "text-amber-600" : "text-content-tertiary",
                    )}
                  >
                    종료 예정 <span className="font-mono font-bold">{item.endsAt}</span>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
