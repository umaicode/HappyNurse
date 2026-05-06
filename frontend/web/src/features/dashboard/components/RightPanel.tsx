'use client'

import { useState } from "react";
import { ClipboardList, Bell, Droplet } from "lucide-react";
import { cn } from "@/lib/utils";
import { STTPanel } from "./STTPanel";
import { PatientAlerts } from "./PatientAlerts";
import { IVTimerPanel } from "./IVTimerPanel";

type TabType = 'orders' | 'alerts' | 'iv-timer';

const TABS: { id: TabType; label: string; icon: typeof ClipboardList }[] = [
  { id: 'orders', label: '의사 오더', icon: ClipboardList },
  { id: 'alerts', label: '알림', icon: Bell },
  { id: 'iv-timer', label: '수액 타이머', icon: Droplet },
];

interface RightPanelProps {
  encounterId: number | null;
}

export function RightPanel({ encounterId }: RightPanelProps) {
  const [activeTab, setActiveTab] = useState<TabType>('orders');

  return (
    <div className="flex flex-col h-full bg-[var(--color-surface-base)]">
      <div className="px-3 pt-3 pb-1 bg-[var(--color-surface-base)]/90 backdrop-blur-md">
        <div className="flex p-1 bg-[var(--color-surface-hover)] rounded-lg">
          {TABS.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={cn(
                  "flex-1 py-2 text-[13px] font-bold rounded-md transition-all flex items-center justify-center gap-1",
                  isActive
                    ? "bg-white text-[var(--color-brand-primary)] shadow-sm"
                    : "text-content-muted hover:text-content-secondary",
                )}
              >
                <Icon className="w-3.5 h-3.5" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className="flex-1 overflow-hidden">
        {activeTab === 'orders' && <STTPanel encounterId={encounterId} />}
        {activeTab === 'alerts' && <PatientAlerts />}
        {activeTab === 'iv-timer' && <IVTimerPanel />}
      </div>
    </div>
  );
}
