'use client'

import { ReactNode, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Plus,
  Share2,
  UserPlus,
  PanelLeftOpen,
  PanelRightOpen,
} from "lucide-react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { Ward } from "@/mockup/wards";

interface DashboardLayoutProps {
  sidebar: ReactNode;
  mainGrid: ReactNode;
  actionPanel: ReactNode;
  isLeftOpen: boolean;
  isRightOpen: boolean;
  onOpenLeft: () => void;
  onOpenRight: () => void;
  wards: Ward[];
  onAssignPatient: (patientId: string) => void;
}

type QuickActionId = "handover" | "assign-patient";

interface QuickAction {
  id: QuickActionId;
  icon: typeof Share2;
  label: string;
  color: string;
}

const QUICK_ACTIONS: QuickAction[] = [
  {
    id: "handover",
    icon: Share2,
    label: "환자 인수인계",
    color: "text-emerald-500",
  },
  {
    id: "assign-patient",
    icon: UserPlus,
    label: "담당 환자 설정",
    color: "text-[var(--color-brand-primary)]",
  },
];

export function DashboardLayout({
  sidebar,
  mainGrid,
  actionPanel,
  isLeftOpen,
  isRightOpen,
  onOpenLeft,
  onOpenRight,
  wards,
  onAssignPatient,
}: DashboardLayoutProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isAssignOpen, setIsAssignOpen] = useState(false);
  const router = useRouter();

  const handleActionClick = (actionId: QuickActionId) => {
    setIsOpen(false);
    if (actionId === "handover") {
      router.push("/handover");
    } else if (actionId === "assign-patient") {
      setIsAssignOpen(true);
    }
  };

  const handleSelectPatient = (patientId: string) => {
    onAssignPatient(patientId);
    setIsAssignOpen(false);
  };

  return (
    <div className="fixed inset-0 overflow-hidden bg-[var(--color-accent)] text-content-primary p-[6px] gap-[6px] flex">
      {/* 1. Left (탐색창 - Patient List) */}
      <aside
        className={`${isLeftOpen ? "w-[240px] border" : "w-0 border-0"} flex-shrink-0 bg-[var(--color-surface-base)] rounded-[6px] flex flex-col shadow-[0_2px_8px_rgba(0,0,0,0.03)] overflow-hidden border-border-base/60 transition-[width] duration-300 ease-in-out`}
      >
        <div className="w-[240px] h-full flex flex-col">{sidebar}</div>
      </aside>

      {/* 2. Center (메인 워크스페이스 - Nursing Record) */}
      <main className="flex-1 flex flex-col min-w-0 relative z-10">
        {mainGrid}

        {/* Floating "open" button when the left sidebar is collapsed */}
        {!isLeftOpen && (
          <button
            type="button"
            onClick={onOpenLeft}
            aria-label="좌측 사이드바 펼치기"
            className="absolute left-0 top-1/2 -translate-y-1/2 z-20 flex h-10 w-6 items-center justify-center rounded-r-md bg-white/95 backdrop-blur border border-l-0 border-border-base/60 text-content-secondary shadow-md hover:bg-[var(--color-surface-hover)] hover:text-content-primary transition"
          >
            <PanelLeftOpen className="h-4 w-4" />
          </button>
        )}

        {/* Floating "open" button when the right panel is collapsed */}
        {!isRightOpen && (
          <button
            type="button"
            onClick={onOpenRight}
            aria-label="우측 패널 펼치기"
            className="absolute right-0 top-1/2 -translate-y-1/2 z-20 flex h-10 w-6 items-center justify-center rounded-l-md bg-white/95 backdrop-blur border border-r-0 border-border-base/60 text-content-secondary shadow-md hover:bg-[var(--color-surface-hover)] hover:text-content-primary transition"
          >
            <PanelRightOpen className="h-4 w-4" />
          </button>
        )}
      </main>

      {/* 3. Right (보조창 - Doctor's Order) */}
      <aside
        className={`${isRightOpen ? "w-[280px] border" : "w-0 border-0"} flex-shrink-0 bg-[var(--color-surface-base)] rounded-[6px] flex flex-col shadow-[0_2px_8px_rgba(0,0,0,0.03)] overflow-hidden border-border-base/60 transition-[width] duration-300 ease-in-out`}
      >
        <div className="w-[280px] h-full flex flex-col">{actionPanel}</div>
      </aside>

      {/* Floating Action Button (FAB) */}
      <div className="absolute bottom-8 right-8 z-[100]">
        <Popover open={isOpen} onOpenChange={setIsOpen}>
          <PopoverTrigger asChild>
            <button
              className="group flex h-14 w-14 items-center justify-center rounded-full bg-[var(--color-brand-primary)] text-white transition-all hover:scale-110 hover:shadow-[0_8px_30px_rgb(21,40,159,0.3)] active:scale-90 outline-none shadow-2xl"
              aria-label="Quick Actions"
            >
              <Plus
                className={`h-8 w-8 transition-transform duration-300 ${isOpen ? "rotate-45" : "rotate-0"}`}
              />
            </button>
          </PopoverTrigger>
          <PopoverContent
            side="top"
            align="end"
            sideOffset={16}
            className="w-56 p-1.5 bg-white border border-[var(--color-border-base)] shadow-2xl rounded-xl overflow-hidden animate-in fade-in zoom-in slide-in-from-bottom-4 duration-200"
          >
            <div className="flex flex-col gap-0.5">
              <div className="px-3 py-2 text-[11px] font-bold text-content-muted uppercase tracking-wider border-b border-border-subtle mb-1">
                Quick Actions
              </div>
              {QUICK_ACTIONS.map((action) => (
                <button
                  key={action.id}
                  onClick={() => handleActionClick(action.id)}
                  className="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg hover:bg-[var(--color-surface-hover)] transition-all text-left group/action active:scale-95"
                >
                  <div
                    className={`flex h-8 w-8 items-center justify-center rounded-full bg-surface-base border border-border-subtle group-hover/action:scale-110 transition-transform ${action.color}`}
                  >
                    <action.icon className="h-4 w-4" />
                  </div>
                  <span className="text-body-sm font-bold text-content-secondary group-hover/action:text-content-primary">
                    {action.label}
                  </span>
                </button>
              ))}
            </div>
          </PopoverContent>
        </Popover>
      </div>

      {/* 담당 환자 설정 Modal */}
      <Dialog open={isAssignOpen} onOpenChange={setIsAssignOpen}>
        <DialogContent className="max-w-[480px] max-h-[80vh] overflow-hidden flex flex-col rounded-2xl p-0 border border-border-base bg-white shadow-2xl">
          <DialogHeader className="px-6 pt-6 pb-3 border-b border-border-subtle">
            <DialogTitle className="text-xl font-bold text-[var(--color-sub-primary)]">
              담당 환자 설정
            </DialogTitle>
            <DialogDescription className="text-body-sm text-content-tertiary">
              선택한 환자가 내 담당으로 배정됩니다.
            </DialogDescription>
          </DialogHeader>

          <div className="flex-1 overflow-y-auto px-4 py-3">
            {wards.map((ward) => (
              <div key={ward.id} className="flex flex-col gap-2 pb-3 last:pb-0">
                <div className="px-2 text-[12px] font-bold text-content-tertiary tracking-wider uppercase">
                  {ward.name}
                </div>
                {ward.rooms.map((room) => (
                  <div key={room.id} className="flex flex-col">
                    <div className="px-2 py-1 text-[11px] font-bold text-content-muted border-b border-border-subtle mb-1">
                      {room.name}
                    </div>
                    <div className="flex flex-col">
                      {room.patients.map((patient) => (
                        <button
                          key={patient.id}
                          onClick={() => handleSelectPatient(patient.id)}
                          className="flex items-center justify-between w-full px-3 py-2 rounded-md hover:bg-[var(--color-surface-hover)] transition-all text-left group/patient"
                        >
                          <div className="flex items-center gap-3 min-w-0">
                            <span className="text-[14px] font-bold text-content-primary truncate">
                              {patient.name}
                            </span>
                            <span className="text-[12px] font-mono font-bold text-content-muted shrink-0">
                              {patient.gender}/{patient.birthday ? patient.birthday.substring(2) : "00.01.01"}
                            </span>
                          </div>
                          <span className="text-[11px] font-semibold text-content-tertiary shrink-0">
                            담당 {patient.assignedNurse}
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            ))}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
