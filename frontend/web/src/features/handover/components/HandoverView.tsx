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
  Shield,
  ArrowUpRight,
  Clock,
  TrendingUp,
} from "lucide-react";
import { useMemo, useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Text } from "@/components/ui/text";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import {
  useGenerateHandover,
  useHandoverDetail,
  useHandoverStream,
  useRosterSummary,
} from "@/features/handover/hooks/useHandover";
import type {
  Citation,
  HandoverPayload,
  RosterPatientItem,
  Slot,
  SlotItem,
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

// SBAR 흐름에 맞춘 grid 배치 순서 — synthesis/safety 는 상단 콜아웃으로 빠지고 나머지 6 슬롯만.
// Situation→Action(현재 상태), Assessment→Recommendation(평가→권고), Background→Patient Problem(맥락).
const SBAR_SLOT_ORDER: Array<keyof Slots> = [
  "situation",
  "action",
  "assessment",
  "recommendation",
  "background",
  "patient_problem",
];

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

// risk_score 는 휴리스틱 (rules_fired severity 합 + verification 미통과 슬롯 수). raw 숫자만 보면
// nurse 가 의미 파악 어려워 0/4/8 경계로 등급 라벨 + 톤을 함께 표시.
// 산식 출처: ai/nursing_ai/app/services/handover/coordination/roster_summary.py:_risk_score
type RiskTier = "low" | "moderate" | "high";

const riskTier = (score: number): RiskTier => {
  if (score >= 8) return "high";
  if (score >= 4) return "moderate";
  return "low";
};

const RISK_TIER_LABEL: Record<RiskTier, string> = {
  low: "낮음",
  moderate: "보통",
  high: "높음",
};

const RISK_TIER_TONE: Record<RiskTier, string> = {
  low: "bg-status-success-surface text-status-success",
  moderate: "bg-status-warning-surface text-status-warning-strong",
  high: "bg-status-danger-surface text-status-danger-strong",
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
              <span className="text-body-sm font-bold text-content-primary leading-none">
                담당 환자
              </span>
              <span className="text-body-sm font-semibold text-brand-primary bg-brand-surface px-2.5 py-0.5 rounded-full leading-none">
                {filteredRows.length}명
              </span>
            </div>
            <p className="mt-1.5 text-body-micro text-content-muted">
              클릭하면 해당 환자 카드로 이동해요
            </p>
          </div>
          <div className="flex-1 min-h-0 overflow-y-auto p-3 space-y-2">
            {filteredRows.length === 0 ? (
              <div className="text-center py-8 text-body-sm text-content-muted">
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
                          "text-body-sm truncate flex-1",
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
                          className="px-1.5 py-0.5 rounded-full bg-status-warning-surface text-status-warning-strong text-body-micro font-bold leading-none shrink-0"
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
                    <Text className="text-body-sm leading-relaxed text-content-primary whitespace-pre-wrap">
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
        "flex items-center gap-3 px-6 py-3 border-b text-body-sm",
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
        <div className="flex items-center gap-2.5 min-w-0">
          <h3 className="text-title-md font-bold text-content-primary truncate leading-tight tracking-tight">
            {wardPatient.name}
          </h3>
          <Text className="text-body-sm text-content-tertiary font-medium shrink-0">
            {wardPatient.roomName} {wardPatient.bedName}
          </Text>
        </div>
        {roster && (
          <RiskBadge score={roster.risk_score} />
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
          {/* [HEADER LINE] — 환자별 요약 본문. 박스 영역 안에서 상하 가운데 정렬. */}
          <div className="px-6 py-5 min-h-[88px] flex items-center">
            <Text className="text-body-base leading-relaxed font-semibold text-content-primary whitespace-pre-wrap">
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
                <div className="text-center py-8 text-body-sm text-status-danger">
                  상세 리포트를 불러오지 못했습니다
                </div>
              ) : (
                <PassBarDetail
                  payload={detailQuery.data.autoSummaryJson}
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
            <Text className="mt-1 text-body-sm text-content-secondary">
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
  onCitationClick,
}: {
  // payload 는 호출 측에서 null 체크 후 전달 (HandoverPayload 보장).
  payload: HandoverPayload;
  onCitationClick: (citation: Citation) => void;
}) {
  // 체크리스트 — action 슬롯 items 의 value/quote 를 액션 아이템으로 사용.
  // 데이터 소스 (BE checklist 필드 vs 슬롯 재활용 vs 별도 API) 미정 — 일단 UI 만.
  // 체크 상태는 PassBarDetail 인스턴스 로컬 (handover_id 별 카드 마운트되므로 자동 격리, 새로고침 시 초기화).
  const [checkedActionIndices, setCheckedActionIndices] = useState<
    Record<number, boolean>
  >({});
  const toggleAction = (index: number) =>
    setCheckedActionIndices((prev) => ({ ...prev, [index]: !prev[index] }));

  // 한 citation 이 여러 slot 에 인용될 수 있음 — CitationList 에서 어느 슬롯에 인용됐는지 표시.
  const slotKeysByCitationId = useMemo(() => {
    const map = new Map<string, Set<keyof Slots>>();
    (Object.keys(payload.slots) as Array<keyof Slots>).forEach((slotKey) => {
      payload.slots[slotKey].items.forEach((item) => {
        item.citation_ids.forEach((cid) => {
          if (!map.has(cid)) map.set(cid, new Set());
          map.get(cid)!.add(slotKey);
        });
      });
    });
    return map;
  }, [payload.slots]);

  // 체크리스트 항목 소스 — action 우선, 비면 recommendation fallback. 둘 다 비면 빈 메시지 표시 (UI 자체는 항상 노출).
  const checklistItems = useMemo<SlotItem[]>(() => {
    if (payload.slots.action.items.length > 0) return payload.slots.action.items;
    if (payload.slots.recommendation.items.length > 0)
      return payload.slots.recommendation.items;
    return [];
  }, [payload.slots.action.items, payload.slots.recommendation.items]);

  return (
    <>
      {/* [1] Synthesis 콜아웃 — 다음 시프트가 가장 먼저 봐야 할 take-away */}
      {payload.slots.synthesis.items.length > 0 && (
        <SlotCallout
          label="Synthesis · 종합"
          slot={payload.slots.synthesis}
          accent="brand"
        />
      )}

      {/* [2] 체크리스트 — Synthesis 직후 배치 (mockup 의 AI 요약 박스 안 체크리스트 위치).
          action 우선, 비면 recommendation fallback, 둘 다 비면 빈 상태 메시지. */}
      <ChecklistSection
        items={checklistItems}
        checkedByIndex={checkedActionIndices}
        onToggle={toggleAction}
      />

      {/* [3] Safety 콜아웃 — 낙상/격리/DNR/알러지/금기 등 안전 사항 */}
      {payload.slots.safety.items.length > 0 && (
        <SlotCallout
          label="Safety · 안전"
          slot={payload.slots.safety}
          accent="danger"
          icon={<Shield className="size-4" />}
        />
      )}

      {/* [4] SBAR grid — 나머지 6 슬롯 */}
      <div className="grid grid-cols-2 gap-3">
        {SBAR_SLOT_ORDER.map((key) => (
          <SlotCard
            key={key}
            label={SLOT_LABEL[key]}
            slot={payload.slots[key]}
          />
        ))}
      </div>

      {/* [5] Citations 전체 목록 — 인용된 슬롯 라벨과 함께 표시 */}
      {payload.citations.length > 0 && (
        <CitationList
          citations={payload.citations}
          slotKeysByCitationId={slotKeysByCitationId}
          onCitationClick={onCitationClick}
        />
      )}
    </>
  );
}

// 체크리스트 섹션 — mockup(docs/mobile_handover_example.png) 의 AI 요약 박스 안 체크리스트.
// 데이터 소스 미정 (BE checklist 필드 vs slots.action 재활용 vs 별도 API) — 현재는 slots.action.items 의 텍스트를 액션 라벨로 사용.
// 체크 상태는 PassBarDetail 로컬 (handover_id 별 격리, 새로고침 시 초기화).
function ChecklistSection({
  items,
  checkedByIndex,
  onToggle,
}: {
  items: SlotItem[];
  checkedByIndex: Record<number, boolean>;
  onToggle: (index: number) => void;
}) {
  const doneCount = items.filter((_, index) => checkedByIndex[index]).length;
  return (
    <div className="rounded-xl border border-brand-primary/20 bg-brand-surface/20 p-4 flex flex-col gap-2.5">
      <div className="flex items-center gap-2">
        <h4 className="text-body-sm font-bold text-brand-primary leading-none">
          체크리스트
        </h4>
        {items.length > 0 && (
          <span className="text-body-micro text-content-muted leading-none">
            ({doneCount}/{items.length})
          </span>
        )}
      </div>
      {items.length === 0 ? (
        <p className="text-body-sm text-content-muted leading-relaxed">
          등록된 체크 항목이 없습니다.
        </p>
      ) : (
        <ul className="flex flex-col gap-1.5">
          {items.map((item, index) => {
            const label = item.value ?? item.quote ?? item.kind ?? "(빈 항목)";
            const checked = checkedByIndex[index] === true;
            const checkboxId = `handover-checklist-${index}`;
            return (
              <li key={index}>
                <label
                  htmlFor={checkboxId}
                  className="flex items-center gap-2.5 px-2.5 py-2 rounded-md bg-white border border-border-subtle hover:border-brand-primary/30 cursor-pointer select-none transition-colors"
                >
                  <Checkbox
                    id={checkboxId}
                    checked={checked}
                    onCheckedChange={() => onToggle(index)}
                  />
                  <span
                    className={cn(
                      "text-body-sm leading-snug break-words flex-1 min-w-0",
                      checked
                        ? "line-through text-content-muted"
                        : "text-content-primary",
                    )}
                  >
                    {label}
                  </span>
                </label>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

// 위험도 배지 — raw score + tier 라벨, hover 시 산식 안내.
function RiskBadge({ score }: { score: number }) {
  const tier = riskTier(score);
  return (
    <HoverCard openDelay={120} closeDelay={80}>
      <HoverCardTrigger asChild>
        <button
          type="button"
          className={cn(
            "ml-auto inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border-none shrink-0 leading-none font-bold text-body-micro cursor-help",
            RISK_TIER_TONE[tier],
          )}
        >
          <span>위험도 {RISK_TIER_LABEL[tier]}</span>
          <span className="font-mono opacity-70">{score}</span>
        </button>
      </HoverCardTrigger>
      <HoverCardContent align="end" sideOffset={6} className="w-[320px] p-3.5">
        <div className="flex flex-col gap-2 text-body-micro text-content-secondary leading-relaxed">
          <p className="font-bold text-content-primary text-body-sm">위험도 산식</p>
          <p>
            발사된 임상 규칙 severity 가중치 합 + 검증(verification) 통과 못한 슬롯 개수.
          </p>
          <ul className="space-y-0.5 pl-3 list-disc text-content-tertiary">
            <li>high 규칙 1건당 +5, medium +3, low +1</li>
            <li>partial/failed 슬롯 1개당 +1</li>
          </ul>
          <p className="text-content-muted">
            등급 경계 — 낮음 0~3 · 보통 4~7 · 높음 8 이상.
          </p>
        </div>
      </HoverCardContent>
    </HoverCard>
  );
}

// 상단 콜아웃 (Synthesis · Safety) — 풀폭, 강조 톤. SlotCard 와 같은 데이터지만 시각 위계가 다름.
function SlotCallout({
  label,
  slot,
  accent,
  icon,
}: {
  label: string;
  slot: Slot;
  accent: "brand" | "danger";
  icon?: React.ReactNode;
}) {
  const accentClass =
    accent === "danger"
      ? "border-l-4 border-l-status-danger bg-status-danger-surface/30"
      : "border-l-4 border-l-brand-primary bg-brand-surface/20";
  const titleClass =
    accent === "danger" ? "text-status-danger-strong" : "text-brand-primary";

  return (
    <div className={cn("rounded-xl border border-border-subtle p-4 flex flex-col gap-2.5", accentClass)}>
      <div className="flex items-center gap-2">
        {icon}
        <h4 className={cn("text-body-sm font-bold leading-none", titleClass)}>
          {label}
        </h4>
        <span
          className={cn(
            "ml-auto px-1.5 py-0.5 rounded text-body-micro font-bold leading-none",
            VERIFICATION_TONE[slot.verification],
          )}
        >
          {VERIFICATION_LABEL[slot.verification]}
        </span>
      </div>
      <ul className="space-y-2">
        {slot.items.map((item, index) => (
          <SlotItemRow key={index} item={item} />
        ))}
      </ul>
    </div>
  );
}

function SlotCard({ label, slot }: { label: string; slot: Slot }) {
  return (
    <div className="rounded-xl border border-border-subtle bg-surface-base/70 p-3.5 flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <h4 className="text-body-sm font-bold text-content-primary leading-none">
          {label}
        </h4>
        <span
          className={cn(
            "ml-auto px-1.5 py-0.5 rounded text-body-micro font-bold leading-none",
            VERIFICATION_TONE[slot.verification],
          )}
        >
          {VERIFICATION_LABEL[slot.verification]}
        </span>
      </div>
      {slot.items.length === 0 ? (
        <Text className="text-body-micro text-content-muted">—</Text>
      ) : (
        <ul className="space-y-2">
          {slot.items.map((item, index) => (
            <SlotItemRow key={index} item={item} />
          ))}
        </ul>
      )}
    </div>
  );
}

// 슬롯 안 한 줄짜리 항목 — value / quote 본문 + meta (time_window, trend, severity_flag) + contingency.
// citation 표시는 슬롯에서 제거됨 — 출처는 카드 하단 "근거 기록" 영역(CitationList) 에만.
function SlotItemRow({ item }: { item: SlotItem }) {
  const headline = item.value ?? item.quote ?? item.kind ?? "(빈 항목)";
  const severityFlag = item.severity_flag;
  return (
    <li className="text-body-sm leading-relaxed flex flex-col gap-1">
      <div className="flex flex-wrap items-start gap-1.5">
        <span className="text-content-primary flex-1 min-w-0 break-words">
          {headline}
        </span>
        {severityFlag && (
          <span
            className={cn(
              "px-1.5 py-0.5 rounded text-body-micro font-bold leading-none shrink-0",
              SEVERITY_TONE[severityFlag] ?? "bg-surface-hover text-content-secondary",
            )}
          >
            {severityFlag}
          </span>
        )}
      </div>

      {/* meta 라인: time_window · trend — 둘 다 옵션. 없으면 라인 자체 안 그림. */}
      {(item.time_window || item.trend) && (
        <div className="flex items-center gap-2 text-body-micro text-content-tertiary">
          {item.time_window && (
            <span className="inline-flex items-center gap-1">
              <Clock className="size-3" />
              {item.time_window}
            </span>
          )}
          {item.trend && (
            <span className="inline-flex items-center gap-1">
              <TrendingUp className="size-3" />
              {item.trend}
            </span>
          )}
        </div>
      )}

      {/* contingency — "if X then Y" 형식의 조건부 조치. 안전 카드에서 특히 중요. */}
      {item.contingency && (
        <div className="text-body-micro text-status-warning-strong leading-snug pl-2 border-l-2 border-status-warning/40">
          ↳ {item.contingency}
        </div>
      )}

      {/* 슬롯 안 citation chip 은 제거 — 출처는 카드 하단의 "근거 기록" 영역(CitationList) 에만 표시. */}
    </li>
  );
}

// hover preview 공용 — chip / citation list 양쪽에서 사용.
function CitationPreview({
  citation,
  onClick,
}: {
  citation: Citation;
  onClick: () => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <Badge className="bg-brand-surface text-brand-primary border-none text-body-micro font-bold leading-none px-2 py-0.5">
          {citation.label}
        </Badge>
      </div>
      <div className="text-body-micro font-mono text-content-tertiary leading-none">
        {citation.ts.slice(0, 16).replace("T", " ")}
      </div>
      <button
        type="button"
        onClick={onClick}
        className="self-end inline-flex items-center gap-1 text-body-micro font-semibold text-brand-primary hover:text-brand-primary/80 transition-colors"
      >
        원본 기록 열기
        <ArrowUpRight className="size-3" />
      </button>
    </div>
  );
}

function CitationList({
  citations,
  slotKeysByCitationId,
  onCitationClick,
}: {
  citations: Citation[];
  slotKeysByCitationId: Map<string, Set<keyof Slots>>;
  onCitationClick: (citation: Citation) => void;
}) {
  // 기본 펼침 — 근거 기록은 인수인계의 핵심 신뢰 근거라 접혀 있으면 발견 못 함.
  const [open, setOpen] = useState(true);
  return (
    <div className="border-t border-border-subtle pt-4">
      <button
        type="button"
        onClick={() => setOpen((previous) => !previous)}
        className="flex items-center gap-2 text-body-sm font-bold text-brand-primary hover:text-brand-primary/80 transition-colors"
      >
        {open ? (
          <ChevronDown className="size-4" />
        ) : (
          <ChevronRight className="size-4" />
        )}
        <FileText className="size-4" />
        근거 기록 {citations.length}건 {open ? "접기" : "보기"}
      </button>
      {open && (
        <ul className="mt-3 space-y-2">
          {citations.map((citation) => {
            const slotKeys = slotKeysByCitationId.get(citation.id) ?? new Set();
            const referencedSlotLabels = Array.from(slotKeys).map(
              (slotKey) => SLOT_LABEL[slotKey],
            );
            return (
              <li key={citation.id}>
                <HoverCard openDelay={120} closeDelay={80}>
                  <HoverCardTrigger asChild>
                    <button
                      type="button"
                      onClick={() => onCitationClick(citation)}
                      className="w-full text-left flex gap-2.5 p-3 rounded-xl border bg-surface-card border-border-subtle hover:border-brand-primary/40 transition-colors"
                    >
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-0.5 flex-wrap">
                          <Badge className="bg-surface-hover text-content-tertiary font-medium border-none text-body-micro px-2 py-0 hover:bg-surface-hover">
                            {citation.label}
                          </Badge>
                          <span className="text-body-micro font-mono text-content-tertiary">
                            {citation.ts.slice(0, 16).replace("T", " ")}
                          </span>
                          {referencedSlotLabels.length > 0 && (
                            <span className="text-body-micro text-content-muted">
                              · {referencedSlotLabels.join(", ")}
                            </span>
                          )}
                        </div>
                      </div>
                      <ArrowUpRight className="size-3.5 text-content-muted opacity-50 self-center" />
                    </button>
                  </HoverCardTrigger>
                  <HoverCardContent align="start" sideOffset={6} className="w-[420px] p-3.5">
                    <CitationPreview
                      citation={citation}
                      onClick={() => onCitationClick(citation)}
                    />
                  </HoverCardContent>
                </HoverCard>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
