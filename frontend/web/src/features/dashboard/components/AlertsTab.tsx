'use client'

import { useMemo } from "react";
import { Loader2 } from "lucide-react";
import { useSymptomReports } from "../hooks/useSymptomReports";
import { formatHHmm, toIsoDate } from "@/lib/time";

type AlertsTabProps = {
  patientId: number | null;
  // 'yyyy-MM-dd' — EMRGrid 에서 단일 일자만 내려옴.
  date: string;
};

export function AlertsTab({ patientId, date }: AlertsTabProps) {
  const { data, isPending, isError } = useSymptomReports(patientId);
  const symptoms = useMemo(() => {
    const list = data?.symptoms ?? [];
    const filtered = list.filter(
      (symptom) => toIsoDate(symptom.submittedAt) === date,
    );
    // submittedAt asc — 오래된 게 위, 최신이 아래.
    return [...filtered].sort(
      (a, b) =>
        new Date(a.submittedAt).getTime() - new Date(b.submittedAt).getTime(),
    );
  }, [data, date]);

  return (
    <div className="flex-1 overflow-auto bg-surface-card min-h-0 relative text-body-base">
      <div className="min-w-[700px] flex flex-col h-full">
        <div className="grid grid-cols-[110px_1fr] gap-2 px-4 py-1.5 bg-surface-hover border-b border-border-base text-body-sm font-extrabold text-content-secondary sticky top-0 z-20 tracking-tight shadow-sm">
          <div className="text-center border-r border-border-base/50">시간</div>
          <div className="pl-2">증상</div>
        </div>

        <div className="flex flex-col flex-1 pb-10">
          {patientId === null ? (
            <EmptyMessage>환자를 선택하면 호출 내역이 표시됩니다</EmptyMessage>
          ) : isPending ? (
            <LoadingMessage />
          ) : isError ? (
            <EmptyMessage>호출 내역을 불러오지 못했습니다</EmptyMessage>
          ) : symptoms.length === 0 ? (
            <EmptyMessage>이 날짜에 표시할 호출이 없습니다</EmptyMessage>
          ) : (
            symptoms.map((symptom) => (
              <div
                key={symptom.selfReportId}
                className="grid grid-cols-[110px_1fr] gap-2 px-4 py-3 border-b border-border-base/50 items-start hover:bg-surface-hover/30 transition-all text-body-sm text-content-secondary"
              >
                <div className="text-center font-mono font-extrabold text-[15px] text-content-primary pt-0.5">
                  {formatHHmm(symptom.submittedAt)}
                </div>
                <div className="pl-2 pt-1">
                  {symptom.buttonLabel && (
                    <span className="inline-block px-1.5 py-0.5 mr-1.5 text-[11px] font-bold rounded bg-brand-surface text-brand-primary leading-none align-middle">
                      {symptom.buttonLabel}
                    </span>
                  )}
                  <span className="font-medium text-content-primary whitespace-pre-wrap break-words">
                    {symptom.symptomText}
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

function EmptyMessage({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center py-16 text-content-muted">
      <p className="text-body-sm font-medium">{children}</p>
    </div>
  );
}

function LoadingMessage() {
  return (
    <div className="flex-1 flex flex-col items-center justify-center py-16 gap-2 text-content-muted">
      <Loader2 className="size-6 animate-spin" />
      <p className="text-body-sm">불러오는 중...</p>
    </div>
  );
}
