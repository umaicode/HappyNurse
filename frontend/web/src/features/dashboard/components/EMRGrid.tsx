'use client'

import {
  Calendar as CalendarIcon,
  ChevronDown,
  Edit2,
  Trash2,
  ChevronLeft,
  ChevronRight,
  Check,
  AlertCircle,
} from "lucide-react";
import * as React from "react";
import { useState, useEffect } from "react";
import { format, addDays, subDays } from "date-fns";
import { ko } from "date-fns/locale";
import {
  HOURS,
  INITIAL_RECORDS,
  INITIAL_ORDERS,
  INITIAL_PATIENT_ALERTS,
  DEFAULT_PATIENT_INFO,
} from "@/mockup/emr-data";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";
import type { NursingRecord } from "../types/record";
import { NursingTab } from "./NursingTab";
import { OrderTab } from "./OrderTab";
import { AlertsTab } from "./AlertsTab";

function SearchableSelect({
  value,
  onSelect,
  options,
  placeholder = "선택하세요",
  searchPlaceholder = "검색...",
  className = "",
  trigger,
  width = "200px",
}: {
  value: string;
  onSelect: (val: string) => void;
  options: string[];
  placeholder?: string;
  searchPlaceholder?: string;
  className?: string;
  trigger?: React.ReactNode;
  width?: string;
}) {
  const [open, setOpen] = React.useState(false);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        {trigger ? (
          <div className="cursor-pointer">{trigger}</div>
        ) : (
          <button
            type="button"
            role="combobox"
            aria-expanded={open}
            className={cn(
              "flex h-9 items-center justify-between gap-2 rounded-md border border-[var(--color-border-base)] bg-[var(--color-surface-card)] px-3 py-2 text-body-sm text-[var(--color-content-primary)] font-bold transition-all outline-none hover:border-[var(--color-border-hover)] focus:border-[var(--color-brand-primary)] focus:ring-2 focus:ring-[var(--color-brand-primary)]/10 min-w-[130px] cursor-pointer shadow-sm",
              className,
            )}
          >
            <span className="truncate">
              {value || placeholder}
            </span>
            <ChevronDown className="size-4 shrink-0 text-[var(--color-content-muted)]" />
          </button>
        )}
      </PopoverTrigger>
      <PopoverContent
        style={{ width }}
        className="p-0 z-[100] shadow-xl border border-[var(--color-border-base)] bg-[var(--color-surface-card)]"
        align="start"
        sideOffset={4}
      >
        <Command className="border-none">
          <CommandInput
            placeholder={searchPlaceholder}
            className="h-9 border-none focus:ring-0"
          />
          <CommandList className="max-h-[300px] overflow-y-auto p-1">
            <CommandEmpty className="py-2 text-body-xs text-center text-content-muted">
              검색 결과가 없습니다.
            </CommandEmpty>
            <CommandGroup>
              {options.map((option) => (
                <CommandItem
                  key={option}
                  value={option}
                  onSelect={(currentValue) => {
                    const selectedOption =
                      options.find(
                        (o) =>
                          o.toLowerCase() ===
                          currentValue.toLowerCase(),
                      ) || currentValue;
                    onSelect(selectedOption);
                    setOpen(false);
                  }}
                  className="flex items-center gap-2 px-2 py-1.5 text-body-sm rounded-sm cursor-pointer data-[selected=true]:bg-[var(--color-brand-surface)] data-[selected=true]:text-[var(--color-brand-primary)] transition-colors"
                >
                  <Check
                    className={cn(
                      "size-4 shrink-0",
                      value === option
                        ? "opacity-100"
                        : "opacity-0",
                    )}
                  />
                  {option}
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}

// Reusable cell for editing patient information inline
function EditableCell({
  value,
  onUpdate,
  multiline = false,
  className = "",
  placeholder = "클릭하여 편집",
  canEdit = true,
}: {
  value: string;
  onUpdate: (val: string) => void;
  multiline?: boolean;
  className?: string;
  placeholder?: string;
  canEdit?: boolean;
}) {
  const [isEditing, setIsEditing] = useState(false);
  const [val, setVal] = useState(value);

  useEffect(() => {
    setVal(value);
  }, [value]);

  const handleBlur = () => {
    setIsEditing(false);
    if (val !== value) onUpdate(val);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !multiline) {
      handleBlur();
    }
  };

  if (isEditing && canEdit) {
    return (
      <div
        className={cn(
          "w-full cursor-text bg-surface-card rounded-[2px] px-1 -mx-1 min-h-[1.5em] flex items-center border border-[var(--color-sub-primary)] shadow-[0_0_0_1px_var(--color-sub-primary)]/10 z-10 relative",
          className,
        )}
      >
        {multiline ? (
          <textarea
            autoFocus
            value={val}
            onChange={(e) => setVal(e.target.value)}
            onBlur={handleBlur}
            style={{
              fontSize: "inherit",
              letterSpacing: "inherit",
              lineHeight: "inherit",
            }}
            className="w-full bg-transparent outline-none resize-none p-0 m-0 block text-inherit"
            rows={2}
          />
        ) : (
          <input
            autoFocus
            value={val}
            onChange={(e) => setVal(e.target.value)}
            onBlur={handleBlur}
            onKeyDown={handleKeyDown}
            style={{
              fontSize: "inherit",
              letterSpacing: "inherit",
              lineHeight: "inherit",
            }}
            className="w-full bg-transparent outline-none p-0 m-0 block text-inherit"
          />
        )}
      </div>
    );
  }

  return (
    <div
      className={cn(
        "w-full rounded-[2px] px-1 -mx-1 transition-colors min-h-[1.5em] flex items-center group/cell border border-transparent",
        canEdit ? "cursor-text hover:bg-surface-hover hover:border-border-subtle" : "cursor-default",
        className,
      )}
      onClick={() => canEdit && setIsEditing(true)}
    >
      {value ? (
        <span
          className={cn(
            "truncate",
            multiline &&
              "whitespace-pre-wrap truncate-none line-clamp-2",
          )}
        >
          {value}
        </span>
      ) : (
        <span className="text-content-muted italic text-body-xs">
          {placeholder}
        </span>
      )}
      {canEdit && <Edit2 className="w-[10px] h-[10px] text-content-muted ml-1 opacity-0 group-hover/cell:opacity-100 shrink-0" />}
    </div>
  );
}

export function EMRGrid() {
  const currentUser = typeof window !== 'undefined' ? localStorage.getItem("currentUser") || "김영희" : "김영희";
  const [activeTab, setActiveTab] = useState<"nursing" | "order" | "alerts" | "handover">("nursing");
  // EMRGrid는 현재 p1(김가민) 단일 환자 화면. patientId가 없거나 "p1"인 기록만 표시.
  const [records, setRecords] = useState<NursingRecord[]>(
    INITIAL_RECORDS.filter((r) => {
      const pid = (r as { patientId?: string }).patientId;
      return !pid || pid === "p1";
    }) as NursingRecord[],
  );
  const [orders] = useState(INITIAL_ORDERS);
  const [selectedTimeHour, setSelectedTimeHour] = useState<number | null>(null);
  const [myRecordsOnly, setMyRecordsOnly] = useState(false);
  const [selectedDate, setSelectedDate] = useState<Date>(
    new Date(2026, 3, 13),
  ); // 2026.04.13

  // Global edit session (enables per-row edit/confirm buttons)
  const [isGlobalEditing, setIsGlobalEditing] = useState(false);
  const [recordsSnapshot, setRecordsSnapshot] = useState<NursingRecord[] | null>(null);

  const [patientInfo, setPatientInfo] = useState(DEFAULT_PATIENT_INFO);

  const handleUpdatePatient = (
    field: keyof typeof patientInfo,
    val: string,
  ) => {
    setPatientInfo((prev) => ({ ...prev, [field]: val }));
  };

  const handleAddRecord = (record: NursingRecord) => {
    setRecords((prev) =>
      [...prev, record].sort((a, b) => a.time.localeCompare(b.time)),
    );
  };

  const handleUpdateRecord = (
    id: number,
    updates: Partial<NursingRecord>,
  ) => {
    setRecords((prev) =>
      prev.map((r) => (r.id === id ? { ...r, ...updates } : r)),
    );
  };

  const handleDeleteRecord = (id: number) => {
    setRecords((prev) => prev.filter((r) => r.id !== id));
  };

  const startGlobalEdit = () => {
    setRecordsSnapshot(JSON.parse(JSON.stringify(records)));
    setIsGlobalEditing(true);
  };

  const saveGlobalEdit = () => {
    setIsGlobalEditing(false);
    setRecordsSnapshot(null);
  };

  const cancelGlobalEdit = () => {
    if (recordsSnapshot) setRecords(recordsSnapshot);
    setIsGlobalEditing(false);
    setRecordsSnapshot(null);
  };

  return (
    <div className="flex flex-col h-full overflow-hidden bg-[var(--color-surface-base)]">
      {/* 1. Patient Info Header - Compact High Density */}
      <div className="shrink-0 bg-[var(--color-action-blue-surface)]/50 px-5 py-2.5">
        <div className="flex items-center justify-between mb-2">
          {/* Left: Vital Patient Identity */}
          <div className="flex items-center gap-4">
            <div className="flex items-baseline gap-2.5 group/name">
              <EditableCell
                value={patientInfo.name}
                onUpdate={(val) =>
                  handleUpdatePatient("name", val)
                }
                className="text-title-lg font-bold text-[var(--color-content-primary)] tracking-tight w-auto min-w-[80px]"
              />
              <div className="flex items-center gap-2 text-body-base font-mono font-bold text-[var(--color-content-tertiary)]">
                <EditableCell
                  value={patientInfo.genderAge}
                  onUpdate={(val) =>
                    handleUpdatePatient("genderAge", val)
                  }
                  className="w-auto min-w-[60px]"
                />
                <span className="text-[var(--color-border-base)] font-normal">
                  |
                </span>
                <EditableCell
                  value={patientInfo.id}
                  onUpdate={(val) =>
                    handleUpdatePatient("id", val)
                  }
                  className="w-auto min-w-[80px]"
                />
              </div>
            </div>
          </div>

          {/* Right: Date and Time Selector */}
          <div className="flex items-center gap-2">
            <div className="flex items-center bg-[var(--color-surface-card)] border border-[var(--color-border-base)] rounded shadow-sm overflow-hidden">
              <button
                onClick={() =>
                  setSelectedDate((prev) => subDays(prev, 1))
                }
                className="p-1.5 hover:bg-[var(--color-surface-hover)] text-[var(--color-content-muted)] hover:text-[var(--color-content-primary)] transition-colors border-r border-[var(--color-border-base)]"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>

              <Popover>
                <PopoverTrigger asChild>
                  <div className="flex items-center gap-2 px-3 py-1 cursor-pointer hover:bg-[var(--color-surface-hover)] transition-colors">
                    <CalendarIcon className="w-4 h-4 text-[var(--color-brand-primary)]" />
                    <div className="flex items-baseline gap-1.5">
                      <span className="text-body-base font-mono font-bold text-[var(--color-content-primary)]">
                        {format(selectedDate, "yyyy.MM.dd")}
                      </span>
                      <span className="text-body-sm font-medium text-[var(--color-content-tertiary)]">
                        (
                        {format(selectedDate, "eee", {
                          locale: ko,
                        })}
                        )
                      </span>
                    </div>
                  </div>
                </PopoverTrigger>
                <PopoverContent
                  className="w-auto p-0"
                  align="end"
                >
                  <Calendar
                    mode="single"
                    selected={selectedDate}
                    onSelect={(date) =>
                      date && setSelectedDate(date)
                    }
                    initialFocus
                  />
                </PopoverContent>
              </Popover>

              <button
                onClick={() =>
                  setSelectedDate((prev) => addDays(prev, 1))
                }
                className="p-1.5 hover:bg-[var(--color-surface-hover)] text-[var(--color-content-muted)] hover:text-[var(--color-content-primary)] transition-colors border-l border-[var(--color-border-base)]"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>

            <SearchableSelect
              value={
                selectedTimeHour === null
                  ? "전체 시간"
                  : `${selectedTimeHour.toString().padStart(2, "0")}시`
              }
              onSelect={(val) => {
                if (val === "전체 시간")
                  setSelectedTimeHour(null);
                else
                  setSelectedTimeHour(
                    parseInt(val.replace("시", ""), 10),
                  );
              }}
              options={HOURS}
              placeholder="전체 시간"
              searchPlaceholder="시간 검색..."
              className="w-[110px] min-w-0"
              width="150px"
            />

            {/* Global Edit Controls */}
            {isGlobalEditing ? (
              <div className="flex items-center gap-1.5">
                <Button
                  variant="brand"
                  className="h-9 px-3 py-2 rounded-md text-sm font-bold shadow-sm"
                  onClick={saveGlobalEdit}
                >
                  저장
                </Button>
                <Button
                  variant="brandOutline"
                  className="h-9 px-3 py-2 rounded-md text-sm font-bold shadow-sm"
                  onClick={cancelGlobalEdit}
                >
                  취소
                </Button>
              </div>
            ) : (
              <Button
                variant="brandOutline"
                className="h-9 px-3 py-2 rounded-md gap-2 text-sm font-bold shadow-sm"
                onClick={startGlobalEdit}
              >
                <Trash2 className="w-4 h-4" />
                삭제
              </Button>
            )}
          </div>
        </div>

        {/* Patient Info Grid - Ultra Dense */}
        <div className="grid grid-cols-[100px_1fr_100px_1fr_100px_1fr_100px_1fr] border border-[var(--color-border-base)] text-body-sm bg-[var(--color-surface-card)] rounded overflow-hidden shadow-sm">
          {/* Row 1 */}
          <div className="bg-[var(--color-action-blue-surface)]/40 border-r border-b border-[var(--color-border-base)] px-2.5 py-1 font-bold text-[var(--color-sub-primary)] flex items-center whitespace-nowrap">
            진료부서
          </div>
          <div className="border-r border-b border-[var(--color-border-base)] px-2.5 py-1 flex items-center min-w-0">
            <EditableCell
              value={patientInfo.department}
              onUpdate={(val) =>
                handleUpdatePatient("department", val)
              }
              className="font-bold text-[var(--color-content-secondary)] truncate w-full"
            />
          </div>
          <div className="bg-[var(--color-action-blue-surface)]/40 border-r border-b border-[var(--color-border-base)] px-2.5 py-1 font-bold text-[var(--color-sub-primary)] flex items-center whitespace-nowrap">
            진료의
          </div>
          <div className="border-r border-b border-[var(--color-border-base)] px-2.5 py-1 flex items-center min-w-0">
            <EditableCell
              value={patientInfo.doctor}
              onUpdate={(val) =>
                handleUpdatePatient("doctor", val)
              }
              className="font-bold text-[var(--color-content-secondary)] truncate w-full"
            />
          </div>
          <div className="bg-[var(--color-action-blue-surface)]/40 border-r border-b border-[var(--color-border-base)] px-2.5 py-1 font-bold text-[var(--color-sub-primary)] flex items-center whitespace-nowrap">
            보험유형
          </div>
          <div className="border-r border-b border-[var(--color-border-base)] px-2.5 py-1 flex items-center min-w-0">
            <EditableCell
              value={patientInfo.insurance}
              onUpdate={(val) =>
                handleUpdatePatient("insurance", val)
              }
              className="font-bold text-[var(--color-content-secondary)] truncate w-full"
            />
          </div>
          <div className="bg-[var(--color-action-blue-surface)]/40 border-r border-b border-[var(--color-border-base)] px-2.5 py-1 font-bold text-[var(--color-sub-primary)] flex items-center whitespace-nowrap">
            진료일(입원)
          </div>
          <div className="border-b border-[var(--color-border-base)] px-2.5 py-1 flex items-center min-w-0">
            <EditableCell
              value={patientInfo.date}
              onUpdate={(val) =>
                handleUpdatePatient("date", val)
              }
              className="font-mono font-bold text-[var(--color-content-secondary)] truncate w-full"
            />
          </div>
          {/* Row 2: CC & Address */}
          <div className="bg-[var(--color-action-blue-surface)]/40 border-r border-b border-[var(--color-border-base)] px-2.5 py-1 font-bold text-[var(--color-sub-primary)] flex items-center whitespace-nowrap">
            주증상(C/C)
          </div>
          <div className="border-r border-b border-[var(--color-border-base)] px-2.5 py-1 col-span-3 flex items-center min-w-0">
            <EditableCell
              value={patientInfo.cc}
              onUpdate={(val) => handleUpdatePatient("cc", val)}
              className="font-bold text-[var(--color-content-secondary)] truncate w-full"
            />
          </div>
          <div className="bg-[var(--color-action-blue-surface)]/40 border-r border-b border-[var(--color-border-base)] px-2.5 py-1 font-bold text-[var(--color-sub-primary)] flex items-center whitespace-nowrap">
            주소
          </div>
          <div className="border-b border-[var(--color-border-base)] px-2.5 py-1 col-span-3 flex items-center min-w-0">
            <EditableCell
              value={patientInfo.address}
              onUpdate={(val) =>
                handleUpdatePatient("address", val)
              }
              className="font-medium text-[var(--color-content-tertiary)] truncate w-full"
            />
          </div>
          {/* Row 3: Patient Memo (Softer Style) */}
          <div className="bg-[var(--color-action-blue-surface)]/40 border-r border-[var(--color-border-base)] px-2.5 py-1 font-bold text-[var(--color-sub-primary)] flex items-center whitespace-nowrap">
            메모
          </div>
          <div className="px-2.5 py-1 col-span-7 flex items-center min-w-0 bg-[var(--color-brand-surface)]/40">
            <EditableCell
              value={patientInfo.memo}
              onUpdate={(val) =>
                handleUpdatePatient("memo", val)
              }
              className="font-bold text-[#1D4ED8] truncate w-full"
              placeholder="중요 메모를 입력하세요"
            />
          </div>
        </div>
      </div>

      {/* 2. Nursing Log Workspace Card - Maximized Space */}
      <div className="flex-1 p-1.5 flex flex-col min-h-0">
        {/* Tab Menu - Nursing Record, Doctor Order */}
        <div className="flex items-center gap-1 px-4 mb-1 border-b border-[var(--color-border-base)] bg-white/50">
          <button
            onClick={() => setActiveTab("nursing")}
            className={cn(
              "px-4 py-2 text-body-base font-bold transition-all border-b-2",
              activeTab === "nursing"
                ? "text-[var(--color-brand-primary)] border-[var(--color-brand-primary)]"
                : "text-[var(--color-content-muted)] border-transparent hover:text-[var(--color-content-primary)]"
            )}
          >
            간호 기록
          </button>
          <button
            onClick={() => setActiveTab("order")}
            className={cn(
              "px-4 py-2 text-body-base font-bold transition-all border-b-2",
              activeTab === "order"
                ? "text-[var(--color-brand-primary)] border-[var(--color-brand-primary)]"
                : "text-[var(--color-content-muted)] border-transparent hover:text-[var(--color-content-primary)]"
            )}
          >
            의사 오더
          </button>
          <button
            onClick={() => setActiveTab("alerts")}
            className={cn(
              "px-4 py-2 text-body-base font-bold transition-all border-b-2",
              activeTab === "alerts"
                ? "text-[var(--color-brand-primary)] border-[var(--color-brand-primary)]"
                : "text-[var(--color-content-muted)] border-transparent hover:text-[var(--color-content-primary)]"
            )}
          >
            알림
          </button>

          {activeTab === "nursing" && (
            <div className="ml-auto flex items-center gap-2">
              <Checkbox
                id="my-records-only"
                checked={myRecordsOnly}
                onCheckedChange={(checked) => setMyRecordsOnly(checked === true)}
              />
              <label
                htmlFor="my-records-only"
                className="cursor-pointer select-none text-body-sm font-semibold text-[var(--color-content-tertiary)] hover:text-[var(--color-content-primary)] transition-colors"
              >
                내 기록만 보기
              </label>
            </div>
          )}
        </div>

        <div className="flex flex-col h-full bg-[var(--color-surface-card)] border border-[var(--color-border-base)] rounded-lg shadow-md overflow-hidden relative">
          {activeTab === "nursing" ? (
            <NursingTab
              records={records}
              currentUser={currentUser}
              myRecordsOnly={myRecordsOnly}
              selectedTimeHour={selectedTimeHour}
              isGlobalEditing={isGlobalEditing}
              patientId={patientInfo.id}
              onAddRecord={handleAddRecord}
              onUpdateRecord={handleUpdateRecord}
              onDeleteRecord={handleDeleteRecord}
            />
          ) : activeTab === "order" ? (
            <OrderTab orders={orders} />
          ) : activeTab === "alerts" ? (
            <AlertsTab alerts={INITIAL_PATIENT_ALERTS} patientId={patientInfo.id} />
          ) : (
            /* AI Handover Placeholder */
            <div className="flex-1 flex flex-col items-center justify-center bg-[var(--color-surface-base)]/30 space-y-4">
              <div className="size-16 rounded-full bg-[var(--color-brand-surface)] flex items-center justify-center text-[var(--color-brand-primary)]">
                <AlertCircle className="size-8" />
              </div>
              <div className="text-center">
                <h3 className="text-title-md font-bold text-[var(--color-sub-primary)] mb-1">AI 인수인계 준비 중</h3>
                <p className="text-body-sm text-[var(--color-content-muted)]">더 나은 서비스를 위해 현재 화면을 개발 중입니다.</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
