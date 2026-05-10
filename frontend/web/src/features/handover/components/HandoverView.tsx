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
import { Text } from "@/components/ui/text";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
  ok: "bg-status-success-surface text-status-success",
  partial: "bg-status-warning-surface text-status-warning-strong",
  failed: "bg-status-danger-surface text-status-danger-strong",
};

const VERIFICATION_LABEL: Record<VerificationStatus, string> = {
  ok: "검증",
  partial: "부분",
  failed: "실패",
};

const SEVERITY_TONE: Record<string, string> = {
  stable: "bg-status-success-surface text-status-success",
  watcher: "bg-status-warning-surface text-status-warning-strong",
  unstable: "bg-status-danger-surface text-status-danger-strong",
};

// 화면의 base 행 — 담당 환자(wardPatient)는 항상 존재, 인수인계 리포트(roster)는 옵션.
// roster-summary 가 빈 응답이어도 환자 카드는 사라지지 않도록 wardPatient 를 1차 키로 둔다.
type HandoverRow = {
  wardPatient: WardPatient;
  roster: RosterPatientItem | null;
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

  // encounterId → RosterPatientItem 매핑 (인수인계 리포트가 있는 환자만 키 존재).
  const rosterByEncounterId = useMemo(() => {
    const map = new Map<number, RosterPatientItem>();
    rosterQuery.data?.patients.forEach((item) => {
      map.set(item.encounter_id, item);
    });
    return map;
  }, [rosterQuery.data]);

  // 화면의 base 는 항상 내 담당 환자(wardPatients.isMyPatient).
  // roster-summary 가 빈 응답이어도 담당 환자 카드는 사라지지 않도록.
  const myPatients = useMemo<WardPatient[]>(
    () => wardPatients?.filter((patient) => patient.isMyPatient) ?? [],
    [wardPatients],
  );

  const filteredRows = useMemo<HandoverRow[]>(() => {
    const rows: HandoverRow[] = myPatients.map((wardPatient) => ({
      wardPatient,
      roster: rosterByEncounterId.get(wardPatient.encounterId) ?? null,
    }));
    const query = searchQuery.trim();
    if (!query) return rows;
    return rows.filter(({ wardPatient, roster }) => {
      return (
        wardPatient.name.includes(query) ||
        wardPatient.roomName.includes(query) ||
        (roster?.header.includes(query) ?? false)
      );
    });
  }, [myPatients, rosterByEncounterId, searchQuery]);

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
    wardPatient: WardPatient,
  ) => {
    const params = new URLSearchParams({
      patientId: String(wardPatient.patientId),
      focusRecordId: citation.record_id,
      date: citation.ts.slice(0, 10),
    });
    router.push(`/dashboard?${params.toString()}`);
  };

  return (
    <div className="flex flex-col h-screen bg-surface-base font-sans">
      {/* Top Header */}
      <header className="h-16 flex items-center justify-between px-6 bg-surface-card border-b border-border-base shrink-0 z-50 shadow-sm">
        <div className="flex items-center gap-4">
          <button
            onClick={() => router.push("/dashboard")}
            className="p-2 rounded-full hover:bg-surface-hover transition-all text-content-muted"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h2 className="text-title-lg font-bold text-content-primary leading-tight tracking-tight">
            AI 인수인계 리포트
          </h2>
        </div>

        <div className="flex items-center gap-3">
          <div className="relative w-80">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted" />
            <Input
              placeholder="환자명, 병실, 헤더 검색..."
              className="pl-9 bg-surface-hover border-border-base h-10 text-body-sm focus-visible:ring-1 focus-visible:ring-brand-primary"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
          </div>
          <Button
            type="button"
            variant="brand"
            size="lg"
            onClick={handleGenerate}
            disabled={generateMutation.isPending || (activeJobId !== null && !progress.done)}
          >
            <Sparkles className="size-4" />
            리포트 생성
          </Button>
        </div>
      </header>

      {/* 진행 토스트 */}
      {activeJobId !== null && (
        <ProgressBanner
          progress={progress}
          totalEstimated={Math.max(
            progress.startedCount,
            myPatients.length,
          )}
          onDismiss={() => setActiveJobId(null)}
        />
      )}

      <main className="flex-1 min-h-0 flex overflow-hidden">
        {/* Left Sidebar — 담당 환자 목록 */}
        <aside className="w-72 shrink-0 bg-surface-card border-r border-border-base flex flex-col">
          <div className="px-5 py-4 border-b border-border-base shrink-0">
            <div className="flex items-center justify-between">
              <span className="text-[15px] font-bold text-content-primary leading-none">
                담당 환자
              </span>
              <span className="text-body-xs font-semibold text-brand-primary bg-brand-surface px-2.5 py-0.5 rounded-full leading-none">
                {filteredRows.length}명
              </span>
            </div>
            <p className="mt-1.5 text-body-micro text-content-muted">
              클릭하면 해당 환자 카드로 이동해요
            </p>
          </div>
          <div className="flex-1 min-h-0 overflow-y-auto p-3 space-y-2">
            {filteredRows.length === 0 ? (
              <div className="text-center py-8 text-body-xs text-content-muted">
                {myPatients.length === 0
                  ? "담당 환자가 없습니다. 대시보드에서 담당 환자를 지정하세요."
                  : "검색 결과 없음"}
              </div>
            ) : (
              filteredRows.map(({ wardPatient, roster }) => {
                const fresh =
                  roster?.freshness?.new_records_since_report ?? 0;
                return (
                  <button
                    key={wardPatient.encounterId}
                    onClick={() => handleSelectPatient(wardPatient.encounterId)}
                    className="w-full text-left px-4 py-3 rounded-xl bg-transparent hover:bg-surface-hover transition-all group"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-body-base font-semibold truncate text-content-primary">
                        {wardPatient.name}
                      </span>
                      <span className="text-body-micro text-content-muted shrink-0">
                        {wardPatient.roomName} {wardPatient.bedName}
                      </span>
                    </div>
                    <div className="mt-1.5 flex items-center gap-1.5">
                      <span
                        className={cn(
                          "text-body-xs truncate flex-1",
                          roster ? "text-content-tertiary" : "text-content-muted italic",
                        )}
                      >
                        {roster?.header ??
                          wardPatient.chiefComplaint ??
                          "리포트 미생성"}
                      </span>
                      {fresh > 0 && (
                        <span
                          title="리포트 이후 신규 간호기록"
                          className="px-1.5 py-0.5 rounded-full bg-status-warning-surface text-status-warning-strong text-[10px] font-bold leading-none shrink-0"
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
                <div className="bg-surface-card rounded-2xl shadow-sm overflow-hidden">
                  <div className="px-6 py-4 border-b border-border-base flex items-center gap-2">
                    <span className="flex items-center gap-1.5 font-bold text-brand-primary bg-brand-surface px-3 py-1 rounded-full text-body-micro leading-none">
                      <Sparkles className="size-3.5" />
                      교대 통합 요약
                    </span>
                    <span className="text-content-tertiary text-body-micro">
                      담당 환자 {myPatients.length}명
                    </span>
                  </div>
                  <div className="px-6 py-5">
                    <Text className="text-[15px] leading-relaxed text-content-primary whitespace-pre-wrap">
                      {rosterQuery.data.narrative_header}
                    </Text>
                  </div>
                </div>
              )}

              {/* [BOTTOM] 환자별 카드 */}
              {filteredRows.length === 0 ? (
                <div className="bg-surface-card rounded-2xl shadow-sm px-8 py-16 flex flex-col items-center gap-3 text-content-muted">
                  <Sparkles className="size-6 opacity-50" />
                  <Text className="text-body-sm font-medium">
                    {myPatients.length === 0
                      ? "담당 환자가 없습니다"
                      : "검색 결과 없음"}
                  </Text>
                </div>
              ) : (
                filteredRows.map(({ wardPatient, roster }) => (
                  <PatientHandoverCard
                    key={wardPatient.encounterId}
                    wardPatient={wardPatient}
                    roster={roster}
                    isSelected={
                      roster !== null &&
                      selectedHandoverId === String(roster.handover_id)
                    }
                    onCardRef={(element) => {
                      cardRefs.current[wardPatient.encounterId] = element;
                    }}
                    onToggleDetail={() => {
                      if (!roster) return;
                      setSelectedHandoverId(
                        selectedHandoverId === String(roster.handover_id)
                          ? null
                          : String(roster.handover_id),
                      );
                    }}
                    onCitationClick={(citation) =>
                      handleCitationClick(citation, wardPatient)
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
        "flex items-center gap-3 px-6 py-3 border-b text-body-xs",
        progress.done
          ? "bg-status-success-surface border-status-success/30 text-status-success"
          : progress.connectionError
            ? "bg-status-danger-surface border-status-danger/30 text-status-danger-strong"
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
        <span className="text-status-danger-strong font-semibold">
          · 실패 {progress.errorCount}건
        </span>
      )}
      <button
        type="button"
        onClick={onDismiss}
        className="ml-auto text-body-micro font-semibold opacity-70 hover:opacity-100"
      >
        닫기
      </button>
    </div>
  );
}

function PatientHandoverCard({
  wardPatient,
  roster,
  isSelected,
  onCardRef,
  onToggleDetail,
  onCitationClick,
}: {
  wardPatient: WardPatient;
  roster: RosterPatientItem | null;
  isSelected: boolean;
  onCardRef: (element: HTMLDivElement | null) => void;
  onToggleDetail: () => void;
  onCitationClick: (citation: Citation) => void;
}) {
  const detailQuery = useHandoverDetail(
    isSelected && roster ? String(roster.handover_id) : null,
  );

  return (
    <div
      ref={onCardRef}
      data-encounter-id={wardPatient.encounterId}
      className={cn(
        "bg-surface-card rounded-2xl overflow-hidden flex flex-col transition-all scroll-mt-4",
        isSelected ? "shadow-xl" : "shadow-sm hover:shadow-md",
      )}
    >
      {/* [CARD HEADER] */}
      <div className="px-5 py-3.5 bg-surface-base/70 border-b border-border-base flex items-center gap-3">
        <div className="flex items-baseline gap-2.5 min-w-0">
          <h3 className="text-title-lg font-bold text-content-primary truncate leading-tight tracking-tight">
            {wardPatient.name}
          </h3>
          <Text className="text-body-xs text-content-tertiary font-medium shrink-0">
            {wardPatient.roomName} {wardPatient.bedName}
          </Text>
        </div>
        {roster && (
          <Badge
            className="ml-auto bg-[#F7F8FA] text-content-secondary font-bold border-none text-[11px] px-2.5 py-1 hover:bg-[#F7F8FA] shrink-0"
            title="risk_score"
          >
            위험도 {roster.risk_score}
          </Badge>
        )}
        {roster && (
          <button
            type="button"
            onClick={onToggleDetail}
            className="flex items-center gap-1 text-body-micro font-semibold text-content-tertiary hover:text-content-primary leading-none"
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
        )}
      </div>

      {roster ? (
        <>
          {/* [HEADER LINE] */}
          <div className="px-6 pt-5">
            <Text className="text-[18px] leading-relaxed font-semibold text-content-primary whitespace-pre-wrap">
              {roster.header}
            </Text>
          </div>

          {/* [RULES BRIEF] — 항상 보임 */}
          {roster.rules_fired_brief.length > 0 && (
            <div className="px-6 pt-4">
              <div className="flex items-center gap-2 mb-2.5">
                <AlertTriangle className="size-4 text-status-warning" />
                <h4 className="text-body-sm font-bold text-status-warning-strong">
                  주의 규칙
                </h4>
              </div>
              <ul className="space-y-1.5 pl-1">
                {roster.rules_fired_brief.map((rule, index) => (
                  <li
                    key={index}
                    className="flex gap-2.5 text-body-sm leading-relaxed text-status-warning-strong"
                  >
                    <span className="mt-2 size-1.5 rounded-full bg-status-warning shrink-0" />
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
                <div className="text-center py-8 text-body-xs text-status-danger">
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
        </>
      ) : (
        <div className="px-6 py-8 flex flex-col items-center gap-2 text-center">
          <Sparkles className="size-5 text-content-muted opacity-50" />
          <Text className="text-body-sm font-semibold text-content-tertiary">
            아직 인수인계 리포트가 없어요
          </Text>
          <Text className="text-body-micro text-content-muted">
            우측 상단 &quot;리포트 생성&quot; 버튼으로 만들 수 있어요
          </Text>
          {wardPatient.chiefComplaint && (
            <Text className="mt-1 text-body-xs text-content-secondary">
              주 증상 · {wardPatient.chiefComplaint}
            </Text>
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
      <div className="flex items-center gap-2 text-body-micro">
        <span className="flex items-center gap-1 font-bold text-brand-primary bg-brand-surface px-2.5 py-1 rounded-full leading-none">
          <Sparkles className="size-3.5" />
          PASS-BAR
        </span>
        <span
          className={cn(
            "px-2 py-0.5 rounded-full font-bold uppercase tracking-wider text-[11px] leading-none",
            SEVERITY_TONE[payload.illness_severity] ??
              "bg-surface-hover text-content-secondary",
          )}
        >
          {payload.illness_severity}
        </span>
        <span className="font-mono text-content-muted">
          {createdAt.slice(0, 16).replace("T", " ")}
        </span>
        <span className="text-content-tertiary">
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
    <div className="rounded-xl border border-border-subtle bg-surface-base/70 p-3.5 flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <h4 className="text-body-xs font-bold text-content-primary leading-none">
          {label}
        </h4>
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
        <Text className="text-body-micro text-content-muted">—</Text>
      ) : (
        <ul className="space-y-1.5">
          {slot.items.map((item, index) => (
            <li key={index} className="text-body-xs leading-relaxed">
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
                        className="px-1.5 py-0.5 rounded bg-brand-surface text-brand-primary text-[10px] font-mono font-bold hover:bg-brand-primary hover:text-brand-text transition-colors leading-none"
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
        className="flex items-center gap-2 text-body-xs font-semibold text-content-tertiary hover:text-content-primary transition-colors"
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
                    <span className="text-body-micro font-mono font-semibold text-content-muted">
                      #{citation.record_id}
                    </span>
                    <Badge className="bg-surface-hover text-content-tertiary font-medium border-none text-[11px] px-2 py-0 hover:bg-surface-hover">
                      {citation.label}
                    </Badge>
                    <span className="text-[11px] font-mono text-content-tertiary">
                      {citation.ts.slice(0, 16).replace("T", " ")}
                    </span>
                  </div>
                  {citation.line_range.length > 0 && (
                    <Text className="text-body-micro text-content-tertiary">
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
