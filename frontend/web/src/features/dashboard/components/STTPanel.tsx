'use client'

import { ChevronDown, ClipboardList, Loader2, Check } from "lucide-react";
import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Command,
  CommandGroup,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { PanelCard } from "./PanelCard";
import {
  ORDER_TYPE_LABEL,
  type OrderType,
} from "@/features/dashboard/types/order";
import { useOrders } from "../hooks/useOrders";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import { formatMonthDayHHmm } from "@/lib/time";

type STTPanelProps = {
  encounterId: number | null;
};

const ORDER_TYPE_OPTIONS: OrderType[] = [
  "MEDICATION",
  "INSTRUCTION",
  "FLUID",
  "TREATMENT",
  "LIS",
  "IMAGE",
];

export function STTPanel({ encounterId }: STTPanelProps) {
  const { data, isPending, isError } = useOrders(encounterId);
  const { data: wardPatients } = useWardPatients();

  // null = 전체. 단일 선택 — 사이드바 폭 한정으로 multi 보다 단일이 깔끔.
  const [filterType, setFilterType] = useState<OrderType | null>(null);
  const [filterOpen, setFilterOpen] = useState(false);

  const patientName = data?.patientName ?? "";
  // encounterId → 호실-침대 ("{roomName-호}-{bedName}") — EMR 헤더와 동일 패턴.
  const roomBed = useMemo(() => {
    if (encounterId === null) return "";
    const match = wardPatients?.find((patient) => patient.encounterId === encounterId);
    if (!match) return "";
    return [match.roomName.replace(/호$/, ""), match.bedName]
      .filter(Boolean)
      .join("-");
  }, [encounterId, wardPatients]);

  const filteredOrders = useMemo(() => {
    const orders = data?.orders ?? [];
    const filtered =
      filterType === null
        ? orders
        : orders.filter((order) => order.orderType === filterType);
    // createdAt desc — 최신 오더가 위.
    return [...filtered].sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
  }, [data, filterType]);

  return (
    <div className="flex flex-col h-full bg-surface-base">
      {/* Filter Header */}
      <div className="bg-white/95 backdrop-blur-md sticky top-0 z-30 flex flex-col gap-2 p-3 border-b border-border-base">
        <Popover open={filterOpen} onOpenChange={setFilterOpen}>
          <PopoverTrigger asChild>
            <button
              type="button"
              className="flex items-center justify-between gap-2 h-10 px-3 mx-1 rounded-md border border-border-base bg-surface-base text-body-xs font-bold text-content-primary hover:bg-surface-hover focus:outline-none focus:ring-1 focus:ring-brand-primary/20 transition-colors"
            >
              <span className="flex items-center gap-1.5">
                <span className="text-content-muted">구분</span>
                <span>
                  {filterType === null ? "전체" : ORDER_TYPE_LABEL[filterType]}
                </span>
              </span>
              <ChevronDown className="size-4 shrink-0 text-content-muted" />
            </button>
          </PopoverTrigger>
          <PopoverContent
            className="p-0 z-[100] shadow-xl border border-border-base bg-surface-card"
            align="start"
            sideOffset={4}
            style={{ width: "var(--radix-popover-trigger-width)" }}
          >
            <Command className="border-none">
              <CommandList className="max-h-[280px] overflow-y-auto p-1">
                <CommandGroup>
                  <CommandItem
                    value="all"
                    onSelect={() => {
                      setFilterType(null);
                      setFilterOpen(false);
                    }}
                    className="flex items-center gap-2 px-2 py-1.5 text-body-xs rounded-sm cursor-pointer data-[selected=true]:bg-brand-surface data-[selected=true]:text-brand-primary"
                  >
                    <Check
                      className={cn(
                        "size-4 shrink-0",
                        filterType === null ? "opacity-100" : "opacity-0",
                      )}
                    />
                    전체
                  </CommandItem>
                  {ORDER_TYPE_OPTIONS.map((type) => (
                    <CommandItem
                      key={type}
                      value={type}
                      onSelect={() => {
                        setFilterType(type);
                        setFilterOpen(false);
                      }}
                      className="flex items-center gap-2 px-2 py-1.5 text-body-xs rounded-sm cursor-pointer data-[selected=true]:bg-brand-surface data-[selected=true]:text-brand-primary"
                    >
                      <Check
                        className={cn(
                          "size-4 shrink-0",
                          filterType === type ? "opacity-100" : "opacity-0",
                        )}
                      />
                      {ORDER_TYPE_LABEL[type]}
                    </CommandItem>
                  ))}
                </CommandGroup>
              </CommandList>
            </Command>
          </PopoverContent>
        </Popover>
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
          <EmptyMessage>
            {filterType === null ? "오더 없음" : "해당 구분의 오더 없음"}
          </EmptyMessage>
        ) : (
          filteredOrders.map((order) => {
            const isChanged = order.updatedAt !== order.createdAt;
            const isCompleted = order.status === "completed";
            return (
              <PanelCard
                key={order.medicationOrderId}
                variantClass={isCompleted ? "opacity-70" : undefined}
              >
                {/* 1행: 타입 라벨 + (변경 칩) | 시간 */}
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5 min-w-0">
                    <span className="text-body-base font-semibold tracking-tight text-brand-primary shrink-0 leading-none">
                      {ORDER_TYPE_LABEL[order.orderType] ?? order.orderType}
                    </span>
                    {isChanged && (
                      <span className="px-1.5 py-0.5 rounded bg-brand-primary text-white text-[10px] font-bold leading-none shrink-0">
                        변경
                      </span>
                    )}
                  </div>
                  <span className="text-body-xs font-medium text-content-tertiary shrink-0 leading-none">
                    {formatMonthDayHHmm(order.createdAt)}
                  </span>
                </div>

                {/* 2행: 처방코드 */}
                <span className="font-mono font-bold text-body-xs text-content-tertiary leading-none">
                  {order.orderCode}
                </span>

                {/* 3행: 처방명 */}
                <p className="text-body-sm font-bold text-content-primary leading-tight break-words">
                  {order.orderName}
                </p>

                {patientName && (
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="text-body-sm font-bold text-content-primary leading-none truncate">
                      {patientName}
                    </span>
                    {roomBed && (
                      <span className="px-1.5 py-0.5 rounded bg-[#F7F8FA] text-content-secondary text-[11px] font-bold leading-none shrink-0">
                        {roomBed}
                      </span>
                    )}
                  </div>
                )}

                {/* Info Grid */}
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
