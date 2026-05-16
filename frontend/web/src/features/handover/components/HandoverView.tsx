"use client";

import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  Sparkles,
  AlertTriangle,
  ChevronDown,
  ChevronRight,
  Loader2,
  ArrowUpRight,
} from "lucide-react";
import { useMemo, useRef, useState } from "react";
import { useQueries, useQueryClient } from "@tanstack/react-query";
import { cn } from "@/lib/utils";
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
import { getPatientDetail } from "@/features/dashboard/api/patient-detail";
import { formatBirthShort } from "@/lib/patient-display";
import {
  handoverChecksKey,
  useGenerateHandover,
  useHandoverChecks,
  useHandoverDetail,
  useHandoverStream,
  usePatchHandoverChecks,
  useRosterSummary,
  useWardEvents,
} from "@/features/handover/hooks/useHandover";
import type {
  Citation,
  HandoverChecksResponse,
  HandoverPayload,
  RosterPatientItem,
  Slot,
  SlotItem,
  Slots,
  WardAdmissionItem,
  WardDischargeItem,
} from "@/features/handover/types/handover";
import { formatHHmm } from "@/lib/time";
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

// 슬롯별 시각 톤 — 모바일 PASS-BAR 디자인과 통일. 8 슬롯 각자 고유 색을 갖고,
// 헤더는 accent 색 7-8% alpha 배경 풀폭 띠 + accent 색 글자 + 우측 N건 카운트로 구성.
// 채도 낮은 차분한 톤이라 SBAR grid 의 6 카드가 동시 노출돼도 시각 충돌 없음.
type SlotAccent =
  | "situation"
  | "patient-problem"
  | "background"
  | "assessment"
  | "safety"
  | "action"
  | "recommendation"
  | "synthesis";

const SBAR_SLOT_ACCENT: Record<keyof Slots, SlotAccent> = {
  patient_problem: "patient-problem",
  assessment: "assessment",
  situation: "situation",
  safety: "safety",
  background: "background",
  action: "action",
  recommendation: "recommendation",
  synthesis: "synthesis",
};

// header: accent 색 7% alpha 배경(헤더 풀폭 띠) + accent 색 글자.
// text: 콜아웃 단독 사용 — Safety/Synthesis 가 강조 톤일 때 가독성 위해 8% alpha 헤더 + accent text.
const SLOT_ACCENT_CLASS: Record<SlotAccent, { header: string; text: string }> = {
  situation:         { header: "bg-slot-situation/[0.07] text-slot-situation",                 text: "text-slot-situation" },
  "patient-problem": { header: "bg-slot-patient-problem/[0.07] text-slot-patient-problem",     text: "text-slot-patient-problem" },
  background:        { header: "bg-slot-background/[0.07] text-slot-background",               text: "text-slot-background" },
  assessment:        { header: "bg-slot-assessment/[0.08] text-slot-assessment",               text: "text-slot-assessment" },
  safety:            { header: "bg-slot-safety/[0.08] text-slot-safety",                       text: "text-slot-safety" },
  action:            { header: "bg-slot-action/[0.07] text-slot-action",                       text: "text-slot-action" },
  recommendation:    { header: "bg-slot-recommendation/[0.07] text-slot-recommendation",       text: "text-slot-recommendation" },
  synthesis:         { header: "bg-slot-synthesis/[0.08] text-slot-synthesis",                 text: "text-slot-synthesis" },
};

// 시프트 자동판정 — 클라이언트 현재 시각 기준. 페이지 마운트 시 1회 산출 (자정 경계 재계산은 SKIP).
// 08:00–16:00 데이 / 16:00–24:00 이브닝 / 00:00–08:00 나이트.
// 표시는 인계 받는 간호사 관점 "이전 시프트 → 현재 시프트" 형식.
type ShiftCode = "DAY" | "EVENING" | "NIGHT";

const SHIFT_LABEL: Record<ShiftCode, string> = {
  DAY: "데이",
  EVENING: "이브닝",
  NIGHT: "나이트",
};

const SHIFT_WINDOW: Record<ShiftCode, string> = {
  DAY: "08:00–16:00",
  EVENING: "16:00–24:00",
  NIGHT: "00:00–08:00",
};

// 현재 시각이 어느 시프트인지(= 인계 받는 시프트). 이전 시프트는 직전 슬롯.
const PREV_SHIFT: Record<ShiftCode, ShiftCode> = {
  DAY: "NIGHT",
  EVENING: "DAY",
  NIGHT: "EVENING",
};

function getCurrentShiftCode(now: Date): ShiftCode {
  const hour = now.getHours();
  if (hour >= 8 && hour < 16) return "DAY";
  if (hour >= 16) return "EVENING";
  return "NIGHT";
}

const SHIFT_TONE: Record<ShiftCode, string> = {
  DAY: "bg-status-warning-surface text-status-warning-strong border-status-warning/30",
  EVENING: "bg-action-blue-surface text-sub-primary border-action-blue/30",
  NIGHT: "bg-sub-primary text-white border-sub-primary",
};

// citation.ts 는 ISO datetime. 시간부가 00:00:00 인 경우는 BE/AI 가 date-only 데이터를
// isoformat 할 때 자정으로 채워진 것으로 간주 — UI 에선 시간을 숨겨 오해 방지.
function hasMeaningfulTime(isoTs: string): boolean {
  return isoTs.slice(11, 19) !== "00:00:00";
}

// verification / severity / risk_tier 라벨/톤은 노출 제거 — 간호사 화면 노이즈로 판단.
// 데이터(RosterPatientItem.risk_score, SlotItem.severity_flag, Slot.verification) 는 응답에 그대로 유지.

// 화면의 base 행 — 담당 환자(wardPatient)는 항상 존재, 인수인계 리포트(roster)는 옵션.
// roster-summary 가 빈 응답이어도 환자 카드는 사라지지 않도록 wardPatient 를 1차 키로 둔다.
type HandoverRow = {
  wardPatient: WardPatient;
  roster: RosterPatientItem | null;
};

export function HandoverView() {
  const router = useRouter();
  const [selectedHandoverId, setSelectedHandoverId] = useState<string | null>(
    null,
  );
  const [activeJobId, setActiveJobId] = useState<string | null>(null);
  // 현재 시프트 — 페이지 마운트 시 1회 산출. 자정 경계에서 갱신 안 함 (재방문 시 자연 갱신).
  // 인계 받는 시프트(현재) 와 인계 주는 시프트(이전) 둘 다 노출.
  const handoverShift = useMemo(() => {
    const to = getCurrentShiftCode(new Date());
    const from = PREV_SHIFT[to];
    return { from, to };
  }, []);

  const rosterQuery = useRosterSummary();
  const wardEventsQuery = useWardEvents();
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

  // myPatients 각각의 PatientDetail — diseaseName 등 slim 응답에 없는 필드 채우기 위함.
  // queryKey 는 usePatientDetail (`["patient", id]`) 와 동일 → EMRGrid 진입 시 받은 캐시와 공유, 재요청 없음.
  const patientDetailQueries = useQueries({
    queries: myPatients.map((patient) => ({
      queryKey: ["patient", patient.patientId] as const,
      queryFn: () => getPatientDetail(patient.patientId),
      enabled: patient.patientId !== null,
    })),
  });
  const diseaseNameByPatientId = useMemo(() => {
    const map = new Map<number, string>();
    myPatients.forEach((patient, index) => {
      const detail = patientDetailQueries[index]?.data;
      if (detail?.diseaseName) map.set(patient.patientId, detail.diseaseName);
    });
    return map;
  }, [myPatients, patientDetailQueries]);

  const rows = useMemo<HandoverRow[]>(
    () =>
      myPatients.map((wardPatient) => ({
        wardPatient,
        roster: rosterByEncounterId.get(wardPatient.encounterId) ?? null,
      })),
    [myPatients, rosterByEncounterId],
  );

  // 통합 요약 상단의 3분할 통계 — RosterSummary 응답 합산만 사용 (추가 API 호출 없음).
  const rosterStats = useMemo(() => {
    const patients = rosterQuery.data?.patients ?? [];
    if (patients.length === 0) return null;
    let newRecordTotal = 0;
    let rulesFiredTotal = 0;
    patients.forEach((patient) => {
      newRecordTotal += patient.freshness?.new_records_since_report ?? 0;
      rulesFiredTotal += patient.rules_fired_brief.length;
    });
    return {
      patientCount: patients.length,
      newRecordTotal,
      rulesFiredTotal,
    };
  }, [rosterQuery.data]);

  // 리스크 상위 환자 — RosterSummary.patients 는 이미 risk_score desc 정렬돼 들어옴.
  // wardPatient 매칭으로 이름/병실 채우고, 매칭 안 되는 항목은 제외 (담당 해제된 케이스 방어).
  const topRiskRows = useMemo<Array<{ roster: RosterPatientItem; wardPatient: WardPatient }>>(() => {
    const patients = rosterQuery.data?.patients ?? [];
    const wardPatientByEncounterId = new Map<number, WardPatient>();
    myPatients.forEach((patient) => {
      wardPatientByEncounterId.set(patient.encounterId, patient);
    });
    const rows: Array<{ roster: RosterPatientItem; wardPatient: WardPatient }> = [];
    for (const roster of patients) {
      const wardPatient = wardPatientByEncounterId.get(roster.encounter_id);
      if (wardPatient) rows.push({ roster, wardPatient });
      if (rows.length >= 3) break;
    }
    return rows;
  }, [rosterQuery.data, myPatients]);

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
    <div className="flex flex-col h-screen bg-surface-base">
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
            AI 인수인계
          </h2>
          <span
            title={`인계 ${SHIFT_LABEL[handoverShift.from]} 시프트(${SHIFT_WINDOW[handoverShift.from]}) → ${SHIFT_LABEL[handoverShift.to]} 시프트(${SHIFT_WINDOW[handoverShift.to]})`}
            className={cn(
              "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-body-micro font-bold leading-none border",
              SHIFT_TONE[handoverShift.to],
            )}
          >
            <span className="opacity-70">{SHIFT_LABEL[handoverShift.from]}</span>
            <span className="opacity-60">→</span>
            <span>{SHIFT_LABEL[handoverShift.to]}</span>
          </span>
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
                {rows.length}명
              </span>
            </div>
            <p className="mt-1.5 text-body-micro text-content-tertiary">
              클릭하면 해당 환자 카드로 이동해요
            </p>
          </div>
          <div className="flex-1 min-h-0 overflow-y-auto p-3 space-y-2">
            {rows.length === 0 ? (
              <div className="text-center py-8 text-body-sm text-content-tertiary">
                담당 환자가 없습니다. 대시보드에서 담당 환자를 지정하세요.
              </div>
            ) : (
              rows.map(({ wardPatient, roster }) => {
                const fresh =
                  roster?.freshness?.new_records_since_report ?? 0;
                const diseaseName = diseaseNameByPatientId.get(
                  wardPatient.patientId,
                );
                const birthLabel = formatBirthShort(wardPatient.birthDate);
                return (
                  <button
                    key={wardPatient.encounterId}
                    onClick={() => handleSelectPatient(wardPatient.encounterId)}
                    className="w-full text-left px-4 py-3 rounded-xl bg-transparent hover:bg-surface-hover transition-all group"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-baseline gap-1.5 min-w-0">
                        <span className="text-body-base font-semibold truncate text-content-primary">
                          {wardPatient.name}
                        </span>
                        {birthLabel && (
                          <span className="text-[13px] leading-tight font-bold text-content-secondary shrink-0">
                            {birthLabel}
                          </span>
                        )}
                      </div>
                      <span className="text-body-micro text-content-tertiary shrink-0">
                        {wardPatient.roomName} {wardPatient.bedName}
                      </span>
                    </div>
                    <div className="mt-1.5 flex items-center gap-1.5">
                      <span
                        className={cn(
                          "text-body-sm truncate flex-1",
                          diseaseName
                            ? "text-content-secondary"
                            : "text-content-tertiary italic",
                        )}
                      >
                        {diseaseName ??
                          wardPatient.chiefComplaint ??
                          "병명 미등록"}
                      </span>
                      {fresh > 0 && (
                        <span
                          title="리포트 이후 신규 간호기록"
                          className="px-1.5 py-0.5 rounded-full bg-status-warning-surface text-status-warning-strong text-body-micro font-bold leading-none shrink-0"
                        >
                          {fresh}건
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
              {/* [TOP] 오늘의 입퇴원 — 입원/퇴원 좌·우 2분할 박스. 박스 헤더(타이틀)는 제거하고 본문만 노출. */}
              <WardEventsBox
                admissions={wardEventsQuery.data?.admissions ?? []}
                discharges={wardEventsQuery.data?.discharges ?? []}
                isPending={wardEventsQuery.isPending}
                isError={wardEventsQuery.isError}
              />

              {/* [MID-1] 시프트 통계 4분할 — RosterSummary 합산 파생, 아이콘 없이 토큰만으로 위계 구성 */}
              {rosterStats && <ShiftStatRow stats={rosterStats} />}

              {/* [MID-2] 리스크 상위 환자 — 클릭 시 해당 환자 카드로 스크롤 (handleSelectPatient 재사용) */}
              {topRiskRows.length > 0 && (
                <RiskTopList rows={topRiskRows} onSelectPatient={handleSelectPatient} />
              )}

              {/* [BOTTOM] 환자별 카드 */}
              {rows.length === 0 ? (
                <div className="bg-surface-card rounded-2xl shadow-sm px-8 py-16 flex flex-col items-center gap-3 text-content-tertiary">
                  <Sparkles className="size-6 opacity-50" />
                  <Text className="text-body-sm font-medium">
                    담당 환자가 없습니다
                  </Text>
                </div>
              ) : (
                rows.map(({ wardPatient, roster }) => (
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

// 입원/퇴원 좌·우 2분할 박스 — 타이틀 헤더는 없음(섹션 헤더가 시각 위계 담당).
function WardEventsBox({
  admissions,
  discharges,
  isPending,
  isError,
}: {
  admissions: WardAdmissionItem[];
  discharges: WardDischargeItem[];
  isPending: boolean;
  isError: boolean;
}) {
  return (
    <div className="bg-surface-card rounded-2xl shadow-sm overflow-hidden">
      {isPending ? (
        <div className="px-6 py-8 flex items-center justify-center text-content-tertiary">
          <Loader2 className="size-4 animate-spin" />
        </div>
      ) : isError ? (
        <div className="px-6 py-8 text-center text-body-sm text-status-danger">
          입퇴원 목록을 불러오지 못했습니다
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 md:divide-x md:divide-y-0 divide-y divide-border-subtle">
          <WardEventSection
            title="입원"
            items={admissions.map((item) => ({
              key: item.encounterId,
              patientName: item.patientName,
              roomName: item.roomName,
              bedName: item.bedName,
              diseaseOrCc:
                item.diseaseName || item.chiefComplaint || item.surgeryName || "-",
              ts: item.periodStart,
            }))}
            emptyMessage="오늘 입원한 환자가 없습니다"
          />
          <WardEventSection
            title="퇴원"
            items={discharges.map((item) => ({
              key: item.encounterId,
              patientName: item.patientName,
              roomName: item.roomName,
              bedName: item.bedName,
              diseaseOrCc:
                item.diseaseName || item.chiefComplaint || item.surgeryName || "-",
              ts: item.periodEnd,
            }))}
            emptyMessage="오늘 퇴원한 환자가 없습니다"
          />
        </div>
      )}
    </div>
  );
}

function WardEventSection({
  title,
  items,
  emptyMessage,
}: {
  title: string;
  items: Array<{
    key: number;
    patientName: string;
    roomName: string;
    bedName: string;
    diseaseOrCc: string;
    ts: string;
  }>;
  emptyMessage: string;
}) {
  return (
    <section className="px-6 py-4">
      <div className="flex items-baseline gap-2 mb-4">
        <h3 className="text-title-md font-bold leading-none text-content-secondary">
          {title}
        </h3>
        <span className="text-body-sm font-bold text-content-tertiary leading-none">
          {items.length}건
        </span>
      </div>
      {items.length === 0 ? (
        <p className="text-body-sm text-content-muted">{emptyMessage}</p>
      ) : (
        <ul className="flex flex-col gap-2.5">
          {items.map((item) => {
            const roomBed = [item.roomName.replace(/호$/, ""), item.bedName]
              .filter(Boolean)
              .join("-");
            const showTime = hasMeaningfulTime(item.ts);
            return (
              <li key={item.key} className="flex flex-col gap-0.5">
                <div className="flex items-center gap-2">
                  <span className="text-body-base font-bold text-content-primary truncate">
                    {item.patientName}
                  </span>
                  {roomBed && (
                    <span className="px-1.5 py-0.5 rounded bg-[#F7F8FA] text-content-secondary text-body-micro font-bold leading-none shrink-0">
                      {roomBed}
                    </span>
                  )}
                  {showTime && (
                    <span className="ml-auto text-body-micro font-bold text-content-tertiary leading-none shrink-0">
                      {formatHHmm(item.ts)}
                    </span>
                  )}
                </div>
                <span className="text-body-sm font-medium text-content-secondary leading-snug truncate">
                  {item.diseaseOrCc}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

// 통합 요약 박스 아래 4분할 통계 카드. 좁은 폭에선 2x2 wrap.
// 디자인 결정: 아이콘 없이 라벨 + 큰 숫자만으로 위계 구성. Tailwind 4 의 text-body-* 토큰이
// line-height 를 자체 적용해서 라벨/숫자 모두 leading-none 명시.
function ShiftStatRow({
  stats,
}: {
  stats: {
    patientCount: number;
    newRecordTotal: number;
    rulesFiredTotal: number;
  };
}) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
      <StatCard
        label="담당 환자"
        value={`${stats.patientCount}명`}
        description="이번 시프트에서 인계받는 환자 수"
      />
      <StatCard
        label="새 기록"
        value={`${stats.newRecordTotal}건`}
        description="리포트 생성 이후 추가된 확정 간호기록 수"
      />
      <StatCard
        label="위험 신호"
        value={`${stats.rulesFiredTotal}회`}
        description="낙상·고위험 약물·DNR·격리·알러지 등 안전 점검 룰이 환자 상태와 일치한 횟수"
      />
    </div>
  );
}

function StatCard({
  label,
  value,
  description,
}: {
  label: string;
  value: string;
  description?: string;
}) {
  return (
    <div
      title={description}
      className="rounded-xl border border-border-subtle bg-surface-card p-4 flex flex-col gap-2"
    >
      <span className="text-body-micro font-bold text-content-secondary leading-none">
        {label}
      </span>
      <span className="text-title-lg font-bold text-content-primary leading-none tracking-tight">
        {value}
      </span>
    </div>
  );
}

// 리스크 상위 환자 행 — RosterSummary.patients 가 이미 risk_score desc 정렬돼 들어오므로 slice 만 한 결과.
// 클릭 시 onSelectPatient → cardRefs 스크롤 (HandoverView 의 handleSelectPatient 재사용).
function RiskTopList({
  rows,
  onSelectPatient,
}: {
  rows: Array<{ roster: RosterPatientItem; wardPatient: WardPatient }>;
  onSelectPatient: (encounterId: number) => void;
}) {
  return (
    <div className="bg-surface-card rounded-2xl shadow-sm overflow-hidden">
      <div className="px-6 py-4 border-b border-border-base flex items-center gap-2">
        <span className="font-bold text-brand-primary text-title-md leading-none">
          리스크 상위 환자
        </span>
      </div>
      <ul className="divide-y divide-border-subtle">
        {rows.map(({ roster, wardPatient }) => {
          const newRecordCount =
            roster.freshness?.new_records_since_report ?? 0;
          return (
            <li key={roster.encounter_id}>
              <button
                type="button"
                onClick={() => onSelectPatient(wardPatient.encounterId)}
                className="w-full text-left px-6 py-3.5 flex items-center gap-4 hover:bg-surface-hover transition-colors"
              >
                <div className="flex items-baseline gap-2 shrink-0 min-w-[120px]">
                  <span className="text-body-base font-semibold text-content-primary leading-none">
                    {wardPatient.name}
                  </span>
                  <span className="text-body-sm text-content-secondary leading-none">
                    {wardPatient.roomName} {wardPatient.bedName}
                  </span>
                </div>
                <span className="flex-1 min-w-0 text-body-sm text-content-secondary truncate">
                  {roster.header}
                </span>
                {newRecordCount > 0 && (
                  <span
                    title="리포트 이후 신규 간호기록"
                    className="shrink-0 px-2 py-0.5 rounded-full bg-status-warning-surface text-status-warning-strong text-body-micro font-bold leading-none"
                  >
                    {newRecordCount}건
                  </span>
                )}
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

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
      {/* [CARD HEADER] — roster 있으면 헤더 전체가 토글 영역. 안쪽 "상세" 버튼은 stopPropagation 으로 이중 토글 방지. */}
      <div
        className={cn(
          "px-5 py-3.5 bg-surface-base/70 border-b border-border-base flex items-center gap-3",
          roster && "cursor-pointer hover:bg-surface-hover/60 transition-colors",
        )}
        onClick={roster ? onToggleDetail : undefined}
        role={roster ? "button" : undefined}
        tabIndex={roster ? 0 : undefined}
        onKeyDown={
          roster
            ? (event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  onToggleDetail();
                }
              }
            : undefined
        }
      >
        <div className="flex items-center gap-2.5 min-w-0">
          <h3 className="text-title-md font-bold text-content-primary truncate leading-tight tracking-tight">
            {wardPatient.name}
          </h3>
          <Text className="text-body-sm text-content-secondary font-medium shrink-0">
            {wardPatient.roomName} {wardPatient.bedName}
          </Text>
        </div>
        {roster && (
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              onToggleDetail();
            }}
            className="ml-auto flex items-center gap-1 text-body-micro font-semibold text-content-tertiary hover:text-content-primary leading-none"
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
        // 본문 영역도 헤더와 동일하게 토글. nested 버튼/링크 (citation chip 등) 는 closest 체크로 무시.
        <div
          className="cursor-pointer"
          onClick={(event) => {
            const target = event.target as HTMLElement | null;
            if (target?.closest("button, a, [role='button']")) return;
            // 텍스트 드래그(selection) 직후 click 발생 시 토글되지 않게.
            if (window.getSelection()?.toString()) return;
            onToggleDetail();
          }}
        >
          {/* [HEADER LINE] — 환자별 요약 본문. 박스 영역 안에서 상하 가운데 정렬.
              <Text> 의 size default(text-sm/14px) 가 className 과 충돌해 실제 14px 로 보이는 문제 회피하려고 <p> 로 직접 적용. */}
          <div className="px-6 py-5 min-h-[88px] flex items-center">
            <p className="text-body-base leading-relaxed font-semibold text-content-primary whitespace-pre-wrap">
              {roster.header}
            </p>
          </div>

          {/* [RULES BRIEF] — 항상 보임 */}
          {roster.rules_fired_brief.length > 0 && (
            <div className="px-6 pt-4">
              <div
                className="flex items-center gap-2 mb-2.5"
                title="낙상·고위험 약물·DNR·격리·알러지 등 안전 점검 룰이 환자 상태와 일치한 항목"
              >
                <AlertTriangle className="size-4 text-status-warning" />
                <h4 className="text-body-base font-bold text-status-warning-strong">
                  위험 신호
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

          {/* [DETAIL — PASS-BAR] — 펼친 상태에서 이 영역 클릭은 토글하지 않음 (텍스트 읽기/근거 클릭용). */}
          {isSelected && (
            <div
              className="px-6 pt-5 pb-6 space-y-5 cursor-auto"
              onClick={(event) => event.stopPropagation()}
            >
              {detailQuery.isPending ? (
                <div className="flex flex-col items-center gap-2 py-10 text-content-tertiary">
                  <Loader2 className="size-5 animate-spin" />
                  <p className="text-body-micro">PASS-BAR 상세 조회 중...</p>
                </div>
              ) : detailQuery.isError || !detailQuery.data?.autoSummaryJson ? (
                <div className="text-center py-8 text-body-sm text-status-danger">
                  상세 리포트를 불러오지 못했습니다
                </div>
              ) : (
                <PassBarDetail
                  handoverId={String(roster.handover_id)}
                  payload={detailQuery.data.autoSummaryJson}
                  onCitationClick={onCitationClick}
                />
              )}
            </div>
          )}
        </div>
      ) : (
        <div className="px-6 py-8 flex flex-col items-center gap-2 text-center">
          <Sparkles className="size-5 text-content-muted opacity-50" />
          <Text className="text-body-sm font-semibold text-content-secondary">
            아직 인수인계 리포트가 없어요
          </Text>
          <Text className="text-body-micro text-content-tertiary">
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
  handoverId,
  payload,
  onCitationClick,
}: {
  handoverId: string;
  // payload 는 호출 측에서 null 체크 후 전달 (HandoverPayload 보장).
  payload: HandoverPayload;
  onCitationClick: (citation: Citation) => void;
}) {
  // 체크리스트는 synthesis 슬롯 기반 — BE 가 키 포맷 `synthesis.{index}` 만 허용.
  // 종합 콜아웃([1]) 과 체크리스트 항목이 동일 슬롯이라 콜아웃은 제거하고 체크리스트가 그 자리 대체.
  const queryClient = useQueryClient();
  const checksQuery = useHandoverChecks(handoverId);
  const patchMutation = usePatchHandoverChecks(handoverId);

  // 시간 정보 없는 citation 필터링 — AI 가 Tier-2(staticfacts) 인용·환각·시드 자정 케이스를 만들 때
  // ts 가 00:00:00 으로 떨어지는 패턴. 원본 추적 가능한 정상 citation 만 화면에 노출.
  const visibleCitations = useMemo(
    () => payload.citations.filter((citation) => hasMeaningfulTime(citation.ts)),
    [payload.citations],
  );
  const visibleCitationIdSet = useMemo(
    () => new Set(visibleCitations.map((citation) => citation.id)),
    [visibleCitations],
  );

  // 한 citation 이 여러 slot 에 인용될 수 있음 — CitationList 에서 어느 슬롯에 인용됐는지 표시.
  // 위에서 거른 visibleCitationIdSet 에 속하는 id 만 매핑에 포함해 popover 도 일관 처리.
  const slotKeysByCitationId = useMemo(() => {
    const map = new Map<string, Set<keyof Slots>>();
    (Object.keys(payload.slots) as Array<keyof Slots>).forEach((slotKey) => {
      payload.slots[slotKey].items.forEach((item) => {
        item.citation_ids.forEach((cid) => {
          if (!visibleCitationIdSet.has(cid)) return;
          if (!map.has(cid)) map.set(cid, new Set());
          map.get(cid)!.add(slotKey);
        });
      });
    });
    return map;
  }, [payload.slots, visibleCitationIdSet]);

  const checklistItems = payload.slots.synthesis.items;
  const checkedItemsJson = checksQuery.data?.checkedItemsJson;
  const checkedByIndex = useMemo<Record<number, boolean>>(() => {
    const map: Record<number, boolean> = {};
    const checks = checkedItemsJson ?? {};
    checklistItems.forEach((_, index) => {
      map[index] = checks[`synthesis.${index}`] !== undefined;
    });
    return map;
  }, [checklistItems, checkedItemsJson]);

  const handleToggle = (index: number) => {
    const key = `synthesis.${index}`;
    const nextChecked = checkedByIndex[index] !== true;
    // Optimistic 업데이트 — 체크박스 즉시 반응. 실패 시 invalidate 가 서버 상태로 되돌림.
    queryClient.setQueryData<HandoverChecksResponse>(
      handoverChecksKey(handoverId),
      (previous) => {
        const baseChecks = previous?.checkedItemsJson ?? {};
        const nextChecks = { ...baseChecks };
        if (nextChecked) {
          // 누른 사람/시각은 서버가 채움 — 로컬은 빈 객체로 두면 "키 존재 = 체크 ON" 판정만 통과.
          nextChecks[key] = { by: 0, at: new Date().toISOString() };
        } else {
          delete nextChecks[key];
        }
        return {
          handoverId: previous?.handoverId ?? handoverId,
          checkedItemsJson: nextChecks,
        };
      },
    );
    patchMutation.mutate({ [key]: nextChecked });
  };

  return (
    <>
      {/* [1] 체크리스트 — synthesis 슬롯 기반. BE 영속. */}
      <ChecklistSection
        items={checklistItems}
        checkedByIndex={checkedByIndex}
        onToggle={handleToggle}
      />

      {/* [2] Safety 콜아웃 — 낙상/격리/DNR/알러지/금기 등 안전 사항 */}
      {payload.slots.safety.items.length > 0 && (
        <SlotCallout
          label="Safety"
          slot={payload.slots.safety}
          accent="safety"
        />
      )}

      {/* [3] SBAR grid — synthesis/safety 제외 6 슬롯 */}
      <div className="grid grid-cols-2 gap-3">
        {SBAR_SLOT_ORDER.map((key) => (
          <SlotCard
            key={key}
            label={SLOT_LABEL[key]}
            slot={payload.slots[key]}
            accent={SBAR_SLOT_ACCENT[key]}
          />
        ))}
      </div>

      {/* [4] Citations 전체 목록 — 인용된 슬롯 라벨과 함께 표시 */}
      {visibleCitations.length > 0 && (
        <CitationList
          citations={visibleCitations}
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
        <h4 className="text-body-base font-bold text-brand-primary leading-none">
          체크리스트
        </h4>
        {items.length > 0 && (
          <span className="text-body-micro text-content-tertiary leading-none">
            ({doneCount}/{items.length})
          </span>
        )}
      </div>
      {items.length === 0 ? (
        <p className="text-body-sm text-content-tertiary leading-relaxed">
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

// 상단 콜아웃 (Synthesis · Safety) — 풀폭, 강조 톤. SlotCard 와 같은 데이터지만 시각 위계가 다름.
// 디자인: 헤더 풀폭 색띠(accent 8% alpha) + 우측 N건 카운트 + 본문 흰배경(가독성).
function SlotCallout({
  label,
  slot,
  accent,
  icon,
}: {
  label: string;
  slot: Slot;
  accent: SlotAccent;
  icon?: React.ReactNode;
}) {
  const tokens = SLOT_ACCENT_CLASS[accent];
  const itemCount = slot.items.length;
  return (
    <div className="rounded-xl border border-border-subtle bg-surface-card overflow-hidden">
      <div
        className={cn(
          "flex items-center gap-2 px-4 py-3 border-b border-border-subtle",
          tokens.header,
        )}
      >
        {icon}
        <h4 className="text-body-base font-bold leading-none">{label}</h4>
        {itemCount > 0 && (
          <span className="ml-auto text-body-sm font-semibold leading-none">
            {itemCount}건
          </span>
        )}
      </div>
      <ul className="px-4 py-3 space-y-2">
        {slot.items.map((item, index) => (
          <SlotItemRow key={index} item={item} />
        ))}
      </ul>
    </div>
  );
}

// SBAR grid 의 6 슬롯 카드. 모바일 PASS-BAR 디자인과 통일 — accent 7% alpha 풀폭 헤더 + 우측 N건 카운트.
function SlotCard({
  label,
  slot,
  accent,
}: {
  label: string;
  slot: Slot;
  accent: SlotAccent;
}) {
  const tokens = SLOT_ACCENT_CLASS[accent];
  const itemCount = slot.items.length;
  return (
    <div className="rounded-xl border border-border-subtle bg-surface-card overflow-hidden flex flex-col">
      <div
        className={cn(
          "flex items-center gap-2 px-3.5 py-2.5 border-b border-border-subtle",
          tokens.header,
        )}
      >
        <h4 className="text-body-base font-bold leading-none">{label}</h4>
        {itemCount > 0 && (
          <span className="ml-auto text-body-xs font-semibold leading-none">
            {itemCount}건
          </span>
        )}
      </div>
      <div className="px-3.5 py-3">
        {itemCount === 0 ? (
          <p className="text-body-micro text-content-tertiary">—</p>
        ) : (
          <ul className="space-y-2">
            {slot.items.map((item, index) => (
              <SlotItemRow key={index} item={item} />
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

// value 본문에 박힌 HH:mm 시간 패턴을 추출 — 모든 항목의 시간 노출을 메타 라인 한 곳으로 통일하기 위한 정규화.
// LLM 이 어떤 항목은 value 안에, 다른 항목은 time_window 에 시간을 박아 평가 슬롯에서 위치가 들쭉날쭉해지는 문제 해결.
// 정확히 1개 시간 매치만 추출 (다수 시간이 있는 텍스트는 본문 의미가 강하므로 그대로 둠).
const TIME_HHMM_PATTERN = /\b(\d{1,2}:\d{2})\b/g;

function extractInlineTime(text: string | null): { stripped: string | null; time: string | null } {
  if (!text) return { stripped: null, time: null };
  const matches = text.match(TIME_HHMM_PATTERN);
  if (!matches || matches.length !== 1) return { stripped: text, time: null };
  const time = matches[0];
  // 시간 + 주변 구두점/공백 정리
  const stripped = text
    .replace(time, "")
    .replace(/\s{2,}/g, " ")
    .replace(/^[\s\-·:,]+|[\s\-·:,]+$/g, "")
    .trim();
  return { stripped: stripped || null, time };
}

// 슬롯 안 한 줄짜리 항목 — value(요약) + quote(원문 인용 박스) + meta (time_window, trend) + contingency.
// severity_flag(stable/watcher/unstable) 칩은 노출 제거 — 간호사 화면 노이즈 회피.
// citation 표시도 슬롯에서 제거됨 — 출처는 카드 하단 "근거 기록" 영역(CitationList) 에만.
// value 와 quote 가 둘 다 있을 때 quote 는 별도 인용 박스(brand-surface)로 시각 구분.
function SlotItemRow({ item }: { item: SlotItem }) {
  const rawValueText = item.value?.trim() || null;
  const quoteText = item.quote?.trim() || null;
  // value 안 시간 패턴을 메타로 끌어올림 — 모든 항목이 같은 위치에 시간 표시.
  const { stripped: valueText, time: timeFromValue } = extractInlineTime(rawValueText);
  // value 도 quote 도 없으면 kind 라도 폴백.
  const headline = valueText ?? (quoteText ? null : (item.kind ?? "(빈 항목)"));

  // 메타 라인 — 시간(time_window 우선, 없으면 value 에서 추출) + trend. dedup 후 ` · ` join.
  const timeWindow = item.time_window?.trim() || null;
  const trendText = item.trend?.trim() || null;
  const metaParts = Array.from(
    new Set(
      [timeWindow ?? timeFromValue, trendText].filter(
        (part): part is string => !!part,
      ),
    ),
  );

  return (
    <li className="text-body-sm leading-relaxed flex flex-col gap-1.5">
      {headline && (
        <div className="flex flex-wrap items-start gap-1.5">
          <span className="text-content-primary flex-1 min-w-0 break-words">
            {headline}
          </span>
        </div>
      )}
      {quoteText && (
        <div className="rounded-md bg-brand-surface/40 px-2 py-1.5">
          <p className="text-body-sm text-content-secondary leading-snug break-words">
            “{quoteText}”
          </p>
        </div>
      )}

      {metaParts.length > 0 && (
        <div className="text-body-micro text-content-secondary">
          {metaParts.join(" · ")}
        </div>
      )}

      {/* contingency — "if X then Y" 형식의 조건부 조치. 안전 카드에서 특히 중요. */}
      {item.contingency && (
        <div className="text-body-micro text-status-warning-strong leading-snug">
          ↳ {item.contingency}
        </div>
      )}

      {/* 슬롯 안 citation chip 은 제거 — 출처는 카드 하단의 "근거 기록" 영역(CitationList) 에만 표시. */}
    </li>
  );
}

// hover preview 공용 — chip / citation list 양쪽에서 사용.
// popover 영역 어디를 눌러도 원본 기록으로 이동. 별도 "열기" 버튼은 없앰.
function CitationPreview({
  citation,
  onClick,
}: {
  citation: Citation;
  onClick: () => void;
}) {
  const showTime = hasMeaningfulTime(citation.ts);
  return (
    <button
      type="button"
      onClick={onClick}
      className="w-full text-left flex flex-col gap-2 cursor-pointer focus:outline-none focus-visible:ring-1 focus-visible:ring-brand-primary/40 rounded"
    >
      <div className="flex items-center gap-2">
        <Badge className="bg-brand-surface text-brand-primary border-none text-body-micro font-bold leading-none px-2 py-0.5">
          {citation.label}
        </Badge>
        {/* 날짜/시간 — 카드 리스트와 동일 폰트. 시간부 00:00:00 이면 숨김. */}
        <span className="ml-auto flex items-baseline gap-2 leading-none">
          <span className="text-body-sm tabular-nums font-bold text-content-primary">
            {citation.ts.slice(0, 10).replace(/-/g, ".")}
          </span>
          {showTime && (
            <span className="text-body-sm tabular-nums font-bold text-content-primary">
              {citation.ts.slice(11, 16)}
            </span>
          )}
        </span>
      </div>
      {/* 본문 발췌 — BE/AI 가 line_range 구간 원본 텍스트를 채워줌. 없으면 행 자체 안 그림. */}
      {citation.excerpt && (
        <p className="text-body-sm text-content-secondary leading-relaxed line-clamp-4 break-words whitespace-pre-wrap">
          {citation.excerpt}
        </p>
      )}
    </button>
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
        className="w-full flex items-center gap-2 text-body-base font-bold text-brand-primary hover:bg-brand-surface/40 transition-colors px-2 py-2 -mx-2 rounded-md"
      >
        {open ? (
          <ChevronDown className="size-4" />
        ) : (
          <ChevronRight className="size-4" />
        )}
        근거 기록 {citations.length}건
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
                      <div className="flex-1 min-w-0 flex flex-col gap-1.5">
                        <div className="flex items-center gap-3 flex-wrap">
                          {/* 라벨 — brand 색으로 강조, 인용 ID */}
                          <span className="text-body-sm font-bold text-brand-primary leading-none">
                            {citation.label}
                          </span>
                          {/* 내용 — 어느 슬롯의 근거인지, 가장 또렷한 톤 */}
                          {referencedSlotLabels.length > 0 && (
                            <span className="text-body-sm font-semibold text-content-primary leading-none">
                              {referencedSlotLabels.join(", ")}
                            </span>
                          )}
                          {/* 날짜 · 시간 — 우측 정렬, 자릿수 고정. 폰트 무게/색 통일. 시간부 00:00:00 이면 숨김. */}
                          <span className="ml-auto flex items-baseline gap-2 leading-none">
                            <span className="text-body-sm tabular-nums font-bold text-content-primary">
                              {citation.ts.slice(0, 10).replace(/-/g, ".")}
                            </span>
                            {hasMeaningfulTime(citation.ts) && (
                              <span className="text-body-sm tabular-nums font-bold text-content-primary">
                                {citation.ts.slice(11, 16)}
                              </span>
                            )}
                          </span>
                        </div>
                        {/* 본문 발췌 — line_range 구간 원본 텍스트. 카드 한 줄 미리보기, 자세한 본문은 hover popover. */}
                        {citation.excerpt && (
                          <p className="text-body-xs text-content-secondary leading-snug line-clamp-1 break-words">
                            {citation.excerpt}
                          </p>
                        )}
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
