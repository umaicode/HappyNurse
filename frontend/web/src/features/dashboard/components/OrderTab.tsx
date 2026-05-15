'use client'

import { useMemo } from "react";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { useOrders } from "../hooks/useOrders";
import {
  ORDER_STATUS_LABEL,
  ORDER_STATUS_TONE,
  ORDER_TYPE_LABEL,
} from "@/features/dashboard/types/order";
import { formatHHmm, toIsoDate } from "@/lib/time";

type OrderTabProps = {
  encounterId: number | null;
  // 'yyyy-MM-dd' — EMRGrid 에서 단일 일자만 내려옴.
  date: string;
};

export function OrderTab({ encounterId, date }: OrderTabProps) {
  const { data, isPending, isError } = useOrders(encounterId);

  const orders = useMemo(() => {
    const list = data?.orders ?? [];
    const filtered = list.filter(
      (order) => toIsoDate(order.createdAt) === date,
    );
    // createdAt asc — 오래된 게 위, 최신이 아래.
    return [...filtered].sort(
      (a, b) =>
        new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
    );
  }, [data, date]);

  return (
    <div className="flex-1 overflow-auto bg-surface-card min-h-0 relative text-body-base">
      <div className="min-w-[1150px] flex flex-col h-full">
        {/* Header Row — 시간 셀은 NursingTab 과 동일하게 border-r + pr-4 패턴.
            처방명칭/참고사항은 minmax(0,Nfr) 로 가용 공간을 비율 분배 (2.5 : 1).
            다른 고정 컬럼은 텍스트 길이에 맞춰 좁힘 — 처방명칭이 truncate 안 되게 여유 확보. */}
        <div className="grid grid-cols-[90px_60px_88px_minmax(0,2.5fr)_60px_48px_56px_60px_80px_minmax(0,1fr)] gap-4 px-4 py-1.5 bg-surface-hover border-b border-border-base text-body-sm font-extrabold text-content-secondary sticky top-0 z-20 tracking-tight shadow-sm">
          <div className="border-r border-border-base pr-4 text-center">시간</div>
          <div className="border-r border-border-base/50 pr-4">구분</div>
          <div className="border-r border-border-base/50 pr-4">처방코드</div>
          <div className="border-r border-border-base/50 pr-4">처방명칭</div>
          <div className="text-center border-r border-border-base/50 pr-4">1회량</div>
          <div className="text-center border-r border-border-base/50 pr-4">횟수</div>
          <div className="text-center border-r border-border-base/50 pr-4">단위</div>
          <div className="text-center border-r border-border-base/50 pr-4">용법</div>
          <div className="text-center border-r border-border-base/50 pr-4">진행상태</div>
          <div>참고사항</div>
        </div>

        {/* Body */}
        <div className="flex flex-col flex-1 pb-10">
          {encounterId === null ? (
            <EmptyState message="환자를 선택하면 의사 오더가 표시됩니다." />
          ) : isPending ? (
            <LoadingState />
          ) : isError ? (
            <EmptyState message="오더를 불러오지 못했습니다." />
          ) : orders.length === 0 ? (
            <EmptyState message="이 날짜에 등록된 의사 오더가 없습니다." />
          ) : (
            orders.map((order) => (
              <div
                key={order.medicationOrderId}
                className="grid grid-cols-[90px_60px_88px_minmax(0,2.5fr)_60px_48px_56px_60px_80px_minmax(0,1fr)] gap-4 px-4 py-3 border-b border-border-base/50 items-center hover:bg-surface-hover/30 transition-all text-body-sm text-content-secondary"
              >
                {/* 시간 — NursingTab 과 동일 패턴 (border-r + pr-4 + 내부 w-full text-center) */}
                <div className="border-r border-border-base/50 pr-4">
                  <div className="w-full text-center tabular-nums font-extrabold text-[15px] text-content-primary leading-[1.6]">
                    {formatHHmm(order.createdAt)}
                  </div>
                </div>
                <div className="font-bold text-content-tertiary truncate">
                  {ORDER_TYPE_LABEL[order.orderType] ?? order.orderType}
                </div>
                <div className="tabular-nums font-bold text-content-primary truncate">
                  {order.orderCode}
                </div>
                <div className="font-medium text-content-secondary truncate">
                  {order.orderName}
                </div>
                <div className="text-center tabular-nums font-bold">{order.dose}</div>
                <div className="text-center tabular-nums font-bold">{order.frequency}</div>
                <div className="text-center text-content-tertiary">
                  {order.doseUnit}
                </div>
                <div className="text-center text-content-primary font-bold">
                  {order.route}
                </div>
                <div className="flex justify-center">
                  <span
                    className={cn(
                      "text-body-xs font-semibold",
                      ORDER_STATUS_TONE[order.status] ?? "text-status-neutral",
                    )}
                  >
                    {ORDER_STATUS_LABEL[order.status] ?? order.status}
                  </span>
                </div>
                <div className="font-medium text-content-secondary truncate">
                  {order.remarks ?? ""}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center flex-1 py-16 text-content-muted">
      <p className="text-body-sm font-bold">{message}</p>
    </div>
  );
}

function LoadingState() {
  return (
    <div className="flex flex-col items-center justify-center flex-1 gap-2 py-16 text-content-muted">
      <Loader2 className="w-6 h-6 animate-spin opacity-60" />
      <p className="text-body-sm">불러오는 중...</p>
    </div>
  );
}
