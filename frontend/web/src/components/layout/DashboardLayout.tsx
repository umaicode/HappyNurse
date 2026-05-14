"use client";

import { ReactNode, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Plus,
  Share2,
  UserPlus,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

interface DashboardLayoutProps {
  sidebar: ReactNode;
  mainGrid: ReactNode;
  actionPanel: ReactNode;
  isLeftOpen: boolean;
  isRightOpen: boolean;
  onToggleLeft: () => void;
  onOpenRight: () => void;
  onToggleRight: () => void;
  onOpenAssignModal: () => void;
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
    label: "AI 인수인계 리포트",
    color: "text-emerald-500",
  },
  {
    id: "assign-patient",
    icon: UserPlus,
    label: "담당 환자 설정",
    color: "text-brand-primary",
  },
];

export function DashboardLayout({
  sidebar,
  mainGrid,
  actionPanel,
  isLeftOpen,
  isRightOpen,
  onToggleLeft,
  onToggleRight,
  onOpenAssignModal,
}: DashboardLayoutProps) {
  const [isOpen, setIsOpen] = useState(false);
  const router = useRouter();

  const handleActionClick = (actionId: QuickActionId) => {
    setIsOpen(false);
    if (actionId === "handover") {
      router.push("/handover");
    } else if (actionId === "assign-patient") {
      onOpenAssignModal();
    }
  };

  return (
    <div className="fixed inset-0 overflow-hidden bg-accent text-content-primary p-[6px] gap-[6px] flex">
      {/* 1. Left (탐색창 - Patient List) */}
      <aside
        className={`${isLeftOpen ? "w-[240px] border" : "w-0 border-0"} flex-shrink-0 bg-surface-base rounded-[6px] flex flex-col shadow-[0_2px_8px_rgba(0,0,0,0.03)] overflow-hidden border-border-base/60 transition-[width] duration-300 ease-in-out`}
      >
        <div className="w-[240px] h-full flex flex-col">{sidebar}</div>
      </aside>

      {/* 2. Center (메인 워크스페이스 - Nursing Record) */}
      <main className="flex-1 flex flex-col min-w-0 relative z-10">
        {mainGrid}

        {/* 좌측 패널 토글 — main 의 좌측 경계(= 좌측 사이드바 우측 경계선) 위에 걸침 */}
        <button
          type="button"
          onClick={onToggleLeft}
          aria-label={isLeftOpen ? "좌측 사이드바 접기" : "좌측 사이드바 펼치기"}
          className="absolute left-0 top-1/2 -translate-x-1/2 -translate-y-1/2 z-20 flex h-10 w-6 items-center justify-center rounded-md bg-white/95 backdrop-blur border border-border-base/60 text-content-secondary shadow-md hover:bg-surface-hover hover:text-content-primary transition-colors"
        >
          {isLeftOpen ? (
            <ChevronLeft className="h-4 w-4" />
          ) : (
            <ChevronRight className="h-4 w-4" />
          )}
        </button>

        {/* 우측 패널 토글 — main 의 우측 경계(= 우측 사이드바 좌측 경계선) 위에 걸침 */}
        <button
          type="button"
          onClick={onToggleRight}
          aria-label={isRightOpen ? "우측 패널 접기" : "우측 패널 펼치기"}
          className="absolute right-0 top-1/2 translate-x-1/2 -translate-y-1/2 z-20 flex h-10 w-6 items-center justify-center rounded-md bg-white/95 backdrop-blur border border-border-base/60 text-content-secondary shadow-md hover:bg-surface-hover hover:text-content-primary transition-colors"
        >
          {isRightOpen ? (
            <ChevronRight className="h-4 w-4" />
          ) : (
            <ChevronLeft className="h-4 w-4" />
          )}
        </button>
      </main>

      {/* 3. Right (보조창 - Doctor's Order) */}
      <aside
        className={`${isRightOpen ? "w-[280px] border" : "w-0 border-0"} flex-shrink-0 bg-surface-base rounded-[6px] flex flex-col shadow-[0_2px_8px_rgba(0,0,0,0.03)] overflow-hidden border-border-base/60 transition-[width] duration-300 ease-in-out`}
      >
        <div className="w-[280px] h-full flex flex-col">{actionPanel}</div>
      </aside>

      {/* Floating Action Button (FAB) */}
      <div className="absolute bottom-8 right-8 z-[100]">
        <Popover open={isOpen} onOpenChange={setIsOpen}>
          <PopoverTrigger asChild>
            <button
              className="group flex h-14 w-14 items-center justify-center rounded-full bg-brand-primary text-white transition-shadow hover:shadow-[0_8px_30px_rgb(21,40,159,0.3)] outline-none shadow-2xl"
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
            className="w-56 p-1.5 bg-white border border-border-base shadow-2xl rounded-xl overflow-hidden animate-in fade-in zoom-in slide-in-from-bottom-4 duration-200"
          >
            <div className="flex flex-col gap-0.5">
              <div className="px-3 py-2 text-[11px] font-bold text-content-muted uppercase tracking-wider border-b border-border-subtle mb-1">
                Quick Actions
              </div>
              {QUICK_ACTIONS.map((action) => (
                <button
                  key={action.id}
                  onClick={() => handleActionClick(action.id)}
                  className="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg hover:bg-surface-hover transition-colors text-left group/action"
                >
                  <div
                    className={`flex h-8 w-8 items-center justify-center rounded-full bg-surface-base border border-border-subtle ${action.color}`}
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

    </div>
  );
}
