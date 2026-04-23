'use client'

import { useState } from "react";
import { ClipboardList, Bell, PanelRightClose } from "lucide-react";
import { cn } from "@/lib/utils";
import { STTPanel } from "./STTPanel";
import { PatientAlerts } from "./PatientAlerts";

type TabType = 'orders' | 'alerts';

interface RightPanelProps {
  onCollapse?: () => void;
}

export function RightPanel({ onCollapse }: RightPanelProps = {}) {
  const [activeTab, setActiveTab] = useState<TabType>('orders');

  return (
    <div className="flex flex-col h-full bg-[var(--color-surface-base)]">
      {/* Tab Switcher Header - Removed border-b and reduced padding */}
      <div className="px-3 pt-3 pb-1 bg-[var(--color-surface-base)]/90 backdrop-blur-md">
        {onCollapse && (
          <div className="flex justify-end mb-1.5">
            <button
              type="button"
              onClick={onCollapse}
              aria-label="우측 패널 접기"
              className="flex h-7 w-7 items-center justify-center rounded-md text-content-muted hover:bg-[var(--color-surface-hover)] hover:text-content-primary transition"
            >
              <PanelRightClose className="h-4 w-4" />
            </button>
          </div>
        )}
        <div className="flex p-1 bg-[var(--color-surface-hover)] rounded-lg">
          <button
            onClick={() => setActiveTab('orders')}
            className={cn(
              "flex-1 py-2 text-body-sm font-bold rounded-md transition-all flex items-center justify-center gap-1.5",
              activeTab === 'orders'
                ? "bg-white text-[var(--color-brand-primary)] shadow-sm"
                : "text-content-muted hover:text-content-secondary",
            )}
          >
            <ClipboardList className="w-4 h-4" />
            의사 오더
          </button>
          <button
            onClick={() => setActiveTab('alerts')}
            className={cn(
              "flex-1 py-2 text-body-sm font-bold rounded-md transition-all flex items-center justify-center gap-1.5",
              activeTab === 'alerts'
                ? "bg-white text-[var(--color-brand-primary)] shadow-sm"
                : "text-content-muted hover:text-content-secondary"
            )}
          >
            <Bell className="w-4 h-4" />
            환자 알림
          </button>
        </div>
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === 'orders' ? <STTPanel /> : <PatientAlerts />}
      </div>
    </div>
  );
}
