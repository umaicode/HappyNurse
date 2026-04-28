"use client";

import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  Search,
  Sparkles,
  TrendingUp,
  AlertTriangle,
  Activity,
  ChevronDown,
  ChevronRight,
  FileText,
} from "lucide-react";
import { useState, useMemo, useRef, useEffect } from "react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Heading } from "@/components/ui/heading";
import { Text } from "@/components/ui/text";
import { Badge } from "@/components/ui/badge";
import { INITIAL_RECORDS } from "@/mockup/emr-data";
import { HANDOVER_PATIENTS } from "@/mockup/handover-data";
import { HANDOVER_SUMMARIES } from "@/mockup/handover-summaries";
import { HANDOVER_SHIFT_OVERVIEW } from "@/mockup/handover-overview";
import { MOCK_WARDS } from "@/mockup/wards";
import { loadWards } from "@/lib/ward-assignments";
import type { NursingRecord } from "@/features/dashboard/types/record";
import type {
  HandoverPatient,
  HandoverSummary,
} from "@/features/handover/types/handover";
import type { Ward } from "@/features/patient/types/patient";

// 환자별 간호 기록을 조회 (patientId 미지정 레코드는 p1 소속)
function getRecordsForPatient(patientId: string): NursingRecord[] {
  return INITIAL_RECORDS.filter(
    (record) => (record.patientId || "p1") === patientId,
  )
    .slice()
    .sort((a, b) => a.time.localeCompare(b.time));
}

function findSummary(patientId: string): HandoverSummary | undefined {
  return HANDOVER_SUMMARIES.find((summary) => summary.patientId === patientId);
}

export function HandoverView() {
  const router = useRouter();
  const currentUser =
    typeof window !== "undefined"
      ? localStorage.getItem("currentUser") || "김영희"
      : "김영희";
  const [wards, setWards] = useState<Ward[]>(MOCK_WARDS);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedPatientId, setSelectedPatientId] = useState<string | null>(
    null,
  );
  const [expandedSources, setExpandedSources] = useState<Set<string>>(
    new Set(),
  );
  const cardRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setWards(loadWards());
  }, []);

  // wards 에서 내 담당 환자 id 추출 → HANDOVER_PATIENTS 와 조인 → 가나다순 정렬
  const myPatients = useMemo<HandoverPatient[]>(() => {
    const myIds = new Set(
      wards
        .flatMap((ward) => ward.rooms.flatMap((room) => room.patients))
        .filter((patient) => patient.assignedNurse === currentUser)
        .map((patient) => patient.id),
    );
    return HANDOVER_PATIENTS.filter((hp) => myIds.has(hp.id))
      .slice()
      .sort((a, b) => a.name.localeCompare(b.name, "ko"));
  }, [wards, currentUser]);

  const filteredPatients = useMemo(() => {
    const query = searchQuery.trim();
    if (!query) return myPatients;
    return myPatients.filter(
      (patient) =>
        patient.name.includes(query) ||
        patient.patientNo.includes(query) ||
        patient.room.includes(query),
    );
  }, [searchQuery, myPatients]);

  // 스크롤 스파이: 매 프레임 카드 위치를 계산해 선택 상태 갱신
  // 우선순위: (1) 감지 밴드(상단 20%~30%)에 걸린 top-most 카드
  //          (2) viewport 에 걸친 top-most 카드 (밴드에 하나도 없을 때)
  useEffect(() => {
    const root = scrollContainerRef.current;
    if (!root || filteredPatients.length === 0) return;

    const computeSelected = () => {
      const rootRect = root.getBoundingClientRect();
      const bandTop = rootRect.top + rootRect.height * 0.2;
      const bandBottom = rootRect.top + rootRect.height * 0.3;

      const candidates = filteredPatients
        .map((patient) => ({
          patient,
          rect: cardRefs.current[patient.id]?.getBoundingClientRect(),
        }))
        .filter(
          (entry): entry is { patient: HandoverPatient; rect: DOMRect } =>
            !!entry.rect,
        );

      const inBand = candidates
        .filter((c) => c.rect.bottom >= bandTop && c.rect.top <= bandBottom)
        .sort((a, b) => a.rect.top - b.rect.top);
      if (inBand.length > 0) {
        setSelectedPatientId(inBand[0].patient.id);
        return;
      }

      const inViewport = candidates
        .filter(
          (c) => c.rect.bottom > rootRect.top && c.rect.top < rootRect.bottom,
        )
        .sort((a, b) => a.rect.top - b.rect.top);
      if (inViewport.length > 0) {
        setSelectedPatientId(inViewport[0].patient.id);
      }
    };

    let rafId: number | null = null;
    const handleScroll = () => {
      if (rafId !== null) return;
      rafId = requestAnimationFrame(() => {
        rafId = null;
        computeSelected();
      });
    };
    root.addEventListener("scroll", handleScroll, { passive: true });

    // 마운트 시점 위치 기반 초기 계산
    computeSelected();

    return () => {
      root.removeEventListener("scroll", handleScroll);
      if (rafId !== null) cancelAnimationFrame(rafId);
    };
  }, [filteredPatients]);

  const handleSelectPatient = (id: string) => {
    // 하이라이트는 스크롤 스파이(IntersectionObserver)가 담당한다.
    cardRefs.current[id]?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  };

  const toggleSource = (patientId: string) => {
    setExpandedSources((prev) => {
      const next = new Set(prev);
      if (next.has(patientId)) next.delete(patientId);
      else next.add(patientId);
      return next;
    });
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
            <Heading
              level="h2"
              className="text-[22px] font-bold text-slate-800"
            >
              AI 인수인계 리포트
            </Heading>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted" />
            <Input
              placeholder="환자명, 병실 검색..."
              className="pl-9 bg-slate-50 border-border-base h-10 text-[14px] focus-visible:ring-1 focus-visible:ring-[var(--color-brand-primary)]"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>
      </header>

      <main className="flex-1 min-h-0 flex overflow-hidden">
        {/* Left Sidebar — 담당 환자 목록 */}
        <aside className="w-72 shrink-0 bg-white border-r border-border-base flex flex-col">
          <div className="px-5 py-4 border-b border-border-base shrink-0">
            <div className="flex items-center justify-between">
              <span className="text-[15px] font-bold text-slate-800">
                담당 환자
              </span>
              <span className="text-[13px] font-semibold text-[var(--color-brand-primary)] bg-[var(--color-brand-primary)]/10 px-2.5 py-0.5 rounded-full">
                {myPatients.length}명
              </span>
            </div>
            <p className="mt-1.5 text-[12px] text-slate-400">
              클릭하면 해당 환자 카드로 이동해요
            </p>
          </div>
          <div className="flex-1 min-h-0 overflow-y-auto p-3 space-y-2">
            {filteredPatients.length === 0 ? (
              <div className="text-center py-8 text-[13px] text-slate-400">
                담당 환자가 없습니다
              </div>
            ) : (
              filteredPatients.map((patient) => {
                const isSelected = selectedPatientId === patient.id;
                return (
                  <button
                    key={patient.id}
                    onClick={() => handleSelectPatient(patient.id)}
                    className={cn(
                      "w-full text-left px-4 py-3 rounded-xl transition-all relative group",
                      isSelected
                        ? "bg-[var(--color-surface-card)] shadow-md"
                        : "bg-transparent hover:bg-[var(--color-surface-hover)]",
                    )}
                  >
                    {isSelected && (
                      <span className="absolute left-0 top-2.5 bottom-2.5 w-1 rounded-r bg-[var(--color-brand-primary)]" />
                    )}
                    <div className="flex items-center justify-between gap-2">
                      <span
                        className={cn(
                          "text-[16px] font-semibold truncate",
                          isSelected
                            ? "text-[var(--color-brand-primary)]"
                            : "text-slate-800",
                        )}
                      >
                        {patient.name}
                      </span>
                      <span className="text-[12px] text-slate-400 shrink-0">
                        {patient.room}
                      </span>
                    </div>
                    <div className="mt-1.5">
                      <span className="text-[13px] text-slate-500 truncate block">
                        {patient.mainSymptom}
                      </span>
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </aside>

        {/* Right Content Area */}
        <div className="flex-1 min-h-0 p-8 overflow-hidden">
          <div className="max-w-6xl mx-auto h-full flex flex-col">
            <div
              ref={scrollContainerRef}
              className="flex-1 min-h-0 overflow-y-auto pr-2 custom-scrollbar space-y-6 pb-10"
            >
              {/* [TOP] 교대 전체 통합 요약 */}
              <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
                <div className="px-6 py-4 border-b border-border-base flex items-center gap-2">
                  <span className="flex items-center gap-1.5 font-bold text-[var(--color-brand-primary)] bg-[var(--color-brand-surface)] px-3 py-1 rounded-full text-[12px]">
                    <Sparkles className="size-3.5" />
                    교대 통합 요약
                  </span>
                  <span className="font-mono font-semibold text-[var(--color-content-muted)] text-[12px]">
                    {HANDOVER_SHIFT_OVERVIEW.generatedAt}
                  </span>
                  {HANDOVER_SHIFT_OVERVIEW.model && (
                    <span className="text-[var(--color-content-tertiary)] text-[12px]">
                      · {HANDOVER_SHIFT_OVERVIEW.model}
                    </span>
                  )}
                </div>
                <div className="px-6 py-5">
                  <Text className="text-[15px] leading-relaxed text-[var(--color-content-primary)] whitespace-pre-wrap">
                    {HANDOVER_SHIFT_OVERVIEW.summary}
                  </Text>
                </div>
              </div>

              {/* [BOTTOM] 담당 환자별 카드 */}
              {filteredPatients.length === 0 ? (
                <div className="bg-white rounded-2xl shadow-sm px-8 py-16 flex flex-col items-center gap-3 text-[var(--color-content-muted)]">
                  <Sparkles className="size-6 opacity-50" />
                  <Text className="text-[14px] font-medium">
                    담당 환자가 없습니다
                  </Text>
                  <Text className="text-[12px] text-[var(--color-content-tertiary)]">
                    대시보드에서 담당 환자를 설정하면 여기에 표시됩니다.
                  </Text>
                </div>
              ) : (
                filteredPatients.map((patient) => {
                  const summary = findSummary(patient.id);
                  const isSourceOpen = expandedSources.has(patient.id);
                  const sourceRecords = summary?.sourceRecordIds
                    ? getRecordsForPatient(patient.id).filter((record) =>
                        summary.sourceRecordIds!.includes(record.id),
                      )
                    : [];
                  return (
                    <div
                      key={patient.id}
                      ref={(el) => {
                        cardRefs.current[patient.id] = el;
                      }}
                      data-patient-id={patient.id}
                      className={cn(
                        "bg-white rounded-2xl overflow-hidden flex flex-col transition-all scroll-mt-4",
                        selectedPatientId === patient.id
                          ? "shadow-xl"
                          : "shadow-sm hover:shadow-md",
                      )}
                    >
                      {/* [CARD HEADER] */}
                      <div className="px-5 py-3.5 bg-slate-50/60 border-b border-border-base flex items-center gap-4">
                        <div className="flex items-baseline gap-2.5">
                          <Heading
                            level="h3"
                            className="text-2xl font-bold text-slate-800"
                          >
                            {patient.name}
                          </Heading>
                          <Text className="text-[15px] text-slate-500 font-medium">
                            {patient.birthDate}
                          </Text>
                        </div>
                        <div className="w-px h-5 bg-border-base" />
                        <Badge className="bg-slate-100 text-slate-600 font-medium border-none text-[13px] px-2.5 py-0.5 hover:bg-slate-100">
                          {patient.room}
                        </Badge>
                        <Badge
                          className="ml-auto bg-[var(--color-brand-surface)] text-[var(--color-brand-primary)] font-semibold border-none text-[13px] px-3 py-1 hover:bg-[var(--color-brand-surface)]"
                          title="주요 증상"
                        >
                          {patient.mainSymptom}
                        </Badge>
                      </div>

                      {/* [AI SUMMARY SECTION] */}
                      {!summary ? (
                        <div className="p-8 flex flex-col items-center justify-center gap-2 text-[var(--color-content-muted)]">
                          <Sparkles className="size-6 opacity-50" />
                          <Text className="text-[14px] font-medium">
                            요약 준비 중
                          </Text>
                          <Text className="text-[12px] text-[var(--color-content-tertiary)]">
                            해당 환자의 AI 인수인계 요약이 아직 생성되지
                            않았어요.
                          </Text>
                        </div>
                      ) : (
                        <div className="p-6 flex flex-col gap-5">
                          {/* Meta bar */}
                          <div className="flex items-center gap-2 text-[12px]">
                            <span className="flex items-center gap-1 font-bold text-[var(--color-brand-primary)] bg-[var(--color-brand-surface)] px-2.5 py-1 rounded-full">
                              <Sparkles className="size-3.5" />
                              AI 요약
                            </span>
                            <span className="font-mono font-semibold text-[var(--color-content-muted)]">
                              {summary.generatedAt}
                            </span>
                            {summary.model && (
                              <span className="text-[var(--color-content-tertiary)]">
                                · {summary.model}
                              </span>
                            )}
                          </div>

                          {/* Headline */}
                          <Text className="text-[18px] leading-relaxed font-semibold text-[var(--color-content-primary)] whitespace-pre-wrap">
                            {summary.headline}
                          </Text>

                          {/* Watch Points — 받는 간호사 액션 아이템 (headline 바로 아래) */}
                          {summary.watchPoints.length > 0 && (
                            <div className="space-y-2.5 p-4 rounded-xl bg-amber-50/60 border border-amber-200">
                              <div className="flex items-center gap-2">
                                <AlertTriangle className="size-4 text-amber-600" />
                                <Heading
                                  level="h4"
                                  className="text-[15px] font-bold text-amber-900"
                                >
                                  다음 교대 확인사항
                                </Heading>
                              </div>
                              <ul className="space-y-1.5 pl-1">
                                {summary.watchPoints.map((point, i) => (
                                  <li
                                    key={i}
                                    className="flex gap-2.5 text-[14px] leading-relaxed text-amber-900"
                                  >
                                    <span className="mt-2 size-1.5 rounded-full bg-amber-500 shrink-0" />
                                    <span className="flex-1 min-w-0">
                                      {point}
                                    </span>
                                  </li>
                                ))}
                              </ul>
                            </div>
                          )}

                          {/* Vitals Note */}
                          {summary.vitalsNote && (
                            <div
                              className={cn(
                                "flex items-start gap-2.5 p-3 rounded-xl border",
                                patient.recentVitals.status === "abnormal"
                                  ? "bg-rose-50/60 border-rose-200"
                                  : "bg-emerald-50/60 border-emerald-200",
                              )}
                            >
                              <Activity
                                className={cn(
                                  "size-4 mt-0.5 shrink-0",
                                  patient.recentVitals.status === "abnormal"
                                    ? "text-rose-500"
                                    : "text-emerald-600",
                                )}
                              />
                              <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2 mb-0.5">
                                  <span className="text-[12px] font-bold uppercase tracking-wider text-[var(--color-content-tertiary)]">
                                    바이탈 트렌드
                                  </span>
                                  <Badge
                                    className={cn(
                                      "border-none text-[11px] px-2 py-0 font-semibold",
                                      patient.recentVitals.status === "abnormal"
                                        ? "bg-rose-100 text-rose-700 hover:bg-rose-100"
                                        : "bg-emerald-100 text-emerald-700 hover:bg-emerald-100",
                                    )}
                                  >
                                    {patient.recentVitals.status === "abnormal"
                                      ? "주의"
                                      : "안정"}
                                  </Badge>
                                </div>
                                <Text className="text-[14px] text-[var(--color-content-secondary)]">
                                  {summary.vitalsNote}
                                </Text>
                              </div>
                            </div>
                          )}

                          {/* Key Issues */}
                          {summary.keyIssues.length > 0 && (
                            <div className="space-y-2.5">
                              <div className="flex items-center gap-2">
                                <TrendingUp className="size-4 text-[var(--color-brand-primary)]" />
                                <Heading
                                  level="h4"
                                  className="text-[15px] font-bold text-[var(--color-content-primary)]"
                                >
                                  주요 경과
                                </Heading>
                              </div>
                              <ul className="space-y-1.5 pl-1">
                                {summary.keyIssues.map((issue, i) => (
                                  <li
                                    key={i}
                                    className="flex gap-2.5 text-[14px] leading-relaxed text-[var(--color-content-secondary)]"
                                  >
                                    <span className="mt-2 size-1.5 rounded-full bg-[var(--color-brand-primary)]/60 shrink-0" />
                                    <span className="flex-1 min-w-0">
                                      {issue}
                                    </span>
                                  </li>
                                ))}
                              </ul>
                            </div>
                          )}

                          {/* Source Records Toggle */}
                          {sourceRecords.length > 0 && (
                            <div className="border-t border-border-subtle pt-4">
                              <button
                                type="button"
                                onClick={() => toggleSource(patient.id)}
                                className="flex items-center gap-2 text-[13px] font-semibold text-[var(--color-content-tertiary)] hover:text-[var(--color-content-primary)] transition-colors"
                              >
                                {isSourceOpen ? (
                                  <ChevronDown className="size-4" />
                                ) : (
                                  <ChevronRight className="size-4" />
                                )}
                                <FileText className="size-3.5" />
                                근거 기록 {sourceRecords.length}건{" "}
                                {isSourceOpen ? "접기" : "보기"}
                              </button>
                              {isSourceOpen && (
                                <ul className="mt-3 space-y-2">
                                  {sourceRecords.map((record) => (
                                    <li
                                      key={record.id}
                                      className="flex gap-2.5 p-3 rounded-xl border bg-[var(--color-surface-card)] border-[var(--color-border-subtle)]"
                                    >
                                      <div className="flex-1 min-w-0">
                                        <div className="flex items-center gap-2 mb-0.5">
                                          <span className="text-[12px] font-mono font-semibold text-[var(--color-content-muted)]">
                                            {record.time}
                                          </span>
                                          <Badge className="bg-slate-100 text-slate-600 font-medium border-none text-[11px] px-2 py-0 hover:bg-slate-100">
                                            {record.category}
                                          </Badge>
                                        </div>
                                        <Text className="text-[13px] leading-relaxed text-[var(--color-content-secondary)] whitespace-pre-wrap">
                                          {record.content}
                                        </Text>
                                      </div>
                                    </li>
                                  ))}
                                </ul>
                              )}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
