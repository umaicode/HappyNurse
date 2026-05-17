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
  ChevronUp,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
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

// risk_score (0~1 추정) 에 따른 좌측 도트 색. 와이어프레임 기준 3단계 (red/orange/green).
function getRiskDotClass(score: number | null | undefined): string {
  if (score == null) return "bg-content-muted";
  if (score >= 0.66) return "bg-[#C53030]";
  if (score >= 0.33) return "bg-[#C77700]";
  return "bg-[#2F855A]";
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
          <span className="text-2xl font-semibold text-[#343841] leading-none">
            AI 인수인계
          </span>
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
        {/* Left Sidebar — 상단 입/퇴원 토글 섹션 + 그 아래 담당 환자 리스트.
            클릭 시 선택만 변경(스크롤 X). 메인은 선택된 환자 1명의 상세를 그림. */}
        <aside className="w-[360px] shrink-0 bg-surface-card border-r border-[#E4E7EE] flex flex-col">
          <SidebarWardEventsToggle
            admissions={wardEventsQuery.data?.admissions ?? []}
            discharges={wardEventsQuery.data?.discharges ?? []}
            isPending={wardEventsQuery.isPending}
            isError={wardEventsQuery.isError}
          />

          <div className="px-5 py-3.5 border-t border-b border-[#E4E7EE] shrink-0 flex items-center justify-between">
            <span className="text-body-lg font-bold text-content-primary leading-none">
              담당 환자
            </span>
            <span className="text-body-lg font-semibold text-brand-primary leading-none">
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
                {rows.map(({ wardPatient, roster }) => {
                  const fresh = roster?.freshness?.new_records_since_report ?? 0;
                  const birthLabel = formatBirthShort(wardPatient.birthDate);
                  const genderLabel = formatGenderShort(wardPatient.gender);
                  const roomBed = formatRoomBed(
                    wardPatient.roomName,
                    wardPatient.bedName,
                  );
                  const dotClass = getRiskDotClass(roster?.risk_score);
                  const isActive =
                    wardPatient.encounterId === selectedEncounterId;
                  return (
                    <li key={wardPatient.encounterId}>
                      <button
                        onClick={() => handleSelectPatient(wardPatient.encounterId)}
                        className={cn(
                          "w-full text-left px-4 py-3 flex items-center gap-2.5 transition-colors",
                          isActive
                            ? "bg-brand-surface/60"
                            : "hover:bg-[#F7F8FB]",
                        )}
                      >
                        <span
                          className={cn("size-2 rounded-full shrink-0", dotClass)}
                          aria-hidden
                        />
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
                        {fresh > 0 && (
                          <span
                            title="리포트 이후 신규 간호기록"
                            className="px-1.5 py-0.5 rounded-full bg-status-warning-surface text-status-warning-strong text-body-micro font-bold leading-none shrink-0"
                          >
                            {fresh}
                          </span>
                        )}
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
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
          <ChevronDown className="size-5 text-content-tertiary" />
        ) : (
          <ChevronUp className="size-5 text-content-tertiary" />
        )}
        <span className="text-lg font-bold text-content-primary leading-none">
          입퇴원
        </span>
        <span className="text-lg font-semibold text-brand-primary leading-none">
          {total}
        </span>
      </button>

      {/* 펼친 상태 — 입원/퇴원 구분 섹션 */}
      {open && (
        <div className="px-5 pb-3 flex flex-col gap-5">
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

  return (
    <div className="flex flex-col gap-5">
      {/* [PATIENT SUMMARY] — 와이어프레임 y=78. 카드/박스 없이 본문 위에 한 줄. */}
      <div className="flex items-baseline gap-3 flex-wrap">
        <h2 className="text-[26px] font-bold text-[#1A1F36] leading-tight tracking-tight">
          {wardPatient.name}
        </h2>
        {wardPatient.birthDate && (
          <span className="text-[22px] font-semibold text-[#454b57] tabular-nums">
            {formatGenderShort(wardPatient.gender)} / {formatBirthShort(wardPatient.birthDate)}
          </span>
        )}
      </div>

      {roster ? (
        <>
          {/* [AI BANNER] — 와이어프레임 y=112, 회색 박스 + brand 라벨 + 본문. */}
          <div className="rounded-lg bg-[#F2F3F7] px-4 py-3 flex items-start gap-3">
            <Sparkles className="size-4 text-brand-primary mt-0.5 shrink-0" />
            <div className="min-w-0 flex-1">
              <p className="text-xl font-bold text-brand-primary leading-none mb-3">
                AI 요약
              </p>
              <p className="text-[18px] text-[#27292e] leading-relaxed whitespace-pre-wrap">
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
    <div className="space-y-4">
      {/* [1] 체크리스트 — synthesis 슬롯 기반. BE 영속. */}
      <ChecklistSection
        items={checklistItems}
        checkedByIndex={checkedByIndex}
        onToggle={handleToggle}
      />

      {/* [2] Safety + 위험 신호 — 와이어프레임 y=268 의 2분할 행 (빨강/노랑 톤). */}
      {(payload.slots.safety.items.length > 0 || rulesFiredBrief.length > 0) && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {payload.slots.safety.items.length > 0 ? (
            <SlotCallout
              label="안전"
              slot={payload.slots.safety}
              accent="safety"
            />
          ) : (
            <div />
          )}
          {rulesFiredBrief.length > 0 && (
            <RulesFiredCallout rules={rulesFiredBrief} />
          )}
        </div>
      )}

      {/* [3] SBAR grid — synthesis/safety 제외 6 슬롯 */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
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
    </div>
  );
}

// 위험 신호 콜아웃 — 와이어프레임 y=268 우측 노랑 톤 (SlotCallout 와 동일 구조의 rules_fired_brief 전용).
function RulesFiredCallout({ rules }: { rules: string[] }) {
  return (
    <div className="rounded-xl border border-border-subtle bg-surface-card overflow-hidden">
      <div className="flex items-center gap-2 px-4 py-3 border-b border-border-subtle bg-[#F5F0E4] text-[#6B4E18]">
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
            className="flex gap-2.5 text-[18px] leading-relaxed text-[#6B4E18]"
          >
            <span className="mt-2 size-1.5 rounded-full bg-[#C77700] shrink-0" />
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
    <div className="rounded-xl border border-brand-primary/20 bg-brand-surface/20 p-4 flex flex-col gap-2.5">
      <div className="flex items-center gap-2">
        <h4 className="text-xl font-bold text-brand-primary leading-none">
          체크리스트
        </h4>
        {items.length > 0 && (
          <span className="text-base text-content-tertiary leading-none">
            ({doneCount}/{items.length})
          </span>
        )}
      </div>
      {items.length === 0 ? (
        <p className="text-[18px] text-content-tertiary leading-relaxed">
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
                      "text-[18px] leading-snug break-words flex-1 min-w-0",
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
        <h4 className="text-xl font-bold leading-none">{label}</h4>
        {itemCount > 0 && (
          <span className="ml-auto text-[18px] font-semibold leading-none">
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
        <h4 className="text-xl font-bold leading-none">{label}</h4>
        {itemCount > 0 && (
          <span className="ml-auto text-[17px] font-semibold leading-none">
            {itemCount}건
          </span>
        )}
      </div>
      <div className="px-3.5 py-3">
        {itemCount === 0 ? (
          <p className="text-base text-content-tertiary">—</p>
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
    <li className="text-[18px] leading-relaxed flex flex-col gap-1.5">
      {headline && (
        <div className="flex flex-wrap items-start gap-1.5">
          <span className="text-content-primary flex-1 min-w-0 break-words">
            {headline}
          </span>
        </div>
      )}
      {quoteText && (
        <div className="rounded-md bg-brand-surface/40 px-2 py-1.5">
          <p className="text-[18px] text-content-secondary leading-snug break-words">
            "{quoteText}"
          </p>
        </div>
      )}

      {metaParts.length > 0 && (
        <div className="text-base text-content-secondary">
          {metaParts.join(" · ")}
        </div>
      )}

      {/* contingency — "if X then Y" 형식의 조건부 조치. 안전 카드에서 특히 중요. */}
      {item.contingency && (
        <div className="text-base text-status-warning-strong leading-snug">
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
        className="w-full flex items-center gap-2 text-xl font-bold text-[#414146] hover:bg-brand-surface/40 transition-colors px-2 py-2 -mx-2 rounded-md"
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
                          <span className="text-[18px] font-bold text-brand-primary leading-none">
                            {citation.label}
                          </span>
                          {/* 내용 — 어느 슬롯의 근거인지, 가장 또렷한 톤 */}
                          {referencedSlotLabels.length > 0 && (
                            <span className="text-[18px] font-semibold text-content-primary leading-none">
                              {referencedSlotLabels.join(", ")}
                            </span>
                          )}
                          {/* 날짜 · 시간 — 우측 정렬, 자릿수 고정. 폰트 무게/색 통일. 시간부 00:00:00 이면 숨김. */}
                          <span className="ml-auto flex items-baseline gap-2 leading-none">
                            <span className="text-[18px] tabular-nums font-semibold text-[#69686b]">
                              {citation.ts.slice(0, 10).replace(/-/g, ".")}
                            </span>
                            {hasMeaningfulTime(citation.ts) && (
                              <span className="text-[18px] tabular-nums font-semibold text-[#69686b]">
                                {citation.ts.slice(11, 16)}
                              </span>
                            )}
                          </span>
                        </div>
                        {/* 본문 발췌 — line_range 구간 원본 텍스트. 카드 한 줄 미리보기, 자세한 본문은 hover popover. */}
                        {citation.excerpt && (
                          <p className="text-[17px] text-content-secondary leading-snug line-clamp-1 break-words">
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
