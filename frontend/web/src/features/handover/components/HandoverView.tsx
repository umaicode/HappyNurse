"use client";

import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  Search,
  Sparkles,
  AlertTriangle,
  ChevronDown,
  ChevronRight,
  FileText,
  Loader2,
  RefreshCw,
} from "lucide-react";
import { useMemo, useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Heading } from "@/components/ui/heading";
import { Text } from "@/components/ui/text";
import { Badge } from "@/components/ui/badge";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import {
  useGenerateHandover,
  useHandoverDetail,
  useHandoverStream,
  useRosterSummary,
} from "@/features/handover/hooks/useHandover";
import type {
  Citation,
  RosterPatientItem,
  Slot,
  Slots,
  VerificationStatus,
} from "@/features/handover/types/handover";
import type { WardPatient } from "@/features/patient/types/ward-patient";

const SLOT_LABEL: Record<keyof Slots, string> = {
  patient_problem: "환자 문제",
  assessment: "평가",
  situation: "상황",
  safety: "안전",
  background: "배경",
  action: "조치",
  recommendation: "권고",
  synthesis: "종합",
};

const VERIFICATION_TONE: Record<VerificationStatus, string> = {
  ok: "bg-emerald-50 text-emerald-700",
  partial: "bg-amber-50 text-amber-700",
  failed: "bg-rose-50 text-rose-700",
};

const VERIFICATION_LABEL: Record<VerificationStatus, string> = {
  ok: "검증",
  partial: "부분",
  failed: "실패",
};

const SEVERITY_TONE: Record<string, string> = {
  stable: "bg-emerald-100 text-emerald-700",
  watcher: "bg-amber-100 text-amber-700",
  unstable: "bg-rose-100 text-rose-700",
};

export function HandoverView() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedHandoverId, setSelectedHandoverId] = useState<string | null>(
    null,
  );
  const [activeJobId, setActiveJobId] = useState<string | null>(null);

  const rosterQuery = useRosterSummary();
  const { data: wardPatients } = useWardPatients();
  const generateMutation = useGenerateHandover();
  const progress = useHandoverStream(activeJobId);

  // encounterId → WardPatient join (RosterPatientItem 엔 환자명/호실 없음)
  const patientByEncounterId = useMemo(() => {
    const map = new Map<number, WardPatient>();
    wardPatients?.forEach((patient) => {
      map.set(patient.encounterId, patient);
    });
    return map;
  }, [wardPatients]);

  const filteredPatients = useMemo<RosterPatientItem[]>(() => {
    const items = rosterQuery.data?.patients ?? [];
    const query = searchQuery.trim();
    if (!query) return items;
    return items.filter((item) => {
      const wp = patientByEncounterId.get(item.encounter_id);
      const name = wp?.name ?? "";
      const room = wp?.roomName ?? "";
      return (
        name.includes(query) ||
        room.includes(query) ||
        item.header.includes(query)
      );
    });
  }, [rosterQuery.data, patientByEncounterId, searchQuery]);

  // 카드별 ref — 좌측 리스트 클릭 시 스크롤
  const cardRefs = useRef<Record<number, HTMLDivElement | null>>({});

  const handleSelectPatient = (encounterId: number) => {
    cardRefs.current[encounterId]?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  };

  const handleGenerate = () => {
    generateMutation.mutate(undefined, {
      onSuccess: ({ jobId }) => {
        setActiveJobId(jobId);
      },
    });
  };

  // Citation 클릭 시 dashboard 로 — patientId + focusRecordId + selectedDate(citation 의 ts 일자)
  // citation 의 ts 가 어제 날짜면 NursingTab 이 그 일자 데이터를 fetch 해야 focus 가능.
  const handleCitationClick = (
    citation: Citation,
    encounterId: number,
  ) => {
    const patient = patientByEncounterId.get(encounterId);
    if (!patient) return;
    const params = new URLSearchParams({
      patientId: String(patient.patientId),
      focusRecordId: citation.record_id,
      date: citation.ts.slice(0, 10),
    });
    router.push(`/dashboard?${params.toString()}`);
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
            {rosterQuery.data?.meta && (
              <span className="text-body-micro text-content-tertiary">
                schema {rosterQuery.data.schema_version}
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted" />
            <Input
              placeholder="환자명, 병실, 헤더 검색..."
              className="pl-9 bg-slate-50 border-border-base h-10 text-[14px] focus-visible:ring-1 focus-visible:ring-[var(--color-brand-primary)]"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
          </div>
          <button
            type="button"
            onClick={handleGenerate}
            disabled={generateMutation.isPending || (activeJobId !== null && !progress.done)}
            className={cn(
              "flex items-center gap-1.5 h-10 px-4 rounded-md bg-brand-primary text-white text-[14px] font-bold shadow-sm transition-colors hover:bg-brand-hover disabled:opacity-60 disabled:cursor-not-allowed",
            )}
          >
            <Sparkles className="size-4" />
            리포트 생성
          </button>
        </div>
      </header>

      {/* 진행 토스트 */}
      {activeJobId !== null && (
        <ProgressBanner
          progress={progress}
          totalEstimated={Math.max(
            progress.startedCount,
            rosterQuery.data?.patients.length ?? 0,
          )}
          onDismiss={() => setActiveJobId(null)}
        />
      )}

      <main className="flex-1 min-h-0 flex overflow-hidden">
        {/* Left Sidebar — 담당 환자 목록 */}
        <aside className="w-72 shrink-0 bg-white border-r border-border-base flex flex-col">
          <div className="px-5 py-4 border-b border-border-base shrink-0">
            <div className="flex items-center justify-between">
              <span className="text-[15px] font-bold text-slate-800">
                담당 환자
              </span>
              <span className="text-[13px] font-semibold text-[var(--color-brand-primary)] bg-[var(--color-brand-primary)]/10 px-2.5 py-0.5 rounded-full">
                {filteredPatients.length}명
              </span>
            </div>
            <p className="mt-1.5 text-[12px] text-slate-400">
              클릭하면 해당 환자 카드로 이동해요
            </p>
          </div>
          <div className="flex-1 min-h-0 overflow-y-auto p-3 space-y-2">
            {rosterQuery.isPending ? (
              <div className="flex flex-col items-center gap-2 py-10 text-content-muted">
                <Loader2 className="size-5 animate-spin" />
                <p className="text-body-micro">불러오는 중...</p>
              </div>
            ) : rosterQuery.isError ? (
              <div className="text-center py-8 text-[13px] text-status-danger">
                요약을 불러오지 못했습니다
              </div>
            ) : filteredPatients.length === 0 ? (
              <div className="text-center py-8 text-[13px] text-slate-400">
                {(rosterQuery.data?.patients.length ?? 0) === 0
                  ? "저장된 인수인계 리포트가 없습니다. 우측 상단 \"리포트 생성\" 으로 시작하세요."
                  : "검색 결과 없음"}
              </div>
            ) : (
              filteredPatients.map((patient) => {
                const wp = patientByEncounterId.get(patient.encounter_id);
                const fresh =
                  patient.freshness?.new_records_since_report ?? 0;
                return (
                  <button
                    key={patient.encounter_id}
                    onClick={() => handleSelectPatient(patient.encounter_id)}
                    className="w-full text-left px-4 py-3 rounded-xl bg-transparent hover:bg-[var(--color-surface-hover)] transition-all group"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-[16px] font-semibold truncate text-slate-800">
                        {wp?.name ?? `환자 #${patient.encounter_id}`}
                      </span>
                      <span className="text-[12px] text-slate-400 shrink-0">
                        {wp ? `${wp.roomName} ${wp.bedName}` : ""}
                      </span>
                    </div>
                    <div className="mt-1.5 flex items-center gap-1.5">
                      <span className="text-[13px] text-slate-500 truncate flex-1">
                        {patient.header}
                      </span>
                      {fresh > 0 && (
                        <span
                          title="리포트 이후 신규 간호기록"
                          className="px-1.5 py-0.5 rounded-full bg-amber-50 text-amber-700 text-[10px] font-bold leading-none shrink-0"
                        >
                          새 {fresh}
                        </span>
                      )}
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
            <div className="flex-1 min-h-0 overflow-y-auto pr-2 custom-scrollbar space-y-6 pb-10">
              {/* [TOP] 교대 통합 narrative_header */}
              {rosterQuery.data && rosterQuery.data.narrative_header && (
                <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
                  <div className="px-6 py-4 border-b border-border-base flex items-center gap-2">
                    <span className="flex items-center gap-1.5 font-bold text-[var(--color-brand-primary)] bg-[var(--color-brand-surface)] px-3 py-1 rounded-full text-[12px]">
                      <Sparkles className="size-3.5" />
                      교대 통합 요약
                    </span>
                    <span className="text-[var(--color-content-tertiary)] text-[12px]">
                      담당 환자 {rosterQuery.data.patients.length}명
                    </span>
                  </div>
                  <div className="px-6 py-5">
                    <Text className="text-[15px] leading-relaxed text-[var(--color-content-primary)] whitespace-pre-wrap">
                      {rosterQuery.data.narrative_header}
                    </Text>
                  </div>
                </div>
              )}

              {/* [BOTTOM] 환자별 카드 */}
              {filteredPatients.length === 0 ? (
                <div className="bg-white rounded-2xl shadow-sm px-8 py-16 flex flex-col items-center gap-3 text-[var(--color-content-muted)]">
                  <Sparkles className="size-6 opacity-50" />
                  <Text className="text-[14px] font-medium">
                    표시할 환자 카드가 없습니다
                  </Text>
                </div>
              ) : (
                filteredPatients.map((patient) => (
                  <PatientHandoverCard
                    key={patient.encounter_id}
                    patient={patient}
                    wardPatient={patientByEncounterId.get(patient.encounter_id)}
                    isSelected={
                      selectedHandoverId === String(patient.handover_id)
                    }
                    onCardRef={(element) => {
                      cardRefs.current[patient.encounter_id] = element;
                    }}
                    onToggleDetail={() => {
                      setSelectedHandoverId(
                        selectedHandoverId === String(patient.handover_id)
                          ? null
                          : String(patient.handover_id),
                      );
                    }}
                    onCitationClick={(citation) =>
                      handleCitationClick(citation, patient.encounter_id)
                    }
                  />
                ))
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

// ---------- Sub Components ----------

function ProgressBanner({
  progress,
  totalEstimated,
  onDismiss,
}: {
  progress: ReturnType<typeof useHandoverStream>;
  totalEstimated: number;
  onDismiss: () => void;
}) {
  const total = Math.max(totalEstimated, progress.startedCount);
  const done = progress.completeCount + progress.errorCount;
  return (
    <div
      className={cn(
        "flex items-center gap-3 px-6 py-3 border-b text-[13px]",
        progress.done
          ? "bg-emerald-50 border-emerald-200 text-emerald-800"
          : progress.connectionError
            ? "bg-rose-50 border-rose-200 text-rose-800"
            : "bg-brand-surface border-brand-primary/30 text-brand-primary",
      )}
    >
      {progress.done ? (
        <Sparkles className="size-4" />
      ) : progress.connectionError ? (
        <AlertTriangle className="size-4" />
      ) : (
        <Loader2 className="size-4 animate-spin" />
      )}
      <span className="font-bold">
        {progress.done
          ? "리포트 생성 완료"
          : progress.connectionError
            ? "진행 추적 끊김 (리포트는 계속 생성되고 있을 수 있어요)"
            : `리포트 생성 중 ${done}/${total || "?"}`}
      </span>
      {progress.errorCount > 0 && (
        <span className="text-rose-700 font-semibold">
          · 실패 {progress.errorCount}건
        </span>
      )}
      <button
        type="button"
        onClick={onDismiss}
        className="ml-auto text-[12px] font-semibold opacity-70 hover:opacity-100"
      >
        닫기
      </button>
    </div>
  );
}

function PatientHandoverCard({
  patient,
  wardPatient,
  isSelected,
  onCardRef,
  onToggleDetail,
  onCitationClick,
}: {
  patient: RosterPatientItem;
  wardPatient: WardPatient | undefined;
  isSelected: boolean;
  onCardRef: (element: HTMLDivElement | null) => void;
  onToggleDetail: () => void;
  onCitationClick: (citation: Citation) => void;
}) {
  const detailQuery = useHandoverDetail(
    isSelected ? String(patient.handover_id) : null,
  );

  return (
    <div
      ref={onCardRef}
      data-handover-id={patient.handover_id}
      className={cn(
        "bg-white rounded-2xl overflow-hidden flex flex-col transition-all scroll-mt-4",
        isSelected ? "shadow-xl" : "shadow-sm hover:shadow-md",
      )}
    >
      {/* [CARD HEADER] */}
      <div className="px-5 py-3.5 bg-slate-50/60 border-b border-border-base flex items-center gap-3">
        <div className="flex items-baseline gap-2.5 min-w-0">
          <Heading
            level="h3"
            className="text-2xl font-bold text-slate-800 truncate"
          >
            {wardPatient?.name ?? `환자 #${patient.encounter_id}`}
          </Heading>
          {wardPatient && (
            <Text className="text-[13px] text-slate-500 font-medium shrink-0">
              {wardPatient.roomName} {wardPatient.bedName}
            </Text>
          )}
        </div>
        <Badge
          className="ml-auto bg-[#F7F8FA] text-content-secondary font-bold border-none text-[11px] px-2.5 py-1 hover:bg-[#F7F8FA] shrink-0"
          title="risk_score"
        >
          위험도 {patient.risk_score}
        </Badge>
        <button
          type="button"
          onClick={onToggleDetail}
          className="flex items-center gap-1 text-[12px] font-semibold text-content-tertiary hover:text-content-primary"
        >
          {isSelected ? (
            <>
              <ChevronDown className="size-4" /> 접기
            </>
          ) : (
            <>
              <ChevronRight className="size-4" /> 상세
            </>
          )}
        </button>
      </div>

      {/* [HEADER LINE] */}
      <div className="px-6 pt-5">
        <Text className="text-[18px] leading-relaxed font-semibold text-[var(--color-content-primary)] whitespace-pre-wrap">
          {patient.header}
        </Text>
      </div>

      {/* [RULES BRIEF] — 항상 보임 */}
      {patient.rules_fired_brief.length > 0 && (
        <div className="px-6 pt-4">
          <div className="flex items-center gap-2 mb-2.5">
            <AlertTriangle className="size-4 text-amber-600" />
            <Heading
              level="h4"
              className="text-[14px] font-bold text-amber-900"
            >
              주의 규칙
            </Heading>
          </div>
          <ul className="space-y-1.5 pl-1">
            {patient.rules_fired_brief.map((rule, index) => (
              <li
                key={index}
                className="flex gap-2.5 text-[14px] leading-relaxed text-amber-900"
              >
                <span className="mt-2 size-1.5 rounded-full bg-amber-500 shrink-0" />
                <span className="flex-1 min-w-0">{rule}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* [DETAIL — PASS-BAR] */}
      {isSelected && (
        <div className="px-6 pt-5 pb-6 space-y-5">
          {detailQuery.isPending ? (
            <div className="flex flex-col items-center gap-2 py-10 text-content-muted">
              <Loader2 className="size-5 animate-spin" />
              <p className="text-body-micro">PASS-BAR 상세 조회 중...</p>
            </div>
          ) : detailQuery.isError || !detailQuery.data?.autoSummaryJson ? (
            <div className="text-center py-8 text-[13px] text-status-danger">
              상세 리포트를 불러오지 못했습니다
            </div>
          ) : (
            <PassBarDetail
              payload={detailQuery.data.autoSummaryJson}
              createdAt={detailQuery.data.createdAt}
              onCitationClick={onCitationClick}
            />
          )}
        </div>
      )}
    </div>
  );
}

function PassBarDetail({
  payload,
  createdAt,
  onCitationClick,
}: {
  // payload 는 호출 측에서 null 체크 후 전달 (HandoverPayload 보장).
  payload: import("@/features/handover/types/handover").HandoverPayload;
  createdAt: string;
  onCitationClick: (citation: Citation) => void;
}) {
  // citation_id → Citation 매핑 (slot 안 citation_ids 를 풀어 표시)
  const citationById = useMemo(() => {
    const map = new Map<string, Citation>();
    payload.citations.forEach((citation) => {
      map.set(citation.id, citation);
    });
    return map;
  }, [payload.citations]);

  return (
    <>
      <div className="flex items-center gap-2 text-[12px]">
        <span className="flex items-center gap-1 font-bold text-[var(--color-brand-primary)] bg-[var(--color-brand-surface)] px-2.5 py-1 rounded-full">
          <Sparkles className="size-3.5" />
          PASS-BAR
        </span>
        <span
          className={cn(
            "px-2 py-0.5 rounded-full font-bold uppercase tracking-wider text-[11px]",
            SEVERITY_TONE[payload.illness_severity] ??
              "bg-slate-100 text-slate-700",
          )}
        >
          {payload.illness_severity}
        </span>
        <span className="font-mono text-[var(--color-content-muted)]">
          {createdAt.slice(0, 16).replace("T", " ")}
        </span>
        <span className="text-[var(--color-content-tertiary)]">
          · {payload.meta.model}
        </span>
      </div>

      {/* PASS-BAR 슬롯 8개 */}
      <div className="grid grid-cols-2 gap-3">
        {(Object.keys(SLOT_LABEL) as Array<keyof Slots>).map((key) => (
          <SlotCard
            key={key}
            label={SLOT_LABEL[key]}
            slot={payload.slots[key]}
            citationById={citationById}
            onCitationClick={onCitationClick}
          />
        ))}
      </div>

      {/* Citations 전체 목록 */}
      {payload.citations.length > 0 && (
        <CitationList
          citations={payload.citations}
          onCitationClick={onCitationClick}
        />
      )}
    </>
  );
}

function SlotCard({
  label,
  slot,
  citationById,
  onCitationClick,
}: {
  label: string;
  slot: Slot;
  citationById: Map<string, Citation>;
  onCitationClick: (citation: Citation) => void;
}) {
  return (
    <div className="rounded-xl border border-border-subtle bg-slate-50/40 p-3.5 flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <Heading
          level="h4"
          className="text-[13px] font-bold text-content-primary"
        >
          {label}
        </Heading>
        <span
          className={cn(
            "ml-auto px-1.5 py-0.5 rounded text-[10px] font-bold leading-none",
            VERIFICATION_TONE[slot.verification],
          )}
        >
          {VERIFICATION_LABEL[slot.verification]}
        </span>
      </div>
      {slot.items.length === 0 ? (
        <Text className="text-[12px] text-content-muted">—</Text>
      ) : (
        <ul className="space-y-1.5">
          {slot.items.map((item, index) => (
            <li key={index} className="text-[13px] leading-relaxed">
              <span className="text-content-primary">
                {item.value ?? item.quote ?? item.kind ?? "(빈 항목)"}
              </span>
              {item.citation_ids.length > 0 && (
                <span className="ml-1.5 inline-flex flex-wrap gap-1">
                  {item.citation_ids.map((cid) => {
                    const citation = citationById.get(cid);
                    if (!citation) return null;
                    return (
                      <button
                        key={cid}
                        type="button"
                        onClick={() => onCitationClick(citation)}
                        title={`${citation.label} (${citation.ts.slice(0, 16).replace("T", " ")})`}
                        className="px-1.5 py-0.5 rounded bg-brand-surface text-brand-primary text-[10px] font-mono font-bold hover:bg-brand-primary hover:text-white transition-colors"
                      >
                        #{citation.record_id}
                      </button>
                    );
                  })}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function CitationList({
  citations,
  onCitationClick,
}: {
  citations: Citation[];
  onCitationClick: (citation: Citation) => void;
}) {
  const [open, setOpen] = useState(false);
  return (
    <div className="border-t border-border-subtle pt-4">
      <button
        type="button"
        onClick={() => setOpen((previous) => !previous)}
        className="flex items-center gap-2 text-[13px] font-semibold text-content-tertiary hover:text-content-primary transition-colors"
      >
        {open ? (
          <ChevronDown className="size-4" />
        ) : (
          <ChevronRight className="size-4" />
        )}
        <FileText className="size-3.5" />
        근거 기록 {citations.length}건 {open ? "접기" : "보기"}
      </button>
      {open && (
        <ul className="mt-3 space-y-2">
          {citations.map((citation) => (
            <li key={citation.id}>
              <button
                type="button"
                onClick={() => onCitationClick(citation)}
                className="w-full text-left flex gap-2.5 p-3 rounded-xl border bg-surface-card border-border-subtle hover:border-brand-primary/40 transition-colors"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <span className="text-[12px] font-mono font-semibold text-content-muted">
                      #{citation.record_id}
                    </span>
                    <Badge className="bg-slate-100 text-slate-600 font-medium border-none text-[11px] px-2 py-0 hover:bg-slate-100">
                      {citation.label}
                    </Badge>
                    <span className="text-[11px] font-mono text-content-tertiary">
                      {citation.ts.slice(0, 16).replace("T", " ")}
                    </span>
                  </div>
                  {citation.line_range.length > 0 && (
                    <Text className="text-[12px] text-content-tertiary">
                      줄 {citation.line_range.join("-")}
                    </Text>
                  )}
                </div>
                <RefreshCw className="size-3.5 text-content-muted opacity-50 self-center" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
