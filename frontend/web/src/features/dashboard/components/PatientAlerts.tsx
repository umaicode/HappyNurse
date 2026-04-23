'use client'

import { useState } from "react";
import { Search, Info } from "lucide-react";
import { Input } from "@/components/ui/input";

export function PatientAlerts() {
  const [searchQuery, setSearchQuery] = useState("");

  return (
    <div className="flex flex-col h-full bg-[var(--color-surface-base)]">
      {/* Search Header */}
      <div className="bg-white/95 backdrop-blur-md sticky top-0 z-30 flex flex-col gap-2 p-3 border-b border-border-base">
        <div className="relative group px-1">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted group-focus-within:text-[var(--color-brand-primary)] transition-colors z-10" />
          <Input
            type="text"
            placeholder="🛠️ 알림 검색..."
            className="pl-9 bg-[var(--color-surface-base)] border-border-base h-10 text-[13px] focus-visible:ring-1 focus-visible:ring-[var(--color-brand-primary)] rounded-md"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      {/* Alerts List */}
      <div className="flex-1 overflow-y-auto p-2">
        <div className="flex flex-col items-center justify-center py-10 text-content-muted gap-2 opacity-30">
          <Info className="w-6 h-6" />
          <p className="text-[11px] font-medium">새 알림 없음</p>
        </div>
      </div>
    </div>
  );
}
