'use client'

import {
  Search,
  ClipboardList,
} from "lucide-react";
import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import type { DoctorOrder } from "@/features/dashboard/types/order";
import { INITIAL_ORDERS } from "@/mockup/emr-data";

export function STTPanel() {
  const [orders] = useState<DoctorOrder[]>(INITIAL_ORDERS);
  const [searchQuery, setSearchQuery] = useState("");

  const filteredOrders = useMemo(() => {
    const result = orders.filter((order) => {
      return (
        order.name.includes(searchQuery) ||
        order.code.includes(searchQuery) ||
        order.remarks.includes(searchQuery) ||
        order.patientName.includes(searchQuery)
      );
    });

    return [...result].sort((a, b) => (b.isChanged ? 1 : 0) - (a.isChanged ? 1 : 0));
  }, [orders, searchQuery]);

  return (
    <div className="flex flex-col h-full bg-[var(--color-surface-base)]">
      {/* Search Header */}
      <div className="bg-white/95 backdrop-blur-md sticky top-0 z-30 flex flex-col gap-2 p-3 border-b border-border-base">
        <div className="relative group px-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted group-focus-within:text-[var(--color-brand-primary)] transition-colors z-10" />
          <Input
            type="text"
            placeholder="오더 검색..."
            className="pl-9 bg-[var(--color-surface-base)] border-border-base h-10 text-[13px] focus-visible:ring-1 focus-visible:ring-[var(--color-brand-primary)] rounded-md"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Modern Card List */}
      <div className="flex-1 overflow-y-auto p-2 flex flex-col gap-2.5">
        {filteredOrders.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-content-muted gap-2 py-20 opacity-30">
            <ClipboardList className="w-10 h-10" />
            <p className="text-[14px] font-bold">오더 없음</p>
          </div>
        ) : (
          filteredOrders.map((order) => (
            <div
              key={order.id}
              className={cn(
                "relative bg-white rounded-xl border border-border-base shadow-sm flex flex-col shrink-0 transition-all hover:border-[var(--color-brand-primary)]/30",
                order.isChanged && "border-l-4 border-l-[var(--color-brand-primary)] border-[var(--color-brand-primary)]/20 bg-[var(--color-brand-surface)]/20",
                order.status === "완료" && "opacity-70 bg-slate-50/50"
              )}
            >
              {/* Card Body */}
              <div className="p-3 pb-4 flex flex-col gap-2.5 h-auto">
                <div className="flex flex-col pl-1.5">
                  <div className="flex items-center justify-between gap-2 mb-1">
                    <span className="text-[14px] font-mono font-black text-[var(--color-brand-primary)] opacity-70 leading-none uppercase tracking-wider">
                      {order.code}
                    </span>
                    {order.isChanged && (
                      <span className="px-1.5 py-0.5 text-[10px] font-bold rounded bg-[var(--color-brand-primary)] text-white leading-none">
                        변경
                      </span>
                    )}
                  </div>
                  <h4 className="text-[15px] font-bold text-content-primary leading-tight">
                    {order.name}
                  </h4>
                </div>

                {/* 2x2 Info Grid: 1회량 / 횟수 / 용법 / 진행 상태 */}
                <div className="mx-1.5 grid grid-cols-2 gap-x-4 gap-y-2 py-1.5">
                  <div className="flex items-baseline gap-2">
                    <span className="text-[12px] font-bold text-content-muted shrink-0">1회량</span>
                    <span className="text-[15px] font-mono font-semibold text-content-primary leading-none">
                      {order.dose}
                      {order.unit !== "-" ? order.unit : ""}
                    </span>
                  </div>
                  <div className="flex items-baseline gap-2">
                    <span className="text-[12px] font-bold text-content-muted shrink-0">횟수</span>
                    <span className="text-[15px] font-semibold text-content-primary leading-none">
                      {order.frequency !== "-"
                        ? `${order.frequency}회${order.unit !== "-" ? `(${order.unit})` : ""}`
                        : "-"}
                    </span>
                  </div>
                  <div className="flex items-baseline gap-2">
                    <span className="text-[12px] font-bold text-content-muted shrink-0">용법</span>
                    <span className="text-[15px] font-semibold text-[var(--color-brand-primary)] leading-none">
                      {order.method}
                    </span>
                  </div>
                </div>

                {/* Remarks - Fully Visible */}
                {order.remarks && (
                  <div className="mx-1.5 pl-3 border-l-2 border-[var(--color-brand-primary)]/20 py-0.5 mt-0.5">
                    <p className="text-[13px] leading-relaxed text-content-tertiary italic font-medium break-words">
                      {order.remarks}
                    </p>
                  </div>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
