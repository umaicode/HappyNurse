'use client'

import { ReactNode, useState } from "react";
import { useRouter } from "next/navigation";
import {
  Plus,
  HeartPulse,
  Pill,
  Share2,
  PanelLeftOpen,
  PanelRightOpen,
} from "lucide-react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";

interface DashboardLayoutProps {
  sidebar: ReactNode;
  mainGrid: ReactNode;
  actionPanel: ReactNode;
  isLeftOpen: boolean;
  isRightOpen: boolean;
  onOpenLeft: () => void;
  onOpenRight: () => void;
}

const QUICK_ACTIONS = [
  {
    icon: HeartPulse,
    label: "바이탈 측정",
    color: "text-red-500",
    path: "/vitals",
  },
  {
    icon: Pill,
    label: "투약 기록",
    color: "text-orange-500",
    path: "/medication",
  },
  {
    icon: Share2,
    label: "환자 인수인계",
    color: "text-emerald-500",
    path: "/handover",
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
}: DashboardLayoutProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isAlertOpen, setIsAlertOpen] = useState(false);
  const [pendingAction, setPendingAction] = useState("");
  const router = useRouter();

  const handleActionClick = (action: typeof QUICK_ACTIONS[0]) => {
    setIsOpen(false);
    if (action.label === "환자 인수인계") {
      router.push(action.path || "/handover");
    } else {
      setPendingAction(action.label);
      setIsAlertOpen(true);
    }
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
              {QUICK_ACTIONS.map((action, index) => (
                <button
                  key={index}
                  onClick={() => handleActionClick(action)}
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

      {/* Coming Soon Alert Dialog */}
      <AlertDialog open={isAlertOpen} onOpenChange={setIsAlertOpen}>
        <AlertDialogContent className="max-w-[340px] rounded-2xl p-6 border border-border-base bg-white shadow-2xl">
          <AlertDialogHeader className="flex flex-col items-center justify-center gap-3">
            <div className="size-12 rounded-full bg-[var(--color-brand-surface)] flex items-center justify-center text-[var(--color-brand-primary)] mb-1">
              {pendingAction === "바이탈 측정" ? <HeartPulse className="size-6" /> : <Pill className="size-6" />}
            </div>
            <AlertDialogTitle className="text-xl font-bold text-[var(--color-sub-primary)] text-center">
              준비 중입니다
            </AlertDialogTitle>
            <AlertDialogDescription className="text-body-sm font-medium text-content-tertiary text-center leading-relaxed">
              <span className="text-[var(--color-brand-primary)] font-bold">{pendingAction}</span> 기능은 현재 개발 중입니다.<br />
              더 나은 서비스를 위해 조금만 기다려 주세요!
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter className="mt-6">
            <AlertDialogAction asChild>
              <Button className="w-full h-11 bg-[var(--color-brand-primary)] hover:bg-[var(--color-brand-hover)] text-white font-bold rounded-xl shadow-lg shadow-[var(--color-brand-primary)]/10 transition-all">
                확인
              </Button>
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
