'use client'

import {
  Search,
  ClipboardList,
} from "lucide-react";
import { useState, useMemo } from "react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";

// Data Types
type OrderStatus = "접수" | "진행" | "검사중" | "완료";

interface DoctorOrder {
  id: string;
  category: "수액" | "지시" | "투약" | "LIS" | "영상";
  code: string;
  name: string;
  dose: string;
  frequency: string;
  unit: string;
  method: string;
  status: OrderStatus;
  time: string;
  remarks: string;
  patientName: string;
  isChanged?: boolean;
}

const STATUS_TEXT_COLOR: Record<OrderStatus, string> = {
  접수: "text-slate-500",
  진행: "text-[var(--color-brand-primary)]",
  검사중: "text-amber-600",
  완료: "text-emerald-600",
};

// Mock Orders
// 처방 코드 체계 (원내 코드: 2-letter prefix + 4자리 숫자)
//  - MD: 경구/투약(Medication)
//  - IV: 주사/수액(Injection)
//  - LB: 검체검사(Laboratory)
//  - RD: 영상검사(Radiology)
//  - OR: 일반지시(Order)
const INITIAL_ORDERS: DoctorOrder[] = [
  {
    id: "o-new",
    category: "투약",
    code: "MD1000",
    name: "Acetaminophen 1000mg Tab.",
    dose: "1",
    frequency: "2",
    unit: "tab",
    method: "PO",
    status: "진행",
    time: "15:30",
    remarks: "기존 500mg QD에서 1000mg BID로 변경됨 (통증 조절 목적)",
    patientName: "박민수",
    isChanged: true,
  },
  {
    id: "o1",
    category: "수액",
    code: "IV0901",
    name: "0.9% Sodium Chloride Inj. 1000ml",
    dose: "1000",
    frequency: "1",
    unit: "bag",
    method: "IV",
    status: "진행",
    time: "14:15",
    remarks: "80cc/hr 유지 및 I/O Check 시작 요망.",
    patientName: "김가민",
  },
  {
    id: "o2",
    category: "지시",
    code: "OR0001",
    name: "금식 (수술 전)",
    dose: "-",
    frequency: "-",
    unit: "-",
    method: "-",
    status: "접수",
    time: "14:30",
    remarks: "자정부터 금식 유지. 보호자 안내 완료.",
    patientName: "이영희",
  },
  {
    id: "o3",
    category: "투약",
    code: "MD0500",
    name: "Acetaminophen 500mg Tab.",
    dose: "1",
    frequency: "3",
    unit: "tab",
    method: "PO",
    status: "완료",
    time: "13:00",
    remarks: "식후 30분 투여 완료.",
    patientName: "박민수",
  },
  {
    id: "o4",
    category: "영상",
    code: "RD0449",
    name: "Abdomen CT (with contrast)",
    dose: "-",
    frequency: "1",
    unit: "-",
    method: "-",
    status: "검사중",
    time: "12:30",
    remarks: "영상의학과 연락 대기 중. 동의서 확인 요망.",
    patientName: "최지윤",
  },
];

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
            placeholder="🛠️ 오더 검색..."
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
                  <div className="flex items-baseline gap-2">
                    <span className="text-[12px] font-bold text-content-muted shrink-0">진행 상태</span>
                    <span
                      className={cn(
                        "text-[15px] font-semibold leading-none",
                        STATUS_TEXT_COLOR[order.status],
                      )}
                    >
                      {order.status}
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
