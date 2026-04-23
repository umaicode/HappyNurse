'use client'

import { cn } from "@/lib/utils";

type Order = {
  id: number;
  category: string;
  code: string;
  name: string;
  dose: string;
  frequency: string;
  unit: string;
  method: string;
  status: string;
  remarks: string;
};

type OrderTabProps = {
  orders: Order[];
};

export function OrderTab({ orders }: OrderTabProps) {
  return (
    <div className="flex-1 overflow-auto bg-[var(--color-surface-card)] min-h-0 relative text-body-base">
      <div className="min-w-[1100px] flex flex-col h-full">
        {/* Header Row - Doctor Orders (Matched with Nursing Log Header) */}
        <div className="grid grid-cols-[80px_100px_1fr_80px_60px_60px_80px_100px_180px] gap-2 px-4 py-1.5 bg-[var(--color-surface-hover)] border-b border-[var(--color-border-base)] text-body-sm font-extrabold text-[var(--color-content-secondary)] sticky top-0 z-20 tracking-tight shadow-sm">
          <div className="text-center border-r border-[var(--color-border-base)]/50">구분</div>
          <div className="border-r border-[var(--color-border-base)]/50 pl-2">처방코드</div>
          <div className="border-r border-[var(--color-border-base)]/50 pl-2">처방명칭</div>
          <div className="text-center border-r border-[var(--color-border-base)]/50">1회량</div>
          <div className="text-center border-r border-[var(--color-border-base)]/50">횟수</div>
          <div className="text-center border-r border-[var(--color-border-base)]/50">단위</div>
          <div className="text-center border-r border-[var(--color-border-base)]/50">용법</div>
          <div className="text-center border-r border-[var(--color-border-base)]/50">진행상태</div>
          <div className="pl-2">참고사항</div>
        </div>

        {/* Order Rows (Matched with Nursing Log Row Style) */}
        <div className="flex flex-col flex-1 pb-10">
          {orders.map((order) => (
            <div
              key={order.id}
              className="grid grid-cols-[80px_100px_1fr_80px_60px_60px_80px_100px_180px] gap-2 px-4 py-3 border-b border-[var(--color-border-base)]/50 items-center hover:bg-[var(--color-surface-hover)]/30 transition-all text-body-sm text-[var(--color-content-secondary)]"
            >
              <div className="text-center font-bold text-[var(--color-content-tertiary)]">
                {order.category}
              </div>
              <div className="font-mono font-bold text-[var(--color-content-primary)] pl-2">{order.code}</div>
              <div className="font-medium text-[var(--color-content-secondary)] truncate pl-2">{order.name}</div>
              <div className="text-center font-mono font-bold">{order.dose}</div>
              <div className="text-center font-mono font-bold">{order.frequency}</div>
              <div className="text-center text-[var(--color-content-tertiary)]">{order.unit}</div>
              <div className="text-center text-[var(--color-content-primary)] font-bold">{order.method}</div>
              <div className="flex justify-center">
                <span className={cn(
                  "text-[13px] font-semibold",
                  order.status === "진행" ? "text-[var(--color-brand-primary)]" :
                  order.status === "완료" ? "text-slate-500" :
                  order.status === "검사중" ? "text-amber-600" :
                  "text-slate-500"
                )}>
                  {order.status}
                </span>
              </div>
              <div className="text-[var(--color-content-tertiary)] text-[12px] truncate pl-2">
                {order.remarks}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
