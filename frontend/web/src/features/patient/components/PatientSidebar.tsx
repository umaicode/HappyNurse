'use client'

import { Search, LogOut, ChevronRight, PanelLeftClose } from "lucide-react";
import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { INITIAL_RECORDS } from "@/mockup/emr-data";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";

// Mock Data updated with assignedNurse
const MOCK_WARDS = [
  {
    id: "71",
    name: "🛠️ 71병동 (내과)",
    rooms: [
      {
        id: "7101",
        name: "7101호",
        capacity: 6,
        patients: [
          { id: "p1", name: "김가민", age: 25, gender: "F", birthday: "1999.05.20", assignedNurse: "김영희", unconfirmedCount: 0 },
          { id: "p2", name: "이철수", age: 42, gender: "M", birthday: "1982.11.03", assignedNurse: "이수진", unconfirmedCount: 1 },
          { id: "p3", name: "박영희", age: 68, gender: "F", birthday: "1956.07.15", assignedNurse: "김영희", unconfirmedCount: 0 },
          { id: "p4", name: "최민호", age: 31, gender: "M", birthday: "1993.02.28", assignedNurse: "김영희", unconfirmedCount: 0 },
          { id: "p5", name: "정수연", age: 54, gender: "F", birthday: "1970.09.12", assignedNurse: "이수진", unconfirmedCount: 0 },
          { id: "p6", name: "강태우", age: 47, gender: "M", birthday: "1977.04.05", assignedNurse: "박민지", unconfirmedCount: 0 },
        ],
      },
      {
        id: "7102",
        name: "7102호",
        capacity: 6,
        patients: [
          { id: "p11", name: "한지민", age: 29, gender: "F", birthday: "1995.12.25", assignedNurse: "김영희", unconfirmedCount: 0 },
          { id: "p12", name: "윤도현", age: 52, gender: "M", birthday: "1972.03.14", assignedNurse: "최지원", unconfirmedCount: 2 },
        ],
      },
    ],
  },
];

interface PatientSidebarProps {
  onCollapse?: () => void;
}

export function PatientSidebar({ onCollapse }: PatientSidebarProps = {}) {
  const router = useRouter();
  const currentUser = typeof window !== 'undefined' ? localStorage.getItem("currentUser") || "김영희" : "김영희";
  const [activePatientId, setActivePatientId] = useState("p1");
  const [searchQuery, setSearchQuery] = useState("");
  const [isMyPatientsOpen, setIsMyPatientsOpen] = useState(true);
  const [isAllPatientsOpen, setIsAllPatientsOpen] = useState(true);

  // Flatten and separate patients
  const allPatients = useMemo(() => MOCK_WARDS.flatMap(ward => ward.rooms.flatMap(room => room.patients)), []);

  const myPatients = useMemo(() =>
    allPatients.filter(p => p.assignedNurse === currentUser),
  [allPatients, currentUser]);

  const otherPatients = useMemo(() =>
    allPatients.filter(p => p.assignedNurse !== currentUser),
  [allPatients, currentUser]);

  const duplicateNames = useMemo(() => allPatients.reduce((acc: {[key: string]: number}, p) => {
    acc[p.name] = (acc[p.name] || 0) + 1;
    return acc;
  }, {}), [allPatients]);

  // 환자별 미확정 기록 개수 계산 (patientId 미지정 레코드는 p1 소속)
  const unconfirmedCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    INITIAL_RECORDS.forEach((record) => {
      if (record.isConfirmed) return;
      const patientId = (record as { patientId?: string }).patientId || "p1";
      counts[patientId] = (counts[patientId] || 0) + 1;
    });
    return counts;
  }, []);

  const filteredMyPatients = myPatients.filter(p => p.name.includes(searchQuery));
  const filteredOtherPatients = otherPatients.filter(p => p.name.includes(searchQuery));

  return (
    <div className="flex flex-col h-full bg-[var(--color-surface-base)]">
      {/* Brand & Search Header */}
      <div className="p-3 border-b border-border-base flex flex-col gap-3">
        <div className="flex items-center justify-between px-1">
          <img src="/images/logo_ic.png" alt="해피너스 로고" className="h-5 w-auto object-contain" />
          {onCollapse && (
            <button
              type="button"
              onClick={onCollapse}
              aria-label="좌측 사이드바 접기"
              className="flex h-7 w-7 items-center justify-center rounded-md text-content-muted hover:bg-[var(--color-surface-hover)] hover:text-content-primary transition"
            >
              <PanelLeftClose className="h-4 w-4" />
            </button>
          )}
        </div>

        <div className="relative">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-content-muted z-10" />
          <Input
            type="text"
            placeholder="🛠️ 환자명 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8 bg-white border-border-subtle shadow-sm h-8 text-body-sm focus-visible:ring-1 focus-visible:ring-[var(--color-brand-primary)]"
          />
        </div>
      </div>

      {/* Patient Lists with Toggle Sections */}
      <div className="flex-1 overflow-y-auto overflow-x-hidden flex flex-col">

        {/* 1. My Patients Section */}
        <Collapsible open={isMyPatientsOpen} onOpenChange={setIsMyPatientsOpen} className="w-full">
          <CollapsibleTrigger className="w-full px-4 py-2.5 flex items-center justify-between bg-white border-b border-border-subtle group hover:bg-slate-50 transition-colors">
            <div className="flex items-center gap-2">
              <span className="text-[14px] font-black text-[var(--color-brand-primary)]">🛠️ 내 담당 환자</span>
              <span className="text-[11px] font-bold bg-[var(--color-brand-surface)] text-[var(--color-brand-primary)] px-2 py-0.5 rounded-full">{filteredMyPatients.length}</span>
            </div>
            <ChevronRight className={cn("w-4 h-4 text-content-muted transition-transform duration-200", isMyPatientsOpen && "rotate-90")} />
          </CollapsibleTrigger>
          <CollapsibleContent>
            <div className="flex flex-col bg-white">
              {filteredMyPatients.map(patient => (
                <PatientItem
                  key={patient.id}
                  patient={patient}
                  isActive={activePatientId === patient.id}
                  isDuplicate={duplicateNames[patient.name] > 1}
                  unconfirmedCount={unconfirmedCounts[patient.id] || 0}
                  onClick={() => setActivePatientId(patient.id)}
                />
              ))}
            </div>
          </CollapsibleContent>
        </Collapsible>

        {/* 2. All Other Patients Section */}
        <Collapsible open={isAllPatientsOpen} onOpenChange={setIsAllPatientsOpen} className="w-full">
          <CollapsibleTrigger className="w-full px-4 py-2.5 flex items-center justify-between bg-slate-50/50 border-b border-border-subtle group hover:bg-slate-50 transition-colors">
            <div className="flex items-center gap-2">
              <span className="text-[14px] font-black text-content-secondary">🛠️ 전체 환자</span>
              <span className="text-[11px] font-bold bg-slate-200 text-content-muted px-2 py-0.5 rounded-full">{filteredOtherPatients.length}</span>
            </div>
            <ChevronRight className={cn("w-4 h-4 text-content-muted transition-transform duration-200", isAllPatientsOpen && "rotate-90")} />
          </CollapsibleTrigger>
          <CollapsibleContent>
            <div className="flex flex-col bg-white">
              {filteredOtherPatients.map(patient => (
                <PatientItem
                  key={patient.id}
                  patient={patient}
                  isActive={activePatientId === patient.id}
                  isDuplicate={duplicateNames[patient.name] > 1}
                  unconfirmedCount={unconfirmedCounts[patient.id] || 0}
                  onClick={() => setActivePatientId(patient.id)}
                />
              ))}
            </div>
          </CollapsibleContent>
        </Collapsible>
      </div>

      {/* User Profile & Logout (Sticky Bottom) */}
      <div className="p-3 border-t border-border-base bg-white">
        <div className="flex items-center justify-between gap-3 p-2.5 bg-[var(--color-surface-base)] rounded-xl border border-border-subtle/50 transition-all hover:border-[var(--color-brand-primary)]/20">
          <div className="flex flex-col min-w-0 pl-1">
            <span className="text-[14px] font-black text-content-primary truncate leading-tight">{currentUser}</span>
            <span className="text-[10px] font-bold text-content-muted uppercase tracking-wider mt-0.5">RN / Charge Nurse</span>
          </div>

          <button
            onClick={() => {
              localStorage.removeItem("currentUser");
              router.push("/");
            }}
            className="p-2 text-[var(--color-brand-primary)] hover:bg-[var(--color-brand-surface)] rounded-xl transition-all shadow-xs border border-[var(--color-brand-primary)]/10"
            title="로그아웃"
          >
            <LogOut className="size-5" />
          </button>
        </div>
      </div>
    </div>
  );
}

interface Patient {
  id: string;
  name: string;
  age: number;
  gender: string;
  birthday: string;
  assignedNurse: string;
  unconfirmedCount: number;
}

interface PatientItemProps {
  patient: Patient;
  isActive: boolean;
  isDuplicate: boolean;
  unconfirmedCount?: number;
  onClick: () => void;
}

function PatientItem({ patient, isActive, isDuplicate, unconfirmedCount = 0, onClick }: PatientItemProps) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "flex items-center justify-between w-full px-4 py-2.5 text-left transition-colors relative border-b border-border-subtle/20",
        isActive
          ? "bg-[var(--color-brand-surface)]/60 border-l-[4px] border-l-[var(--color-brand-primary)]"
          : "hover:bg-slate-50 bg-white border-l-[4px] border-l-transparent",
      )}
    >
      <div className="flex flex-col gap-1 min-w-0 flex-1">
        <div className="flex items-center gap-3">
          <div className="relative inline-block shrink-0">
            <span className={cn("text-base tracking-tight truncate", isActive ? "font-bold text-[var(--color-sub-primary)]" : "font-semibold text-content-secondary")}>
              {patient.name}
            </span>
            {isDuplicate && (
              <div className="absolute top-0 -right-1 size-1 rounded-full bg-[var(--color-brand-primary)] shadow-[0_0_4px_var(--color-brand-primary)]/40" title="동명이인" />
            )}
          </div>
          <div className={cn("flex items-center gap-1 text-[13px] font-mono shrink-0", isActive ? "text-[var(--color-sub-primary)]/70 font-bold" : "text-content-muted")}>
            <span>{patient.gender}</span>
            <span>/</span>
            <span>{patient.birthday ? patient.birthday.substring(2) : "00.01.01"}</span>
          </div>
        </div>
      </div>
      {unconfirmedCount > 0 && (
        <span
          title={`확정 전 기록 ${unconfirmedCount}건`}
          className="flex items-center justify-center min-w-[20px] h-[20px] px-1.5 bg-[var(--color-brand-surface)] text-[var(--color-brand-primary)] text-[11px] font-bold rounded-full shrink-0 ml-2"
        >
          {unconfirmedCount}
        </span>
      )}
    </button>
  );
}
