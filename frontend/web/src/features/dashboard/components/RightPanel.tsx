'use client'

import { useState } from "react";
import { cn } from "@/lib/utils";
import { STTPanel } from "./STTPanel";
import { PatientAlerts } from "./PatientAlerts";
import { IVTimerPanel } from "./IVTimerPanel";

type TabType = 'orders' | 'alerts' | 'iv-timer';

const TABS: { id: TabType; label: string }[] = [
  { id: 'orders', label: '의사 오더' },
  { id: 'iv-timer', label: '수액 타이머' },
  { id: 'alerts', label: '알림' },
];

interface RightPanelProps {
  encounterId: number | null;
}

export function RightPanel({ encounterId }: RightPanelProps) {
  const [activeTab, setActiveTab] = useState<TabType>('orders');

  return (
    <div className="flex flex-col h-full bg-surface-base">
      <div className="px-3 pt-3 pb-1 bg-surface-base/90 backdrop-blur-md">
        <div className="flex p-1 bg-surface-hover rounded-lg">
          {TABS.map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={cn(
                  "flex-1 py-2 text-[15px] font-bold rounded-md transition-all flex items-center justify-center",
                  isActive
                    ? "bg-white text-brand-primary shadow-sm"
                    : "text-content-muted hover:text-content-secondary",
                )}
              >
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
