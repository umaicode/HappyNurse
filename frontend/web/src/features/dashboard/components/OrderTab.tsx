'use client'

import { useMemo } from "react";
import { ClipboardList, Loader2 } from "lucide-react";
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
      <div className="min-w-[1100px] flex flex-col h-full">
        {/* Header Row */}
        <div className="grid grid-cols-[110px_80px_100px_1fr_220px_70px_50px_60px_70px_90px] gap-2 px-4 py-1.5 bg-surface-hover border-b border-border-base text-body-sm font-extrabold text-content-secondary sticky top-0 z-20 tracking-tight shadow-sm">
          <div className="text-center border-r border-border-base/50">시간</div>
          <div className="border-r border-border-base/50 pl-2">구분</div>
          <div className="border-r border-border-base/50 pl-2">처방코드</div>
          <div className="border-r border-border-base/50 pl-2">처방명칭</div>
          <div className="border-r border-border-base/50 pl-2">참고사항</div>
          <div className="text-center border-r border-border-base/50">1회량</div>
          <div className="text-center border-r border-border-base/50">횟수</div>
          <div className="text-center border-r border-border-base/50">단위</div>
          <div className="text-center border-r border-border-base/50">용법</div>
          <div className="text-center">진행상태</div>
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
                className="grid grid-cols-[110px_80px_100px_1fr_220px_70px_50px_60px_70px_90px] gap-2 px-4 py-3 border-b border-border-base/50 items-center hover:bg-surface-hover/30 transition-all text-body-sm text-content-secondary"
              >
                <div className="text-center font-mono font-extrabold text-[15px] text-content-primary">
                  {formatHHmm(order.createdAt)}
                </div>
                <div className="pl-2 font-bold text-content-tertiary">
                  {ORDER_TYPE_LABEL[order.orderType] ?? order.orderType}
                </div>
                <div className="font-mono font-bold text-content-primary pl-2">
                  {order.orderCode}
                </div>
                <div className="font-medium text-content-secondary truncate pl-2">
                  {order.orderName}
                </div>
                <div className="font-medium text-content-secondary truncate pl-2">
                  {order.remarks ?? ""}
                </div>
                <div className="text-center font-mono font-bold">{order.dose}</div>
                <div className="text-center font-mono font-bold">{order.frequency}</div>
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
    <div className="flex flex-col items-center justify-center flex-1 gap-2 py-16 text-content-muted">
      <ClipboardList className="w-10 h-10 opacity-40" />
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
