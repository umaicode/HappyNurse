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
        {/* Header Row */}
        <div className="grid grid-cols-[90px_60px_150px_minmax(0,1.25fr)_60px_48px_56px_60px_80px_minmax(0,1fr)] gap-4 px-4 py-1.5 bg-surface-hover border-b border-border-base text-body-sm font-bold text-content-secondary sticky top-0 z-20 tracking-tight shadow-sm">
          <div className="border-r border-border-base pr-4 text-center">시간</div>
          <div className="border-r border-border-base pr-4 text-center">구분</div>
          <div className="border-r border-border-base pr-4">처방코드</div>
          <div className="border-r border-border-base pr-4">처방명칭</div>
          <div className="text-center border-r border-border-base pr-4">1회량</div>
          <div className="text-center border-r border-border-base pr-4">횟수</div>
          <div className="text-center border-r border-border-base pr-4">단위</div>
          <div className="text-center border-r border-border-base pr-4">용법</div>
          <div className="text-center border-r border-border-base pr-4">진행상태</div>
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
                className="grid grid-cols-[90px_60px_150px_minmax(0,1.25fr)_60px_48px_56px_60px_80px_minmax(0,1fr)] gap-4 px-4 py-2 min-h-[40px] border-b border-border-base/50 items-center hover:bg-surface-hover/60 transition-colors text-body-base text-content-secondary"
              >
                {/* 시간 */}
                <div className="py-1 border-r border-border-base/50 pr-4">
                  <div className="w-full text-center tabular-nums font-bold text-[15px] text-content-primary leading-[1.6]">
                    {formatHHmm(order.createdAt)}
                  </div>
                </div>
                {/* 구분 */}
                <div className="h-full flex items-center justify-center border-r border-border-base/50 pr-4">
                  <span className="text-body-lg font-semibold text-content-primary">
                    {ORDER_TYPE_LABEL[order.orderType] ?? order.orderType}
                  </span>
                </div>
                {/* 처방코드 */}
                <div className="border-r border-border-base/50 tabular-nums font-medium text-content-primary truncate">
                  {order.orderCode}
                </div>
                {/* 처방명칭 */}
                <div className="border-r border-border-base/50 pr-4 font-medium text-content-primary truncate">
                  {order.orderName}
                </div>
                {/* 1회량 */}
                <div className="text-center border-r border-border-base/50 pr-4 tabular-nums font-medium text-content-tertiary">
                  {order.dose}
                </div>
                {/* 횟수 */}
                <div className="text-center border-r border-border-base/50 pr-4 tabular-nums font-medium text-content-tertiary">
                  {order.frequency}
                </div>
                {/* 단위 */}
                <div className="text-center border-r border-border-base/50 pr-4 text-content-tertiary font-medium">
                  {order.doseUnit}
                </div>
                {/* 용법 */}
                <div className="text-center border-r border-border-base/50 pr-4 font-medium text-content-tertiary">
                  {order.route}
                </div>
                {/* 진행상태 */}
                <div className="pt-1 h-full flex items-center justify-center border-r border-border-base/50 pr-4">
                  <span
                    className={cn(
                      "text-body-sm font-semibold",
                      ORDER_STATUS_TONE[order.status] ?? "text-content-tertiary",
                    )}
                  >
                    {ORDER_STATUS_LABEL[order.status] ?? order.status}
                  </span>
                </div>
                {/* 참고사항 */}
                <div className="font-medium text-content-tertiary truncate">
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
