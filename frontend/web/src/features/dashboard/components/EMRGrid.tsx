'use client'

import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  Pencil,
} from "lucide-react";
import { useMemo, useState } from "react";
import { format, addDays, subDays } from "date-fns";
import { ko } from "date-fns/locale";
import { useAuthStore } from "@/features/auth/stores/auth";
import { usePatientDetail } from "../hooks/usePatientDetail";
import { useMonthNursingCounts } from "../hooks/useMonthNursingCounts";
import { toIsoDate } from "@/lib/time";
import type { PatientDetailResponse } from "../types/patient-detail";
import {
  calculateAge,
  formatBirthFull,
  formatGenderShort,
} from "@/lib/patient-display";

interface PatientHeader {
  name: string;
  genderAge: string;
  id: string;
  department: string;
  doctor: string;
  date: string;
  cc: string;
  surgeryName: string;
  diseaseName: string;
  roomBed: string;
  birthday: string;
  phone: string;
}

const EMPTY_HEADER: PatientHeader = {
  name: "",
  genderAge: "",
  id: "",
  department: "",
  doctor: "",
  date: "",
  cc: "",
  surgeryName: "",
  diseaseName: "",
  roomBed: "",
  birthday: "",
  phone: "",
};

function formatAdmissionDate(periodStart: string): string {
  if (!periodStart) return "";
  const start = new Date(periodStart);
  if (Number.isNaN(start.getTime())) return periodStart;
  const yyyy = start.getFullYear();
  const mm = String(start.getMonth() + 1).padStart(2, "0");
  const dd = String(start.getDate()).padStart(2, "0");
  const days = Math.floor(
    (Date.now() - start.getTime()) / (1000 * 60 * 60 * 24),
  );
  return `${yyyy}.${mm}.${dd} (D+${days})`;
}

function buildHeaderFromApi(
  detail: PatientDetailResponse | undefined,
): PatientHeader {
  if (!detail) return EMPTY_HEADER;
  return {
    name: detail.name,
    genderAge: `${formatGenderShort(detail.gender)}/${calculateAge(detail.birthDate)}`,
    id: detail.identifierValue,
    department: detail.departmentCode,
    doctor: detail.attendingPhysicianName ?? "",
    date: formatAdmissionDate(detail.periodStart),
    cc: detail.chiefComplaint,
    surgeryName: detail.surgeryName,
    diseaseName: detail.diseaseName,
    roomBed: [detail.roomName?.replace(/호$/, ""), detail.bedName]
      .filter(Boolean)
      .join("-"),
    birthday: formatBirthFull(detail.birthDate),
    phone: detail.phone,
  };
}

import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { NursingTab } from "./NursingTab";
import { OrderTab } from "./OrderTab";
import { AlertsTab } from "./AlertsTab";

export type EMRTab = "nursing" | "order" | "alerts" | "handover";

interface EMRGridProps {
  patientId: number | null;
  // Controlled — DashboardView 가 보유.
  activeTab: EMRTab;
  onTabChange: (tab: EMRTab) => void;
  selectedDate: Date;
  onChangeSelectedDate: (date: Date) => void;
  // 사이드바의 "확정 전 기록" 항목 클릭 시 NursingTab 이 해당 record 로 자동 스크롤.
  focusRecordId: number | null;
  onFocusHandled?: () => void;
}

export function EMRGrid({
  patientId,
  activeTab,
  onTabChange,
  selectedDate,
  onChangeSelectedDate,
  focusRecordId,
  onFocusHandled,
}: EMRGridProps) {
  const currentUser = useAuthStore((state) => state.user?.name ?? "");
  const patientDetailQuery = usePatientDetail(patientId);

  const [myRecordsOnly, setMyRecordsOnly] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  // 캘린더 popover 가 보고 있는 월 — 사용자가 prev/next 로 넘기면 갱신, 그 월의 카운트만 fetch.
  const [calendarMonth, setCalendarMonth] = useState<Date>(selectedDate);

  const dateIso = useMemo(() => format(selectedDate, "yyyy-MM-dd"), [selectedDate]);
  // 캘린더 셀에 일자별 간호기록 건수 표시. 모바일과 동일하게 28~31× 일자 호출 (캐시 공유로 NursingTab 진입 시 재사용).
  const monthCounts = useMonthNursingCounts(
    patientDetailQuery.data?.encounterId ?? null,
    calendarMonth,
  );

  // 헤더는 API 응답을 그대로 표시한다 (수정 불가).
  const patientInfo: PatientHeader = useMemo(
    () => buildHeaderFromApi(patientDetailQuery.data),
    [patientDetailQuery.data],
  );

  if (patientId === null) {
    return (
      <div className="flex flex-1 items-center justify-center bg-surface-base text-content-muted text-body-base font-medium">
        좌측 환자 목록에서 환자를 선택해주세요
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full overflow-hidden bg-surface-base">
      {/* 1. Patient Info Header - Compact High Density */}
      <div className="shrink-0 bg-action-blue-surface/50 px-5 py-2.5">
        <div className="flex items-center justify-between mb-2">
          {/* Left: Vital Patient Identity */}
          <div className="flex items-center gap-4">
            <div className="flex items-baseline gap-2.5 group/name">
              <span className="text-body-base font-bold text-content-primary tracking-tight leading-none">
                {patientInfo.name}
              </span>
              <div className="flex items-center gap-2 text-body-base tabular-nums font-bold text-content-tertiary">
                <span>{patientInfo.genderAge}</span>
                <span className="text-border-base font-normal">|</span>
                <span>{patientInfo.id}</span>
              </div>
            </div>
          </div>

          {/* Right: Date and Time Selector */}
          <div className="flex items-center gap-2">
            <div className="flex items-center bg-surface-card border border-border-base rounded shadow-sm overflow-hidden">
              <button
                onClick={() => onChangeSelectedDate(subDays(selectedDate, 1))}
                className="p-1.5 hover:bg-surface-hover text-content-muted hover:text-content-primary transition-colors border-r border-border-base"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>

              <Popover>
                <PopoverTrigger asChild>
                  <button
                    type="button"
                    className="flex items-center gap-2 px-3 py-1 cursor-pointer hover:bg-surface-hover transition-colors"
                  >
                    <CalendarIcon className="w-4 h-4 text-brand-primary" />
                    <div className="flex items-baseline gap-1.5">
                      <span className="text-body-base tabular-nums font-bold text-content-primary">
                        {format(selectedDate, "yyyy.MM.dd")}
                      </span>
                      <span className="text-body-sm font-medium text-content-tertiary">
                        (
                        {format(selectedDate, "eee", {
                          locale: ko,
                        })}
                        )
                      </span>
                    </div>
                  </button>
                </PopoverTrigger>
                <PopoverContent
                  className="w-auto p-0"
                  align="center"
                >
                  <Calendar
                    mode="single"
                    selected={selectedDate}
                    month={calendarMonth}
                    onMonthChange={setCalendarMonth}
                    onSelect={(date) => {
                      if (!date) return;
                      onChangeSelectedDate(date);
                    }}
                    locale={ko}
                    formatters={{
                      formatCaption: (date) =>
                        format(date, "yyyy년 M월", { locale: ko }),
                    }}
                    components={{
                      DayButton: ({ day, modifiers, ...buttonProps }) => {
                        const count = monthCounts.get(toIsoDate(day.date)) ?? 0;
                        const isSelected = modifiers.selected ?? false;
                        return (
                          <button
                            {...buttonProps}
                            className="flex flex-col items-center justify-center size-9 p-0 font-normal hover:bg-accent rounded-md transition-colors data-[selected-single=true]:bg-brand-primary data-[selected-single=true]:text-white aria-selected:bg-brand-primary aria-selected:text-white"
                          >
                            <span className="text-body-xs leading-none">
                              {day.date.getDate()}
                            </span>
                            <span
                              className={cn(
                                "text-[9px] font-bold leading-none mt-0.5 h-2.5",
                                count > 0
                                  ? isSelected
                                    ? "text-white"
                                    : "text-brand-primary"
                                  : "opacity-0",
                              )}
                            >
                              {count > 0 ? `${count}건` : "·"}
                            </span>
                          </button>
                        );
                      },
                    }}
                  />
                </PopoverContent>
              </Popover>

              <button
                onClick={() => onChangeSelectedDate(addDays(selectedDate, 1))}
                className="p-1.5 hover:bg-surface-hover text-content-muted hover:text-content-primary transition-colors border-l border-border-base"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        {/* Patient Info Grid - Ultra Dense (마지막 value 컬럼은 1.5fr — 진료일/휴대폰 가독성 확보) */}
        <div className="grid grid-cols-[100px_1fr_100px_1fr_100px_1fr_100px_1.5fr] border border-border-base text-body-sm bg-surface-card rounded overflow-hidden shadow-sm">
          {/* Row 1: Department · Doctor · Birthday (1:1:1) */}
          <div className="col-span-8 grid grid-cols-3 border-b border-border-base">
            <div className="flex items-stretch border-r border-border-base">
              <div className="bg-action-blue-surface/40 border-r border-border-base px-2.5 py-1 font-bold text-sub-primary flex items-center whitespace-nowrap w-[100px] shrink-0">
                진료부서
              </div>
              <div className="px-2.5 py-1 flex items-center flex-1 min-w-0">
                <span className="font-bold text-content-secondary truncate w-full">
                  {patientInfo.department}
                </span>
              </div>
            </div>
            <div className="flex items-stretch border-r border-border-base">
              <div className="bg-action-blue-surface/40 border-r border-border-base px-2.5 py-1 font-bold text-sub-primary flex items-center whitespace-nowrap w-[100px] shrink-0">
                담당의
              </div>
              <div className="px-2.5 py-1 flex items-center flex-1 min-w-0">
                <span className="font-bold text-content-secondary truncate w-full">
                  {patientInfo.doctor}
                </span>
              </div>
            </div>
            <div className="flex items-stretch">
              <div className="bg-action-blue-surface/40 border-r border-border-base px-2.5 py-1 font-bold text-sub-primary flex items-center whitespace-nowrap w-[100px] shrink-0">
                생년월일
              </div>
              <div className="px-2.5 py-1 flex items-center flex-1 min-w-0">
                <span className="tabular-nums font-bold text-content-secondary truncate w-full">
                  {patientInfo.birthday}
                </span>
              </div>
            </div>
          </div>
          {/* Row 2: Room/Bed (read-only) · Admission Date · Phone (1:1:1) — Row 3 와 정렬 */}
          <div className="col-span-8 grid grid-cols-3 border-b border-border-base">
            <div className="flex items-stretch border-r border-border-base">
              <div className="bg-action-blue-surface/40 border-r border-border-base px-2.5 py-1 font-bold text-sub-primary flex items-center whitespace-nowrap w-[100px] shrink-0">
                호실/침대
              </div>
              <div className="px-2.5 py-1 flex items-center flex-1 min-w-0">
                <span className="tabular-nums font-bold text-content-secondary truncate w-full">
                  {patientInfo.roomBed}
                </span>
              </div>
            </div>
            <div className="flex items-stretch border-r border-border-base">
              <div className="bg-action-blue-surface/40 border-r border-border-base px-2.5 py-1 font-bold text-sub-primary flex items-center whitespace-nowrap w-[100px] shrink-0">
                입원일
              </div>
              <div className="px-2.5 py-1 flex items-center flex-1 min-w-0">
                <span className="tabular-nums font-bold text-content-secondary truncate w-full">
                  {patientInfo.date}
                </span>
              </div>
            </div>
            <div className="flex items-stretch">
              <div className="bg-action-blue-surface/40 border-r border-border-base px-2.5 py-1 font-bold text-sub-primary flex items-center whitespace-nowrap w-[100px] shrink-0">
                휴대폰
              </div>
              <div className="px-2.5 py-1 flex items-center flex-1 min-w-0">
                <span className="tabular-nums font-bold text-content-secondary truncate w-full">
                  {patientInfo.phone}
                </span>
              </div>
            </div>
          </div>
          {/* Row 3: Disease Name · CC · Surgery Name (1:1:1) */}
          <div className="col-span-8 grid grid-cols-3">
            <div className="flex items-stretch border-r border-border-base">
              <div className="bg-action-blue-surface/40 border-r border-border-base px-2.5 py-1 font-bold text-sub-primary flex items-center whitespace-nowrap w-[100px] shrink-0">
                병명
              </div>
              <div className="px-2.5 py-1 flex items-center flex-1 min-w-0">
                <span className="font-bold text-content-secondary truncate w-full">
                  {patientInfo.diseaseName}
                </span>
              </div>
            </div>
            <div className="flex items-stretch border-r border-border-base">
              <div className="bg-action-blue-surface/40 border-r border-border-base px-2.5 py-1 font-bold text-sub-primary flex items-center whitespace-nowrap w-[100px] shrink-0">
                주증상
              </div>
              <div className="px-2.5 py-1 flex items-center flex-1 min-w-0">
                <span className="font-bold text-content-secondary truncate w-full">
                  {patientInfo.cc}
                </span>
              </div>
            </div>
            <div className="flex items-stretch">
              <div className="bg-action-blue-surface/40 border-r border-border-base px-2.5 py-1 font-bold text-sub-primary flex items-center whitespace-nowrap w-[100px] shrink-0">
                수술명
              </div>
              <div className="px-2.5 py-1 flex items-center flex-1 min-w-0">
                <span className="font-bold text-content-secondary truncate w-full">
                  {patientInfo.surgeryName}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 2. Nursing Log Workspace Card - Maximized Space */}
      <div className="flex-1 p-1.5 flex flex-col min-h-0">
        {/* Tab Menu - Nursing Record, Doctor Order */}
        <div className="flex items-center gap-1 px-4 mb-1 border-b border-border-base bg-white/50">
          <button
            onClick={() => onTabChange("nursing")}
            className={cn(
              "px-4 py-2 text-body-base font-bold transition-all border-b-2",
              activeTab === "nursing"
                ? "text-brand-primary border-brand-primary"
                : "text-content-muted border-transparent hover:text-content-primary"
            )}
          >
            간호 기록
          </button>
          <button
            onClick={() => onTabChange("order")}
            className={cn(
              "px-4 py-2 text-body-base font-bold transition-all border-b-2",
              activeTab === "order"
                ? "text-brand-primary border-brand-primary"
                : "text-content-muted border-transparent hover:text-content-primary"
            )}
          >
            의사 오더
          </button>
          <button
            onClick={() => onTabChange("alerts")}
            className={cn(
              "px-4 py-2 text-body-base font-bold transition-all border-b-2",
              activeTab === "alerts"
                ? "text-brand-primary border-brand-primary"
                : "text-content-muted border-transparent hover:text-content-primary"
            )}
          >
            환자 호출
          </button>

          {activeTab === "nursing" && (
            <div className="ml-auto flex items-center gap-4">
              <div className="flex items-center gap-2">
                <Checkbox
                  id="my-records-only"
                  checked={myRecordsOnly}
                  onCheckedChange={(checked) => setMyRecordsOnly(checked === true)}
                />
                <label
                  htmlFor="my-records-only"
                  className="cursor-pointer select-none text-body-sm font-semibold text-content-tertiary hover:text-content-primary transition-colors"
                >
                  내 기록만 보기
                </label>
              </div>
              <Button
                type="button"
                variant={isEditMode ? "brand" : "neutral"}
                size="sm"
                className={cn(
                  "h-7 px-2.5 rounded text-body-xs font-bold",
                  isEditMode && "text-white hover:text-white",
                )}
                onClick={() => setIsEditMode((prev) => !prev)}
              >
                <Pencil className="size-3.5" />
                편집
              </Button>
            </div>
          )}

        </div>

        <div className="flex flex-col h-full bg-surface-card border border-border-base rounded-lg shadow-md overflow-hidden relative">
          {activeTab === "nursing" ? (
            <NursingTab
              encounterId={patientDetailQuery.data?.encounterId ?? null}
              date={dateIso}
              currentUser={currentUser}
              myRecordsOnly={myRecordsOnly}
              isEditMode={isEditMode}
              focusRecordId={focusRecordId}
              onFocusHandled={onFocusHandled}
            />
          ) : activeTab === "order" ? (
            <OrderTab
              encounterId={patientDetailQuery.data?.encounterId ?? null}
              date={dateIso}
            />
          ) : activeTab === "alerts" ? (
            <AlertsTab patientId={patientId} date={dateIso} />
          ) : (
            /* AI Handover Placeholder */
            <div className="flex-1 flex flex-col items-center justify-center bg-surface-base/30 space-y-4">
              <div className="size-16 rounded-full bg-brand-surface flex items-center justify-center text-brand-primary">
                <AlertCircle className="size-8" />
              </div>
              <div className="text-center">
                <h3 className="text-title-md font-bold text-sub-primary mb-1">AI 인수인계 준비 중</h3>
                <p className="text-body-sm text-content-muted">더 나은 서비스를 위해 현재 화면을 개발 중입니다.</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
