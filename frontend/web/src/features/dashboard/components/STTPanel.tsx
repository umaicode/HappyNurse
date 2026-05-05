'use client'

import { Search, ClipboardList, Loader2 } from "lucide-react";
import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { PanelCard } from "./PanelCard";
import {
  ORDER_STATUS_LABEL,
  ORDER_STATUS_TONE,
  ORDER_TYPE_LABEL,
} from "@/features/dashboard/types/order";
import { useOrders } from "../hooks/useOrders";
import { formatMonthDayHHmm } from "@/lib/time";

type STTPanelProps = {
  encounterId: number | null;
};

export function STTPanel({ encounterId }: STTPanelProps) {
  const { data, isPending, isError } = useOrders(encounterId);
  const orders = data?.orders ?? [];

  const [searchQuery, setSearchQuery] = useState("");

  const patientName = data?.patientName ?? "";

  const filteredOrders = useMemo(() => {
    const query = searchQuery.trim();
    const filtered = query
      ? orders.filter(
          (order) =>
            order.orderName.includes(query) ||
            order.orderCode.includes(query) ||
            (order.remarks ?? "").includes(query) ||
            order.prescriberName.includes(query) ||
            patientName.includes(query),
        )
      : orders;

    // createdAt desc — 최신 오더가 위.
    return [...filtered].sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }, [orders, searchQuery, patientName]);

  return (
    <div className="flex flex-col h-full bg-surface-base">
      {/* Search Header */}
      <div className="bg-white/95 backdrop-blur-md sticky top-0 z-30 flex flex-col gap-2 p-3 border-b border-border-base">
        <div className="relative group px-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted group-focus-within:text-brand-primary transition-colors z-10" />
          <Input
            type="text"
            placeholder="오더 검색..."
            className="pl-9 bg-surface-base border-border-base h-10 text-body-xs focus-visible:ring-1 focus-visible:ring-brand-primary rounded-md"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Card List */}
      <div className="flex-1 overflow-y-auto p-2 flex flex-col gap-2.5">
        {encounterId === null ? (
          <EmptyMessage>환자를 선택해주세요</EmptyMessage>
        ) : isPending ? (
          <div className="h-full flex flex-col items-center justify-center text-content-muted gap-2 py-20 opacity-60">
            <Loader2 className="w-6 h-6 animate-spin" />
            <p className="text-body-xs">불러오는 중...</p>
          </div>
        ) : isError ? (
          <EmptyMessage>오더를 불러오지 못했습니다</EmptyMessage>
        ) : filteredOrders.length === 0 ? (
          <EmptyMessage>{searchQuery ? "검색 결과 없음" : "오더 없음"}</EmptyMessage>
        ) : (
          filteredOrders.map((order) => {
            const isChanged = order.updatedAt !== order.createdAt;
            const isCompleted = order.status === "completed";
            return (
              <PanelCard
                key={order.medicationOrderId}
                accentBorderClass={
                  isChanged
                    ? "border-l-4 border-l-brand-primary border-brand-primary/20 bg-brand-surface/20"
                    : undefined
                }
                variantClass={isCompleted ? "opacity-70 bg-slate-50/50" : undefined}
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-baseline gap-2 min-w-0">
                    <span className="text-[15px] text-content-primary leading-none shrink-0">
                      {order.orderCode}
                    </span>
                    <span className="text-body-micro text-brand-primary shrink-0">
                      {ORDER_TYPE_LABEL[order.orderType] ?? order.orderType}
                    </span>
                    {isChanged && (
                      <span className="text-body-micro text-brand-primary shrink-0">
                        · 변경
                      </span>
                    )}
                  </div>
                  <span className="text-body-base font-mono text-content-primary shrink-0 leading-none">
                    {formatMonthDayHHmm(order.createdAt)}
                  </span>
                </div>

                {patientName && (
                  <span className="text-[15px] text-content-primary leading-none truncate">
                    {patientName}
                  </span>
                )}

                <p className="text-body-sm text-content-secondary leading-tight break-words">
                  {order.orderName}
                </p>

                {/* 2x2 Info Grid */}
                <div className="grid grid-cols-2 gap-x-4 gap-y-2 py-1">
                  <InfoCell label="1회량">
                    <span className="font-mono">
                      {order.dose}
                      {order.doseUnit}
                    </span>
                  </InfoCell>
                  <InfoCell label="횟수">
                    <span>{order.frequency}회</span>
                  </InfoCell>
                  <InfoCell label="용법">
                    <span className="text-brand-primary">{order.route}</span>
                  </InfoCell>
                  <InfoCell label="상태">
                    <span
                      className={cn(
                        ORDER_STATUS_TONE[order.status] ?? "text-status-neutral",
                      )}
                    >
                      {ORDER_STATUS_LABEL[order.status] ?? order.status}
                    </span>
                  </InfoCell>
                </div>

                {order.remarks && (
                  <p className="text-[15px] leading-relaxed text-content-primary break-words">
                    {order.remarks}
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

function InfoCell({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline gap-2">
      <span className="text-body-micro text-content-muted shrink-0">{label}</span>
      <span className="text-[15px] text-content-primary leading-none">
        {children}
      </span>
    </div>
  );
}

function EmptyMessage({ children }: { children: React.ReactNode }) {
  return (
    <div className="h-full flex flex-col items-center justify-center text-content-muted gap-2 py-20 opacity-30">
      <ClipboardList className="w-10 h-10" />
      <p className="text-body-sm">{children}</p>
    </div>
  );
}
