"use client";

import { useRouter } from "next/navigation";
import {
  ArrowLeft,
  Sparkles,
  AlertTriangle,
  ChevronDown,
  ChevronRight,
  Loader2,
  ChevronUp,
  FileText,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { cn } from "@/lib/utils";
import { Text } from "@/components/ui/text";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import { formatBirthShort, formatGenderShort } from "@/lib/patient-display";
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
  SeverityFlag,
  Slot,
  SlotItem,
  Slots,
  WardAdmissionItem,
  WardDischargeItem,
} from "@/features/handover/types/handover";
import { formatHHmm } from "@/lib/time";
import type { WardPatient } from "@/features/patient/types/ward-patient";

// "302호" + "5" → "302-5". 호실 라벨에 이미 "호" 가 붙어 있는 경우만 제거.
function formatRoomBed(roomName: string, bedName: string): string {
  const room = (roomName ?? "").replace(/호$/, "");
  return [room, bedName].filter(Boolean).join("-");
}

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

// 7 슬롯 4+3 그리드 — synthesis 는 체크리스트 전용 슬롯이라 grid 에서 제외.
// 1줄(4칸): 안전 · 상황 · 조치 · 평가 — 안전을 첫줄 첫칸에 배치해 시급 도메인을 우선 노출.
// 2줄(3칸): 권고 · 배경 · 환자문제 — 3등분 균등 분할로 첫줄과 동일 풀폭.
const ROW1_SLOT_ORDER: Array<keyof Slots> = [
  "safety",
  "situation",
  "action",
  "assessment",
];
const ROW2_SLOT_ORDER: Array<keyof Slots> = [
  "recommendation",
  "background",
  "patient_problem",
];
// citation 우선순위 매핑에 쓰일 전체 슬롯 순서 (synthesis 제외).
const ALL_SLOT_ORDER: Array<keyof Slots> = [
  ...ROW1_SLOT_ORDER,
  ...ROW2_SLOT_ORDER,
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
  // 선택된 환자(encounterId) — 메인 영역이 이 1명의 상세를 그림. 첫 담당 환자 자동 선택.
  const [selectedEncounterId, setSelectedEncounterId] = useState<number | null>(
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
  // 헤더 좌측 — "AI 인수인계" 옆에 노출할 오늘 날짜 (예: "2026년 5월 18일 월요일").
  const todayLabel = useMemo(
    () =>
      new Intl.DateTimeFormat("ko-KR", { dateStyle: "full" }).format(new Date()),
    [],
  );

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

  const rows = useMemo<HandoverRow[]>(
    () =>
      myPatients.map((wardPatient) => ({
        wardPatient,
        roster: rosterByEncounterId.get(wardPatient.encounterId) ?? null,
      })),
    [myPatients, rosterByEncounterId],
  );

  // 첫 담당 환자 자동 선택 — 마운트 / myPatients 변동 시 현재 선택이 유효하지 않으면 첫 항목으로 설정.
  useEffect(() => {
    if (myPatients.length === 0) {
      if (selectedEncounterId !== null) setSelectedEncounterId(null);
      return;
    }
    const stillExists = myPatients.some(
      (patient) => patient.encounterId === selectedEncounterId,
    );
    if (!stillExists) setSelectedEncounterId(myPatients[0].encounterId);
  }, [myPatients, selectedEncounterId]);

  const selectedRow = useMemo(
    () => rows.find((row) => row.wardPatient.encounterId === selectedEncounterId) ?? null,
    [rows, selectedEncounterId],
  );

  const handleSelectPatient = (encounterId: number) => {
    setSelectedEncounterId(encounterId);
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
    <div className="flex flex-col h-screen bg-[#F7F8FB]">
      {/* Top Header — 와이어프레임 톤: 흰 배경 56px, 좌측 brand 워드마크 + 페이지 타이틀. */}
      <header className="h-14 flex items-center justify-between px-6 bg-white border-b border-[#E4E7EE] shrink-0 z-50">
        <div className="flex items-center gap-4">
          <button
            onClick={() => router.push("/dashboard")}
            className="p-2 -ml-2 rounded-full hover:bg-[#F2F3F7] transition-all text-content-muted"
          >
            <ArrowLeft className="w-6 h-6" />
          </button>
          <span className="text-2xl font-semibold text-[#1a1b1d] leading-none">
            AI 인수인계
          </span>
          <span className="text-body-base font-medium text-content-tertiary tabular-nums">
            {todayLabel}
          </span>
          {/* 시프트 흐름 — 이전 시프트(회색) → 현재 시프트(brand) 두 칩만 노출. */}
          <span
            title={`인계 ${SHIFT_LABEL[handoverShift.from]} 시프트(${SHIFT_WINDOW[handoverShift.from]}) → ${SHIFT_LABEL[handoverShift.to]} 시프트(${SHIFT_WINDOW[handoverShift.to]})`}
            className="inline-flex items-center gap-1.5"
          >
            <span className="px-2.5 py-1 rounded-full bg-[#F2F3F7] text-content-tertiary text-body-base font-bold leading-none">
              {SHIFT_LABEL[handoverShift.from]}
            </span>
            <ChevronRight className="size-3.5 text-content-muted" />
            <span className="px-2.5 py-1 rounded-full bg-brand-surface text-brand-primary text-body-base font-bold leading-none">
              {SHIFT_LABEL[handoverShift.to]}
            </span>
          </span>
        </div>

        <Button
          type="button"
          variant="brandOutline"
          size="default"
          onClick={handleGenerate}
          disabled={generateMutation.isPending || (activeJobId !== null && !progress.done)}
          className="text-[20px] gap-2.5 font-semibold"
        >
          <FileText className="size-5" />
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
        {/* Left Sidebar — 상단 입/퇴원 토글 섹션 + 그 아래 담당 환자 리스트.
            클릭 시 선택만 변경(스크롤 X). 메인은 선택된 환자 1명의 상세를 그림. */}
        <aside className="w-[300px] shrink-0 bg-surface-card border-r border-[#E4E7EE] flex flex-col">
          <SidebarWardEventsToggle
            admissions={wardEventsQuery.data?.admissions ?? []}
            discharges={wardEventsQuery.data?.discharges ?? []}
            isPending={wardEventsQuery.isPending}
            isError={wardEventsQuery.isError}
          />

          <div className="px-5 py-3.5 border-t border-b border-[#E4E7EE] shrink-0 flex items-center justify-between">
            <span className="text-lg font-bold text-content-primary leading-none">
              담당 환자
            </span>
            <span className="text-lg font-semibold text-brand-primary leading-none">
              {rows.length}명
            </span>
          </div>
          <div className="flex-1 min-h-0 overflow-y-scroll">
            {rows.length === 0 ? (
              <div className="text-center py-10 px-6 text-body-sm text-content-tertiary">
                담당 환자가 없습니다. 대시보드에서 담당 환자를 지정하세요.
              </div>
            ) : (
              <ul className="divide-y divide-[#E4E7EE]">
                {rows.map(({ wardPatient, roster }) => (
                  <SidebarPatientRow
                    key={wardPatient.encounterId}
                    wardPatient={wardPatient}
                    roster={roster}
                    isActive={wardPatient.encounterId === selectedEncounterId}
                    onSelect={handleSelectPatient}
                  />
                ))}
              </ul>
            )}
          </div>

          {/* 사이드바 하단 SidebarPatientProfile 제거 — 과거력은 환자 헤더의 중증도 옆에 직접 노출. */}
        </aside>

        {/* Right Content Area — 선택된 환자 1명의 상세. 스크롤바가 화면 오른쪽 끝에 위치. */}
        <div className="flex-1 min-w-0 min-h-0 overflow-y-scroll">
          <div className="px-8 py-6 pb-10">
              {selectedRow ? (
                <PatientHandoverCard
                  key={selectedRow.wardPatient.encounterId}
                  wardPatient={selectedRow.wardPatient}
                  roster={selectedRow.roster}
                  onCitationClick={(citation) =>
                    handleCitationClick(citation, selectedRow.wardPatient)
                  }
                />
              ) : (
                <div className="bg-surface-card rounded-2xl shadow-sm px-8 py-16 flex flex-col items-center gap-3 text-content-tertiary">
                  <Sparkles className="size-6 opacity-50" />
                  <Text className="text-body-sm font-medium">
                    {rows.length === 0
                      ? "담당 환자가 없습니다"
                      : "좌측에서 환자를 선택하세요"}
                  </Text>
                </div>
              )}
          </div>
        </div>
      </main>
    </div>
  );
}

// ---------- Sub Components ----------

// 사이드바 상단 — 오늘의 입원/퇴원 토글 섹션. 각각 독립적으로 펼침/접힘.
// 펼친 상태에서 환자 행: 이름 + 호실/침대 + (시간 의미 있으면) HH:mm.
function SidebarWardEventsToggle({
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
  const [open, setOpen] = useState(false);
  const total = admissions.length + discharges.length;

  if (isPending) {
    return (
      <div className="px-5 py-3 border-b border-[#E4E7EE] flex items-center justify-center text-content-tertiary shrink-0">
        <Loader2 className="size-4 animate-spin" />
      </div>
    );
  }
  if (isError) {
    return (
      <div className="px-5 py-3 border-b border-[#E4E7EE] text-center text-body-sm text-status-danger shrink-0">
        입퇴원 목록을 불러오지 못했습니다
      </div>
    );
  }

  return (
    <div className="shrink-0 border-b border-[#E4E7EE]">
      {/* 단일 "입퇴원" 토글 버튼 */}
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="w-full px-5 py-3 flex items-center gap-2 hover:bg-[#F7F8FB] transition-colors"
      >
        {open ? (
          <ChevronUp className="size-5 text-content-tertiary" />
        ) : (
          <ChevronDown className="size-5 text-content-tertiary" />
        )}
        <span className="text-lg font-bold text-content-primary leading-none">
          입퇴원 환자
        </span>
      </button>

      {/* 펼친 상태 — 입원/퇴원 구분 섹션 */}
      {open && (
        <div className="px-5 py-3 flex flex-col gap-5">
          {/* 입원 */}
          <div>
            <p className="text-body-base font-bold text-content-secondary mb-3 leading-none">
              입원 {admissions.length}
            </p>
            {admissions.length === 0 ? (
              <p className="text-body-base text-content-tertiary">오늘 입원한 환자가 없습니다</p>
            ) : (
              <ul className="flex flex-col gap-1">
                {admissions.map((item) => {
                  const roomBed = formatRoomBed(item.roomName, item.bedName);
                  const showTime = hasMeaningfulTime(item.periodStart);
                  return (
                    <li key={item.encounterId} className="flex items-center gap-2 text-body-sm">
                      <span className="font-semibold text-[#1A1F36] truncate flex-1 min-w-0">
                        {item.patientName}
                      </span>
                      {roomBed && (
                        <span className="text-body-micro font-semibold text-[#5A6478] tabular-nums shrink-0">
                          {roomBed}
                        </span>
                      )}
                      {showTime && (
                        <span className="text-body-micro font-semibold text-content-tertiary tabular-nums shrink-0">
                          {formatHHmm(item.periodStart)}
                        </span>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
          {/* 퇴원 */}
          <div>
            <p className="text-body-base font-bold text-content-secondary mb-3 leading-none">
              퇴원 {discharges.length}
            </p>
            {discharges.length === 0 ? (
              <p className="text-body-base text-content-tertiary">오늘 퇴원한 환자가 없습니다</p>
            ) : (
              <ul className="flex flex-col gap-1">
                {discharges.map((item) => {
                  const roomBed = formatRoomBed(item.roomName, item.bedName);
                  const showTime = hasMeaningfulTime(item.periodEnd);
                  return (
                    <li key={item.encounterId} className="flex items-center gap-2 text-body-sm">
                      <span className="font-semibold text-[#1A1F36] truncate flex-1 min-w-0">
                        {item.patientName}
                      </span>
                      {roomBed && (
                        <span className="text-body-micro font-semibold text-[#5A6478] tabular-nums shrink-0">
                          {roomBed}
                        </span>
                      )}
                      {showTime && (
                        <span className="text-body-micro font-semibold text-content-tertiary tabular-nums shrink-0">
                          {formatHHmm(item.periodEnd)}
                        </span>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      )}
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

// 중증도 — HandoverPayload.illness_severity (stable/watcher/unstable).
// "중증도 안정" 형태로 환자명 행에 노출. border/배경 없이 텍스트 색만 톤 표현.
const SEVERITY_CHIP: Record<SeverityFlag, { label: string; textClass: string }> = {
  stable:   { label: "안정",   textClass: "text-status-success" },
  watcher:  { label: "관찰",   textClass: "text-status-warning-strong" },
  unstable: { label: "불안정", textClass: "text-status-danger-strong" },
};

function SeverityChip({ flag }: { flag: SeverityFlag }) {
  const tone = SEVERITY_CHIP[flag];
  return (
    <span className="inline-flex items-baseline gap-1.5 text-[22px] leading-none">
      <span className="font-semibold text-content-tertiary">중증도</span>
      <span className={cn("font-bold", tone.textClass)}>{tone.label}</span>
    </span>
  );
}

// 환자 헤더의 중증도 옆 과거력 라인 — "과거력 당뇨 8년 · 고혈압 12년" 형식 한 줄 텍스트.
// 0건이면 null. SeverityChip 과 같은 텍스트 톤(text-[22px] inline-baseline)으로 자연스럽게 어울리도록.
function ComorbidityLine({ payload }: { payload: HandoverPayload }) {
  const comorbidities = useMemo(() => extractComorbidities(payload), [payload]);
  if (comorbidities.length === 0) return null;
  const text = comorbidities.join(" · ");
  return (
    <span
      className="inline-flex items-baseline gap-2 text-[22px] leading-none min-w-0"
      title={text}
    >
      <span className="font-semibold text-content-tertiary shrink-0">과거력</span>
      <span className="font-semibold text-[#2270c9] truncate">{text}</span>
    </span>
  );
}

// 위험 항목 카운트 집계 — payload.slots 중 synthesis 제외, severity_flag 가 unstable/watcher 인 항목만.
// 사이드바 행과 환자 헤더(PatientAlertSummary) 양쪽에서 재사용.
function countRisksInPayload(payload: HandoverPayload): { unstable: number; watcher: number } {
  let unstable = 0;
  let watcher = 0;
  (Object.entries(payload.slots) as Array<[keyof Slots, Slot]>).forEach(([slotKey, slot]) => {
    if (slotKey === "synthesis") return;
    slot.items.forEach((item) => {
      if (item.severity_flag === "unstable") unstable++;
      else if (item.severity_flag === "watcher") watcher++;
    });
  });
  return { unstable, watcher };
}

// 입원/POD/HD 컨텍스트 텍스트 패턴 — 사이드바 패널 섹션 C 와 과거력 추출에서 제외 필터로 함께 사용.
const ADMISSION_CONTEXT_PATTERN = /(입원|POD\d+|HD\d+)/;

// 과거력(만성 질환) 추출 — background 슬롯에서 kind === "comorbidity" 또는 텍스트에 "과거력" 포함 항목 추출.
// prefix("과거력:", "과거력 ") 제거 후 trim. 입원/POD/HD 컨텍스트 패턴은 과거력 칩에서 제외.
function extractComorbidities(payload: HandoverPayload): string[] {
  return payload.slots.background.items
    .map((item) => {
      const isComorbidity =
        item.kind === "comorbidity" ||
        (item.value ?? "").includes("과거력") ||
        (item.quote ?? "").includes("과거력");
      if (!isComorbidity) return null;
      const text = item.value?.trim() || item.quote?.trim() || item.kind?.trim() || null;
      if (!text) return null;
      // "과거력: X" / "과거력 X" prefix 제거.
      const cleaned = text.replace(/^과거력\s*[:·]?\s*/, "").trim();
      return cleaned || null;
    })
    .filter((text): text is string => !!text)
    .filter((text) => !ADMISSION_CONTEXT_PATTERN.test(text));
}

// 사이드바 환자 리스트 행 우측 — 위험 항목 카운트 작은 칩. fresh 배지와 동일 패턴(px-1.5 py-0.5 rounded-full).
// 0건 슬롯은 숨김. 둘 다 0건이면 컴포넌트 자체가 null 반환.
function SidebarRiskBadge({ unstable, watcher }: { unstable: number; watcher: number }) {
  if (unstable === 0 && watcher === 0) return null;
  return (
    <span className="flex items-center gap-1 shrink-0">
      {unstable > 0 && (
        <span
          title="즉시 주의 항목"
          className="px-1.5 py-0.5 rounded-full bg-status-danger-surface text-[#a01c1c] text-body-micro font-bold leading-none tabular-nums"
        >
          {unstable}
        </span>
      )}
      {watcher > 0 && (
        <span
          title="관찰 항목"
          className="px-1.5 py-0.5 rounded-full bg-status-warning-surface text-status-warning-strong text-body-micro font-bold leading-none tabular-nums"
        >
          {watcher}
        </span>
      )}
    </span>
  );
}

// 사이드바 환자 행 — 각 행이 자체 useHandoverDetail 호출로 위험 카운트 산출.
// React Query 캐시 키가 useHandoverDetail 과 공유되므로 본문(PatientHandoverCard)과 동일 캐시 재사용.
function SidebarPatientRow({
  wardPatient,
  roster,
  isActive,
  onSelect,
}: {
  wardPatient: WardPatient;
  roster: RosterPatientItem | null;
  isActive: boolean;
  onSelect: (encounterId: number) => void;
}) {
  const birthLabel = formatBirthShort(wardPatient.birthDate);
  const genderLabel = formatGenderShort(wardPatient.gender);
  const roomBed = formatRoomBed(wardPatient.roomName, wardPatient.bedName);

  // roster 가 없으면(리포트 미생성) detail 호출도 안 함 — null 키 시 disabled 처리되는 훅 계약 활용.
  const detailQuery = useHandoverDetail(roster ? String(roster.handover_id) : null);
  const riskCount = useMemo(() => {
    const payload = detailQuery.data?.autoSummaryJson;
    return payload ? countRisksInPayload(payload) : { unstable: 0, watcher: 0 };
  }, [detailQuery.data]);

  return (
    <li>
      <button
        onClick={() => onSelect(wardPatient.encounterId)}
        className={cn(
          "w-full text-left px-4 py-4 flex items-center gap-2.5 transition-colors",
          isActive ? "bg-brand-surface/40" : "hover:bg-[#F7F8FB]",
        )}
      >
        {/* 호실-침대 */}
        <span className="text-body-lg font-bold text-[#36393f] tabular-nums shrink-0 w-[54px] text-right">
          {roomBed}
        </span>
        {/* 환자명 */}
        <span
          className={cn(
            "text-body-base font-bold truncate",
            isActive ? "text-brand-primary" : "text-[#1A1F36]",
          )}
        >
          {wardPatient.name}
        </span>
        {/* 성별/생년 */}
        {birthLabel && (
          <span className="text-body-sm font-semibold text-[#52565e] tabular-nums shrink-0">
            {genderLabel} / {birthLabel}
          </span>
        )}
        {/* 우측 배지 — 위험 카운트만 노출. fresh(리포트 이후 신규 간호기록) 는 제외. */}
        <span className="ml-auto flex items-center gap-1 shrink-0">
          <SidebarRiskBadge unstable={riskCount.unstable} watcher={riskCount.watcher} />
        </span>
      </button>
    </li>
  );
}

// 위험 항목 텍스트 추출 — severity_flag 별로 리스트 분리. 사이드바 패널 토글 노출용.
function extractRiskItems(payload: HandoverPayload): {
  unstable: string[];
  watcher: string[];
} {
  const unstable: string[] = [];
  const watcher: string[] = [];
  (Object.entries(payload.slots) as Array<[keyof Slots, Slot]>).forEach(([slotKey, slot]) => {
    if (slotKey === "synthesis") return;
    slot.items.forEach((item) => {
      const text = item.value?.trim() || item.quote?.trim() || item.kind?.trim();
      if (!text) return;
      if (item.severity_flag === "unstable") unstable.push(text);
      else if (item.severity_flag === "watcher") watcher.push(text);
    });
  });
  return { unstable, watcher };
}

// 사이드바 하단 환자 프로필 패널 — 선택된 환자의 한줄 요약 + 과거력 칩 + 주의 사항 카운트(토글 상세).
// "작년에 OO 앓으셨지" 트리거가 본문 background 카드 안에 묻혀버리는 문제 해소.
function SidebarPatientProfile({
  wardPatient,
  roster,
}: {
  wardPatient: WardPatient | null;
  roster: RosterPatientItem | null;
}) {
  const detailQuery = useHandoverDetail(roster ? String(roster.handover_id) : null);
  const payload = detailQuery.data?.autoSummaryJson ?? null;

  // 어떤 위험 카테고리 상세가 펼쳐져 있는지 — "unstable" / "watcher" / null.
  const [expanded, setExpanded] = useState<"unstable" | "watcher" | null>(null);

  const { comorbidities, riskCount, riskItems } = useMemo(() => {
    if (!payload) {
      return {
        comorbidities: [] as string[],
        riskCount: { unstable: 0, watcher: 0 },
        riskItems: { unstable: [] as string[], watcher: [] as string[] },
      };
    }
    return {
      comorbidities: extractComorbidities(payload),
      riskCount: countRisksInPayload(payload),
      riskItems: extractRiskItems(payload),
    };
  }, [payload]);

  if (!wardPatient) return null;

  const toggle = (key: "unstable" | "watcher") => {
    setExpanded((prev) => (prev === key ? null : key));
  };

  return (
    <div className="shrink-0 border-t border-[#E4E7EE] bg-surface-card max-h-[52%] overflow-y-auto">
      <div className="px-5 py-4 flex flex-col gap-4 pb-5">
        {/* 섹션 A — 환자 이름만 */}
        <div className="flex items-baseline">
          <span className="text-[18px] font-bold text-content-primary leading-none">
            {wardPatient.name}
          </span>
        </div>

        {/* 섹션 B — 과거력 (배경색 없이 텍스트 목록) */}
        <div className="flex flex-col gap-2">
          <span className="text-[18px] font-bold text-content-tertiary tracking-wider">
            과거력
          </span>
          {!payload && roster ? (
            <span className="text-[16px] text-content-tertiary">불러오는 중...</span>
          ) : comorbidities.length === 0 ? (
            <span className="text-[16px] text-content-tertiary">정보 없음</span>
          ) : (
            <ul className="flex flex-col gap-1">
              {comorbidities.map((text, index) => (
                <li
                  key={index}
                  title={text}
                  className="text-[18px] font-semibold text-[#20884c] leading-snug break-words"
                >
                  {text}
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* 섹션 C — 주의 사항 카운트 (클릭 시 상세 토글) */}
        {(riskCount.unstable > 0 || riskCount.watcher > 0) && (
          <div className="flex flex-col gap-2">
            <span className="text-[18px] font-bold text-content-tertiary tracking-wider">
              주의 사항
            </span>
            <div className="flex flex-wrap gap-1.5">
              {riskCount.unstable > 0 && (
                <button
                  type="button"
                  onClick={() => toggle("unstable")}
                  aria-expanded={expanded === "unstable"}
                  className={cn(
                    "inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-md bg-[#f7eaea] text-[#a82525] text-[16px] font-bold leading-none transition-opacity",
                    expanded === "unstable" ? "ring-1 ring-[#7F1D1D]/40" : "hover:opacity-80",
                  )}
                >
                  주의 {riskCount.unstable}
                </button>
              )}
              {riskCount.watcher > 0 && (
                <button
                  type="button"
                  onClick={() => toggle("watcher")}
                  aria-expanded={expanded === "watcher"}
                  className={cn(
                    "inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-md bg-[#fcf3e8] text-[#b8690f] text-[16px] font-bold leading-none transition-opacity",
                    expanded === "watcher" ? "ring-1 ring-status-warning/40" : "hover:opacity-80",
                  )}
                >
                  관찰 {riskCount.watcher}
                </button>
              )}
            </div>
            {/* 토글된 카테고리의 위험 항목 텍스트 리스트 */}
            {expanded && riskItems[expanded].length > 0 && (
              <ul className="flex flex-col gap-4 pt-2">
                {riskItems[expanded].map((text, index) => (
                  <li
                    key={index}
                    className="text-[16px] font-medium text-content-primary leading-snug break-words flex gap-3"
                  >
                    <span
                      className={cn(
                        "mt-[0.55em] size-[5px] rounded-[1px] shrink-0",
                        expanded === "unstable" ? "bg-[#a11d1d]" : "bg-status-warning",
                      )}
                      aria-hidden
                    />
                    <span className="flex-1 min-w-0">{text}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

// 중증도 옆 "주의 사항" 요약 — 위험 항목 텍스트와 동반 과거력 동시 노출.
// SeverityChip 처럼 한 줄 텍스트 패턴이라 환자 헤더 라인에 자연스럽게 어울림.
function PatientAlertSummary({ payload }: { payload: HandoverPayload }) {
  const { riskValues, comorbidities } = useMemo(() => {
    const risk: string[] = [];
    (Object.entries(payload.slots) as Array<[keyof Slots, Slot]>).forEach(([slotKey, slot]) => {
      if (slotKey === "synthesis") return;
      slot.items.forEach((item) => {
        if (item.severity_flag !== "unstable" && item.severity_flag !== "watcher") return;
        const text = item.value?.trim() || item.quote?.trim() || item.kind?.trim();
        if (text) risk.push(text);
      });
    });
    return { riskValues: risk, comorbidities: extractComorbidities(payload) };
  }, [payload]);

  if (riskValues.length === 0 && comorbidities.length === 0) return null;

  return (
    <span
      className="inline-flex items-baseline gap-1.5 text-[18px] leading-none"
      title={
        [
          riskValues.length > 0 ? `주의: ${riskValues.join(" · ")}` : null,
          comorbidities.length > 0 ? `과거력: ${comorbidities.join(" · ")}` : null,
        ]
          .filter(Boolean)
          .join("\n")
      }
    >
      <AlertTriangle className="size-4 text-status-danger-strong self-center" />
      <span className="font-semibold text-content-tertiary">주의 사항</span>
      <span className="font-bold text-status-danger-strong tabular-nums">
        {riskValues.length + comorbidities.length}
      </span>
    </span>
  );
}

// 단일 환자 상세 — 와이어프레임의 메인 영역. 항상 펼친 상태로 PASS-BAR 본문 표시.
function PatientHandoverCard({
  wardPatient,
  roster,
  onCitationClick,
}: {
  wardPatient: WardPatient;
  roster: RosterPatientItem | null;
  onCitationClick: (citation: Citation) => void;
}) {
  const detailQuery = useHandoverDetail(
    roster ? String(roster.handover_id) : null,
  );

  const payload = detailQuery.data?.autoSummaryJson ?? null;
  const illnessSeverity = payload?.illness_severity ?? null;

  return (
    <div className="flex flex-col gap-5">
      {/* [PATIENT SUMMARY] — 와이어프레임 y=78. 카드/박스 없이 본문 위에 한 줄. */}
      <div className="flex items-center gap-3 flex-wrap">
        <h2 className="text-[26px] font-bold text-[#1A1F36] leading-tight tracking-tight">
          {wardPatient.name}
        </h2>
        {wardPatient.birthDate && (
          <span className="text-[22px] font-semibold text-[#454b57] tabular-nums">
            {formatGenderShort(wardPatient.gender)} / {formatBirthShort(wardPatient.birthDate)}
          </span>
        )}
        {illnessSeverity && <SeverityChip flag={illnessSeverity} />}
        {payload && <ComorbidityLine payload={payload} />}
      </div>

      {roster ? (
        <>
          {/* [AI BANNER] — 와이어프레임 y=112, 회색 박스 + brand 라벨 + 본문. */}
          <div className="rounded-lg bg-[#F2F3F7] px-5 py-5 flex items-start gap-3">
            <Sparkles className="size-4 text-brand-primary mt-0.5 shrink-0" />
            <div className="min-w-0 flex-1">
              <p className="text-xl font-bold text-brand-primary leading-none mb-3">
                AI 요약
              </p>
              <p className="text-[18px] font-medium text-[#35373a] leading-relaxed whitespace-pre-wrap">
                {roster.header}
              </p>
            </div>
          </div>

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
              rulesFiredBrief={roster.rules_fired_brief}
              onCitationClick={onCitationClick}
            />
          )}
        </>
      ) : (
        <div className="bg-surface-card rounded-2xl shadow-sm px-6 py-10 flex flex-col items-center gap-2 text-center">
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
  rulesFiredBrief,
  onCitationClick,
}: {
  handoverId: string;
  // payload 는 호출 측에서 null 체크 후 전달 (HandoverPayload 보장).
  payload: HandoverPayload;
  rulesFiredBrief: string[];
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

  // citation → 1개 슬롯 매핑. 한 citation 이 여러 슬롯에 걸리면 ALL_SLOT_ORDER 우선순위(안전 최우선)로 1곳만.
  // 각 SlotCard 가 자기 카드 안에 토글로 노출하므로 중복 방지.
  const { citationsBySlot, otherCitations } = useMemo(() => {
    // 어떤 슬롯에 어떤 citation id 가 인용됐는지 역 매핑.
    const slotsByCid = new Map<string, Set<keyof Slots>>();
    (Object.keys(payload.slots) as Array<keyof Slots>).forEach((slotKey) => {
      payload.slots[slotKey].items.forEach((item) => {
        item.citation_ids.forEach((cid) => {
          if (!visibleCitationIdSet.has(cid)) return;
          if (!slotsByCid.has(cid)) slotsByCid.set(cid, new Set());
          slotsByCid.get(cid)!.add(slotKey);
        });
      });
    });
    const bySlot = new Map<keyof Slots, Citation[]>();
    const other: Citation[] = [];
    visibleCitations.forEach((citation) => {
      const slots = slotsByCid.get(citation.id);
      if (!slots || slots.size === 0) {
        other.push(citation);
        return;
      }
      const hit = ALL_SLOT_ORDER.find((slotKey) => slots.has(slotKey));
      if (!hit) {
        other.push(citation);
        return;
      }
      if (!bySlot.has(hit)) bySlot.set(hit, []);
      bySlot.get(hit)!.push(citation);
    });
    return { citationsBySlot: bySlot, otherCitations: other };
  }, [payload.slots, visibleCitations, visibleCitationIdSet]);

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
    <div className="space-y-6">
      {/* [1] 체크리스트 — synthesis 슬롯 기반. BE 영속. */}
      <ChecklistSection
        items={checklistItems}
        checkedByIndex={checkedByIndex}
        onToggle={handleToggle}
      />

      {/* [2] 위험 신호 — rules_fired_brief 풀폭 콜아웃 (노랑 톤). 안전 슬롯은 아래 7섹션 그리드에 포함. */}
      {rulesFiredBrief.length > 0 && (
        <RulesFiredCallout rules={rulesFiredBrief} />
      )}

      {/* [3] I-PASS-BAR 섹션 헤더 — 7개 슬롯 그리드 영역을 묶는 타이틀. */}
      <div className="flex flex-col gap-4 pt-2">
        <div className="flex items-center gap-6 flex-wrap">
          <h3 className="text-[24px] font-bold text-brand-primary leading-none tracking-tight">
            I-PASS-BAR
          </h3>
          {/* <PassBarLegend /> 약어 가이드 칩 — 잠정 비활성. 필요 시 주석 해제. */}
        </div>
        <div className="h-px bg-[#E4E7EE]" aria-hidden />
      </div>

      {/* [3] 7섹션 그리드 — 1줄: 안전·상황·조치·평가 (4칸), 2줄: 권고·배경·환자문제 (3칸 균등 분할). */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
        {ROW1_SLOT_ORDER.map((key) => (
          <SlotCard
            key={key}
            slotKey={key}
            label={SLOT_LABEL[key]}
            slot={payload.slots[key]}
            accent={SBAR_SLOT_ACCENT[key]}
            citations={citationsBySlot.get(key) ?? []}
            onCitationClick={onCitationClick}
          />
        ))}
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        {ROW2_SLOT_ORDER.map((key) => (
          <SlotCard
            key={key}
            slotKey={key}
            label={SLOT_LABEL[key]}
            slot={payload.slots[key]}
            accent={SBAR_SLOT_ACCENT[key]}
            citations={citationsBySlot.get(key) ?? []}
            onCitationClick={onCitationClick}
          />
        ))}
      </div>

      {/* [4] 어느 슬롯에도 안 매핑된 citation — "기타" 그룹. 그리드 아래 풀폭 카드. */}
      {otherCitations.length > 0 && (
        <OtherCitationsCard
          citations={otherCitations}
          onCitationClick={onCitationClick}
        />
      )}
    </div>
  );
}

// I-PASS-BAR 약어 가이드 — 8개 슬롯의 약어와 라벨을 칩으로 노출. 체크리스트 슬롯(I=종합) + 7개 본문 슬롯.
// 사진 기준 배치: I·P·A·S·S (앞 5개) · B·A·R (뒤 3개) 사이에 가운데점 구분.
const PASS_BAR_CHIPS: Array<{ letter: string; label: string }> = [
  { letter: "I", label: "중증도" },
  { letter: "P", label: "요약" },
  { letter: "A", label: "조치" },
  { letter: "S", label: "상황" },
  { letter: "S", label: "전달" },
];
const PASS_BAR_CHIPS_TAIL: Array<{ letter: string; label: string }> = [
  { letter: "B", label: "배경" },
  { letter: "A", label: "계획" },
  { letter: "R", label: "권고" },
];

function PassBarLegend() {
  return (
    <div className="flex items-center gap-1.5 flex-wrap">
      {PASS_BAR_CHIPS.map((chip, index) => (
        <LegendChip key={`head-${index}`} letter={chip.letter} label={chip.label} />
      ))}
      <span className="size-1 rounded-full bg-content-muted/60 mx-1" aria-hidden />
      {PASS_BAR_CHIPS_TAIL.map((chip, index) => (
        <LegendChip key={`tail-${index}`} letter={chip.letter} label={chip.label} />
      ))}
    </div>
  );
}

function LegendChip({ letter, label }: { letter: string; label: string }) {
  return (
    <span className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-md bg-brand-surface/60 leading-none">
      <span className="text-[15px] font-bold text-brand-primary">{letter}</span>
      <span className="text-[15px] font-semibold text-[#454b57]">{label}</span>
    </span>
  );
}

// 위험 신호 콜아웃 — 와이어프레임 y=268 우측 노랑 톤 (SlotCallout 와 동일 구조의 rules_fired_brief 전용).
function RulesFiredCallout({ rules }: { rules: string[] }) {
  return (
    <div className="rounded-xl bg-surface-card overflow-hidden">
      <div className="flex items-center gap-2 px-4 py-3 bg-[#F5F0E4] text-[#6B4E18]">
        <AlertTriangle className="size-4" />
        <h4 className="text-xl font-bold leading-none">위험 신호</h4>
        <span className="ml-auto text-[18px] font-semibold leading-none">
          {rules.length}건
        </span>
      </div>
      <ul className="px-4 py-3 space-y-1.5">
        {rules.map((rule, index) => (
          <li
            key={index}
            className="flex items-start gap-3 text-[18px] leading-relaxed text-[#6B4E18]"
          >
            <span className="mt-[0.8em] size-[5px] rounded-[1px] bg-[#6B4E18] opacity-60 shrink-0" aria-hidden />
            <span className="flex-1 min-w-0">{rule}</span>
          </li>
        ))}
      </ul>
    </div>
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
    <div className="rounded-xl bg-[#f5f7f1] px-10 py-4 flex flex-col gap-6 mb-8">
      <div className="flex items-center">
        <h4 className="text-[22px] font-bold text-[#4a5748] leading-none">
          체크리스트
        </h4>
        {items.length > 0 && (
          <span className="ml-auto text-[18px] font-semibold text-content-tertiary leading-none tabular-nums">
            {doneCount}/{items.length}
          </span>
        )}
      </div>
      {items.length === 0 ? (
        <p className="text-[18px] text-content-tertiary leading-relaxed">
          등록된 체크 항목이 없습니다.
        </p>
      ) : (
        <ul className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {items.map((item, index) => {
            const kindLabel = item.kind?.trim() || null;
            const bodyLabel =
              item.value?.trim() ||
              item.quote?.trim() ||
              (kindLabel ? null : "(빈 항목)");
            const checked = checkedByIndex[index] === true;
            const checkboxId = `handover-checklist-${index}`;
            return (
              <li key={index}>
                <label
                  htmlFor={checkboxId}
                  className="flex items-center gap-4 px-2.5 py-2 rounded-md bg-white hover:bg-brand-surface/40 cursor-pointer select-none transition-colors"
                >
                  <Checkbox
                    id={checkboxId}
                    checked={checked}
                    onCheckedChange={() => onToggle(index)}
                  />
                  <span
                    className={cn(
                      "text-[18px] leading-snug break-words flex-1 min-w-0",
                      checked && "line-through text-content-muted",
                    )}
                  >
                    {kindLabel && (
                      <span
                        className={cn(
                          "font-bold mr-1.5",
                          checked ? "text-content-muted" : "text-content-primary",
                        )}
                      >
                        {kindLabel} :
                      </span>
                    )}
                    {bodyLabel && (
                      <span
                        className={cn(
                          "font-medium",
                          checked
                            ? "text-content-muted"
                            : kindLabel
                              ? "text-content-secondary"
                              : "text-content-primary",
                        )}
                      >
                        {bodyLabel}
                      </span>
                    )}
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

// 7 슬롯 그리드 카드. border 없는 플랫 디자인. 헤더에 슬롯 라벨 + 항목 N건 + 근거 N건 토글 아이콘.
// 헤더 클릭 시 근거 기록 영역이 본문 아래 펼쳐짐. 본문 항목은 하이픈 불릿으로 정렬.
function SlotCard({
  slotKey,
  label,
  slot,
  accent,
  citations,
  onCitationClick,
}: {
  slotKey: keyof Slots;
  label: string;
  slot: Slot;
  accent: SlotAccent;
  citations: Citation[];
  onCitationClick: (citation: Citation) => void;
}) {
  const tokens = SLOT_ACCENT_CLASS[accent];
  const itemCount = slot.items.length;
  const citationCount = citations.length;
  const [open, setOpen] = useState(false);
  // 헤더 디자인은 citation 유무와 무관하게 동일. 0건이면 클릭만 비활성(disabled) + 근거 카운트만 톤다운.
  const isToggleable = citationCount > 0;

  return (
    <div className="rounded-xl bg-surface-card overflow-hidden flex flex-col">
      <button
        type="button"
        onClick={isToggleable ? () => setOpen((prev) => !prev) : undefined}
        disabled={!isToggleable}
        className={cn(
          "flex items-center gap-2 px-6 py-4 text-left transition-colors disabled:cursor-default",
          tokens.header,
        )}
        aria-expanded={isToggleable ? open : undefined}
        aria-controls={isToggleable ? `slot-citations-${slotKey}` : undefined}
      >
        <h4 className="text-xl font-bold leading-none">{label}</h4>
        {itemCount > 0 && (
          <span className="text-[18px] font-semibold leading-none opacity-80">
            {itemCount}건
          </span>
        )}
        <span
          className={cn(
            "ml-auto flex items-center gap-1.5 text-[16px] font-semibold leading-none",
            isToggleable ? "opacity-90" : "opacity-50",
          )}
        >
          근거 {citationCount}
          {open ? (
            <ChevronUp className="size-3.5" />
          ) : (
            <ChevronDown className="size-3.5" />
          )}
        </span>
      </button>

      <div className="px-5 py-5">
        {itemCount === 0 ? (
          <p className="text-base text-content-tertiary">—</p>
        ) : (
          <ul className="space-y-4">
            {sortItemsBySeverity(slot.items).map((item, index) => (
              <SlotItemRow key={index} item={item} slotKey={slotKey} />
            ))}
          </ul>
        )}
      </div>

      {/* 슬롯 헤더 토글로 펼쳐지는 근거 기록 — 카드 하단에 인라인 렌더 (그리드 자리 그대로 확장). */}
      {open && citationCount > 0 && (
        <div
          id={`slot-citations-${slotKey}`}
          className="px-3.5 pb-3 pt-1 space-y-2 bg-[#ffffff]"
        >
          {citations.map((citation) => (
            <CitationCard
              key={citation.id}
              citation={citation}
              onClick={() => onCitationClick(citation)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

// 어느 슬롯에도 매핑되지 않은 citation 을 담는 풀폭 카드. 그리드 아래에 별도 노출.
function OtherCitationsCard({
  citations,
  onCitationClick,
}: {
  citations: Citation[];
  onCitationClick: (citation: Citation) => void;
}) {
  const [open, setOpen] = useState(false);
  return (
    <div className="rounded-xl bg-surface-card overflow-hidden">
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="w-full flex items-center gap-2 px-3.5 py-2.5 bg-[#F2F3F7] text-content-secondary text-left transition-colors"
        aria-expanded={open}
      >
        <h4 className="text-lg font-bold leading-none">기타</h4>
        <span className="ml-auto flex items-center gap-1.5 text-[13px] font-semibold leading-none">
          근거 {citations.length}
          {open ? (
            <ChevronUp className="size-3.5" />
          ) : (
            <ChevronDown className="size-3.5" />
          )}
        </span>
      </button>
      {open && (
        <div className="px-3.5 py-3 space-y-2 bg-[#F7F8FB]">
          {citations.map((citation) => (
            <CitationCard
              key={citation.id}
              citation={citation}
              onClick={() => onCitationClick(citation)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

// severity_flag → 점/본문 톤 매핑. null 은 매핑 없이 폴백(기존 회색 점 + 본문 primary).
// unstable 은 사이드바 즉시 주의 칩과 같은 어두운 와인 레드(#7F1D1D)로 통일.
const SEVERITY_TONE: Record<"unstable" | "watcher", { dot: string; text: string }> = {
  unstable: { dot: "bg-[#7F1D1D]", text: "text-[#a82222] font-semibold" },
  watcher: { dot: "bg-status-warning", text: "text-status-warning-strong font-semibold" },
};

// 정렬용 — unstable 먼저, watcher, stable, null 순.
const SEVERITY_ORDER: Record<string, number> = {
  unstable: 0,
  watcher: 1,
  stable: 2,
};

function sortItemsBySeverity(items: SlotItem[]): SlotItem[] {
  return [...items].sort((a, b) => {
    const aRank = SEVERITY_ORDER[a.severity_flag ?? "stable"] ?? 3;
    const bRank = SEVERITY_ORDER[b.severity_flag ?? "stable"] ?? 3;
    return aRank - bRank;
  });
}

// 슬롯 안 한 줄짜리 항목 — value(요약) + quote(원문 인용 박스).
// severity_flag 가 있으면 bullet 점과 본문 텍스트 톤을 위험 색으로 강조.
// background 슬롯은 사이드바 과거력 패널과 동일한 font-semibold + content-primary 톤으로 통일.
function SlotItemRow({ item, slotKey }: { item: SlotItem; slotKey?: keyof Slots }) {
  const valueText = item.value?.trim() || null;
  const quoteText = item.quote?.trim() || null;
  // value 도 quote 도 없으면 kind 라도 폴백.
  const headline = valueText ?? (quoteText ? null : (item.kind ?? "(빈 항목)"));

  const tone =
    item.severity_flag === "unstable" || item.severity_flag === "watcher"
      ? SEVERITY_TONE[item.severity_flag]
      : null;
  const dotClass = tone?.dot ?? "bg-content-tertiary";
  // background 슬롯 안에서도 "과거력 항목" 만 파란 톤으로 강조. 입원/POD/HD 등 컨텍스트는 기본 톤.
  const isComorbidity =
    slotKey === "background" &&
    (item.kind === "comorbidity" ||
      (item.value ?? "").includes("과거력") ||
      (item.quote ?? "").includes("과거력"));
  const defaultHeadlineClass = isComorbidity
    ? "text-[#2270c9] font-semibold"
    : "text-content-primary font-medium";
  const headlineClass = tone?.text ?? defaultHeadlineClass;

  return (
    <li className="text-[18px] leading-relaxed flex items-start gap-3">
      <span
        className={cn("mt-[0.6em] size-[5px] rounded-[1px] shrink-0", dotClass)}
        aria-hidden
      />
      <div className="flex-1 min-w-0 flex flex-col gap-1.5">
      {headline && (
        <div className="flex flex-wrap items-start gap-1.5">
          <span className={cn("flex-1 min-w-0 break-words", headlineClass)}>
            {headline}
          </span>
        </div>
      )}
      {quoteText && (
        <div className="rounded-md bg-brand-surface/40 px-2 py-1.5">
          <p className="text-[18px] text-content-secondary leading-snug break-words">
            {quoteText}
          </p>
        </div>
      )}

      </div>
    </li>
  );
}

// citation 1건당 단일 카드 — hover 시 배경 진해지고 클릭 시 환자 상세로 이동.
function CitationCard({
  citation,
  onClick,
}: {
  citation: Citation;
  onClick: () => void;
}) {
  const [hovered, setHovered] = useState(false);
  const cardRef = useRef<HTMLDivElement>(null);
  const labelRef = useRef<HTMLSpanElement>(null);
  const excerptRef = useRef<HTMLParagraphElement>(null);
  const [isClamped, setIsClamped] = useState(false);
  const [tooltipPos, setTooltipPos] = useState<{ left: number; top: number; width: number } | null>(null);

  // 실제로 텍스트가 잘렸을 때만 툴팁 노출 — scrollHeight 가 clientHeight 보다 크면 clamp 발동.
  useEffect(() => {
    const checkClamp = () => {
      const labelEl = labelRef.current;
      const excerptEl = excerptRef.current;
      const labelClamped = !!labelEl && labelEl.scrollHeight > labelEl.clientHeight + 1;
      const excerptClamped = !!excerptEl && excerptEl.scrollHeight > excerptEl.clientHeight + 1;
      setIsClamped(labelClamped || excerptClamped);
    };
    checkClamp();
    window.addEventListener("resize", checkClamp);
    return () => window.removeEventListener("resize", checkClamp);
  }, [citation.label, citation.excerpt]);

  // hover 시 카드 위치 계산해서 fixed 툴팁 좌표 설정 — 부모 overflow-hidden 영향 받지 않도록.
  useEffect(() => {
    if (!hovered || !isClamped) return;
    const updatePos = () => {
      const el = cardRef.current;
      if (!el) return;
      const rect = el.getBoundingClientRect();
      setTooltipPos({ left: rect.left, top: rect.bottom + 4, width: rect.width });
    };
    updatePos();
    window.addEventListener("scroll", updatePos, true);
    window.addEventListener("resize", updatePos);
    return () => {
      window.removeEventListener("scroll", updatePos, true);
      window.removeEventListener("resize", updatePos);
    };
  }, [hovered, isClamped]);

  const clampStyle = {
    display: "-webkit-box",
    WebkitLineClamp: 3,
    WebkitBoxOrient: "vertical" as const,
  };

  return (
    <div
      ref={cardRef}
      className="relative"
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      <button
        type="button"
        onClick={onClick}
        className="w-full text-left flex items-center gap-3 px-4 py-3 my-5 rounded-xl bg-[#f0f1f5] hover:bg-[#d3d8e6] transition-colors"
      >
        <div className="flex-1 min-w-0 flex flex-col gap-3">
          <span
            ref={labelRef}
            className="text-[16px] font-medium text-[#373738] leading-snug break-words overflow-hidden"
            style={clampStyle}
          >
            {citation.label}
          </span>
          {citation.excerpt && (
            <p
              ref={excerptRef}
              className="text-[15px] text-content-secondary leading-snug break-words overflow-hidden"
              style={clampStyle}
            >
              {citation.excerpt}
            </p>
          )}
          <span className="flex items-center gap-3 leading-none">
            <span className="text-body-sm tabular-nums font-semibold text-content-tertiary">
              {citation.ts.slice(0, 10).replace(/-/g, ".")}
            </span>
            {hasMeaningfulTime(citation.ts) && (
              <span className="text-body-sm tabular-nums font-semibold text-content-tertiary">
                {citation.ts.slice(11, 16)}
              </span>
            )}
            <ChevronRight className="ml-auto size-5 text-content-primary opacity-60 shrink-0" />
          </span>
        </div>
      </button>

      {/* hover 시 전체 내용 툴팁 — fixed 위치로 띄워 부모 overflow 영향 없음. */}
      {hovered && isClamped && tooltipPos && (
        <div
          className="fixed z-[100] pointer-events-none"
          style={{ left: tooltipPos.left, top: tooltipPos.top, width: tooltipPos.width }}
        >
          <div className="rounded-xl bg-white border border-[#E4E7EE] shadow-xl px-4 py-3 flex flex-col gap-2">
            <p className="text-[16px] font-medium text-[#373738] leading-relaxed break-words whitespace-pre-wrap">
              {citation.label}
            </p>
            {citation.excerpt && (
              <p className="text-[15px] text-content-secondary leading-relaxed break-words whitespace-pre-wrap">
                {citation.excerpt}
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

