'use client'

import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  Search,
  ClipboardCheck,
  Check,
  ClipboardList
} from "lucide-react";
import { useState, useMemo, useRef } from "react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Heading } from "@/components/ui/heading";
import { Text } from "@/components/ui/text";
import { Badge } from "@/components/ui/badge";
import { INITIAL_RECORDS } from "@/mockup/emr-data";

type NursingRecord = {
  id: number;
  time: string;
  content: string;
  isHandover?: boolean;
  isAISuggested?: boolean;
};

// Nurse-Centric Mock Data (기록은 INITIAL_RECORDS에서 patientId로 조회)
const HANDOVER_PATIENTS = [
  {
    id: "p1",
    name: "김가민",
    patientNo: "2026-00125",
    birthDate: "1999.05.20 (25세/F)",
    room: "7101-01",
    mainSymptom: "Acute Appendicitis (급성 충수염)",
    assignedNurse: "김영희",
    recentVitals: { status: "abnormal", detail: "BT 38.2℃ (14:30)" },
  },
  {
    id: "p2",
    name: "이철수",
    patientNo: "2026-00342",
    birthDate: "1982.11.03 (42세/M)",
    room: "7101-02",
    mainSymptom: "Post-op Gastrectomy (위절제술 후)",
    assignedNurse: "김영희",
    recentVitals: { status: "normal", detail: "안정적" },
  },
  {
    id: "p3",
    name: "박영희",
    patientNo: "2026-00088",
    birthDate: "1956.07.15 (68세/F)",
    room: "7101-03",
    mainSymptom: "Pneumonia (폐렴)",
    assignedNurse: "김영희",
    recentVitals: { status: "abnormal", detail: "SpO2 91% (RA, 13:00)" },
  }
];

// 환자별 간호 기록을 조회 (patientId 미지정 레코드는 p1 소속)
function getRecordsForPatient(patientId: string): NursingRecord[] {
  return INITIAL_RECORDS
    .filter((record) => {
      const pid = (record as { patientId?: string }).patientId;
      return (pid || "p1") === patientId;
    })
    .map((record) => ({
      id: record.id,
      time: record.time,
      content: record.content,
      isHandover: (record as { isHandover?: boolean }).isHandover,
      isAISuggested: (record as { isAISuggested?: boolean }).isAISuggested,
    }))
    .sort((a, b) => a.time.localeCompare(b.time));
}

export function HandoverView() {
  const router = useRouter();
  const currentUser = typeof window !== 'undefined' ? localStorage.getItem("currentUser") || "김영희" : "김영희";
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedPatientId, setSelectedPatientId] = useState<string | null>(null);
  const cardRefs = useRef<Record<string, HTMLDivElement | null>>({});

  const myPatients = useMemo(
    () => HANDOVER_PATIENTS.filter(p => p.assignedNurse === currentUser),
    [currentUser]
  );

  const filteredPatients = useMemo(() => {
    return myPatients.filter(p =>
      p.name.includes(searchQuery) ||
      p.patientNo.includes(searchQuery) ||
      p.room.includes(searchQuery)
    );
  }, [searchQuery, myPatients]);

  const handleSelectPatient = (id: string) => {
    setSelectedPatientId(id);
    cardRefs.current[id]?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  return (
    <div className="flex flex-col h-screen bg-[#F4F7FB] font-sans">
      {/* Top Header */}
      <header className="h-16 flex items-center justify-between px-6 bg-white border-b border-border-base shrink-0 z-50 shadow-sm">
        <div className="flex items-center gap-4">
          <button
            onClick={() => router.push("/dashboard")}
            className="p-2 rounded-full hover:bg-slate-100 transition-all text-content-muted"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="flex items-center gap-2">
            <ClipboardList className="w-6 h-6 text-[var(--color-brand-primary)]" />
            <Heading level="h2" className="text-[22px] font-bold text-slate-800">스마트 인수인계</Heading>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted" />
            <Input
              placeholder="🛠️ 환자명, 병실 검색..."
              className="pl-9 bg-slate-50 border-border-base h-10 text-[14px] focus-visible:ring-1 focus-visible:ring-[var(--color-brand-primary)]"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <Button className="gap-2 bg-[var(--color-brand-primary)] hover:bg-[var(--color-brand-hover)] font-bold shadow-lg">
            <ClipboardCheck className="w-4 h-4" />
            인수인계 완료
          </Button>
        </div>
      </header>

      <main className="flex-1 min-h-0 flex overflow-hidden">
        {/* Left Sidebar — 담당 환자 목록 */}
        <aside className="w-72 shrink-0 bg-white border-r border-border-base flex flex-col">
          <div className="px-5 py-4 border-b border-border-base shrink-0">
            <div className="flex items-center justify-between">
              <span className="text-[15px] font-bold text-slate-800">🛠️ 담당 환자</span>
              <span className="text-[13px] font-semibold text-[var(--color-brand-primary)] bg-[var(--color-brand-primary)]/10 px-2.5 py-0.5 rounded-full">
                {myPatients.length}명
              </span>
            </div>
            <p className="mt-1.5 text-[12px] text-slate-400">클릭하면 해당 환자 카드로 이동해요</p>
          </div>
          <div className="flex-1 min-h-0 overflow-y-auto p-3 space-y-2">
            {filteredPatients.length === 0 ? (
              <div className="text-center py-8 text-[13px] text-slate-400">
                담당 환자가 없습니다
              </div>
            ) : (
              filteredPatients.map((p) => {
                const isSelected = selectedPatientId === p.id;
                return (
                  <button
                    key={p.id}
                    onClick={() => handleSelectPatient(p.id)}
                    className={cn(
                      "w-full text-left px-4 py-3 rounded-xl transition-all relative group",
                      isSelected
                        ? "bg-[var(--color-surface-card)] shadow-md"
                        : "bg-transparent hover:bg-[var(--color-surface-hover)]"
                    )}
                  >
                    {isSelected && (
                      <span className="absolute left-0 top-2.5 bottom-2.5 w-1 rounded-r bg-[var(--color-brand-primary)]" />
                    )}
                    <div className="flex items-center justify-between gap-2">
                      <span className={cn(
                        "text-[16px] font-semibold truncate",
                        isSelected ? "text-[var(--color-brand-primary)]" : "text-slate-800"
                      )}>
                        {p.name}
                      </span>
                      <span className="text-[12px] text-slate-400 shrink-0">{p.room}</span>
                    </div>
                    <div className="mt-1.5">
                      <span className="text-[13px] text-slate-500 truncate block">{p.mainSymptom}</span>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </aside>

        {/* Right Content Area */}
        <div className="flex-1 min-h-0 p-8 overflow-hidden">
        <div className="max-w-6xl mx-auto h-full flex flex-col gap-6">
          {/* Patient Cards Area */}
          <div className="flex-1 min-h-0 overflow-y-auto pr-2 custom-scrollbar space-y-6 pb-10">
            {filteredPatients.map((p) => (
              <div
                key={p.id}
                ref={(el) => { cardRefs.current[p.id] = el; }}
                className={cn(
                  "bg-white rounded-2xl overflow-hidden flex flex-col transition-all scroll-mt-4",
                  selectedPatientId === p.id
                    ? "shadow-xl"
                    : "shadow-sm hover:shadow-md"
                )}>
                {/* [CARD HEADER] */}
                <div className="px-5 py-3.5 bg-slate-50/60 border-b border-border-base flex items-center gap-4">
                  <div className="flex items-baseline gap-2.5">
                    <Heading level="h3" className="text-2xl font-bold text-slate-800">{p.name}</Heading>
                    <Text className="text-[15px] text-slate-500 font-medium">{p.birthDate}</Text>
                  </div>
                  <div className="w-px h-5 bg-border-base" />
                  <Badge className="bg-slate-100 text-slate-600 font-medium border-none text-[13px] px-2.5 py-0.5 hover:bg-slate-100">{p.room}</Badge>

                  <Badge
                    className="ml-auto bg-[var(--color-brand-surface)] text-[var(--color-brand-primary)] font-semibold border-none text-[13px] px-3 py-1 hover:bg-[var(--color-brand-surface)]"
                    title="주요 증상"
                  >
                    {p.mainSymptom}
                  </Badge>
                </div>

                {/* [SHIFT CONTENT] — 간호 기록에서 직접 조회 */}
                {(() => {
                  const records = getRecordsForPatient(p.id);
                  const handoverRecords = records.filter((record) => record.isHandover);
                  const aiSuggestedRecords = records.filter((record) => record.isAISuggested);
                  return (
                    <div className="flex divide-x divide-[var(--color-border-subtle)]">
                      {/* LEFT: 인수인계 내용 — 간호사가 직접 체크한 기록 */}
                      <div className="flex-1 p-5 space-y-3.5">
                        <div className="flex items-center gap-2 text-[var(--color-brand-primary)]">
                          <div className="w-1 h-4 bg-[var(--color-brand-primary)] rounded-full" />
                          <Heading level="h4" className="text-[16px] font-bold">인수인계 내용</Heading>
                        </div>
                        {handoverRecords.length === 0 ? (
                          <Text className="text-[14px] text-[var(--color-content-muted)]">인수인계로 선택된 기록이 없습니다.</Text>
                        ) : (
                          <ul className="space-y-2">
                            {handoverRecords.map((record) => (
                              <li
                                key={record.id}
                                className="flex gap-2.5 p-3 rounded-xl border bg-[var(--color-surface-card)] border-[var(--color-border-subtle)]"
                              >
                                <Check className="size-4 text-[var(--color-brand-primary)] mt-0.5 shrink-0" />
                                <div className="flex-1 min-w-0">
                                  <span className="text-[12px] font-mono font-semibold text-[var(--color-content-muted)] block mb-0.5">{record.time}</span>
                                  <Text className="text-[14px] leading-relaxed text-[var(--color-content-secondary)] whitespace-pre-wrap">{record.content}</Text>
                                </div>
                              </li>
                            ))}
                          </ul>
                        )}
                      </div>

                      {/* RIGHT: AI 제안 — 체크를 깜박했지만 중요할 것 같은 기록 */}
                      <div className="flex-1 p-5 space-y-3.5">
                        <div className="flex items-center gap-2 text-[var(--color-sub-primary)]">
                          <div className="w-1 h-4 bg-[var(--color-sub-primary)] rounded-full" />
                          <Heading level="h4" className="text-[16px] font-bold">AI 제안</Heading>
                          <span className="ml-1 text-[11px] font-semibold text-[var(--color-brand-primary)] bg-[var(--color-brand-surface)] px-2 py-0.5 rounded-full">AI</span>
                        </div>
                        {aiSuggestedRecords.length === 0 ? (
                          <Text className="text-[14px] text-[var(--color-content-muted)]">제안할 내용이 없습니다.</Text>
                        ) : (
                          <ul className="space-y-2">
                            {aiSuggestedRecords.map((record) => (
                              <li
                                key={record.id}
                                className="flex gap-2.5 p-3 rounded-xl border border-dashed border-[var(--color-border-hover)] bg-[var(--color-surface-card)]"
                              >
                                <div className="flex-1 min-w-0">
                                  <span className="text-[12px] font-mono font-semibold text-[var(--color-content-muted)] block mb-0.5">{record.time}</span>
                                  <Text className="text-[14px] leading-relaxed text-[var(--color-content-secondary)] whitespace-pre-wrap">{record.content}</Text>
                                </div>
                              </li>
                            ))}
                          </ul>
                        )}
                      </div>
                    </div>
                  );
                })()}
              </div>
            ))}
          </div>
        </div>
        </div>
      </main>
    </div>
  );
}
