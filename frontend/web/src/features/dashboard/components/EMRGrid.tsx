'use client'

import {
  Calendar as CalendarIcon,
  ChevronDown,
  Edit2,
  Trash2,
  ChevronLeft,
  ChevronRight,
  Check,
  Plus,
  AlertCircle,
} from "lucide-react";
import * as React from "react";
import { useState, useRef, useEffect } from "react";
import { format, addDays, subDays } from "date-fns";
import { ko } from "date-fns/locale";
import {
  HOURS,
  INITIAL_RECORDS,
  INITIAL_ORDERS,
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
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";

// NFC 태깅으로 들어온 약물 기록의 구조화된 값
type DrugInfo = {
  code: string;
  name: string;
  dose: string;
  unit: string;
  frequency: string;
  method: string;
};

// 간호 기록 단일 행 타입 (STT 줄글 + NFC 구조화 기록 공용)
type NursingRecord = {
  id: number;
  time: string;
  category: string;
  content: string;
  status: string;
  writer: string;
  isConfirmed: boolean;
  isHandover?: boolean;
  isAISuggested?: boolean;
  patientId?: string;
  source?: string;
  drug?: DrugInfo;
};

function NfcDrugContent({
  drug,
  editable,
  onChange,
}: {
  drug: DrugInfo;
  editable: boolean;
  onChange: (patch: Partial<DrugInfo>) => void;
}) {
  const valueInputBase =
    "bg-white border border-[var(--color-border-base)] rounded px-1.5 py-0.5 shadow-xs focus:outline-none focus:ring-1 focus:ring-[var(--color-brand-primary)]/20 focus:border-[var(--color-brand-primary)] transition-colors";

  const divider = (
    <span className="text-border-base select-none">·</span>
  );

  return (
    <div className="px-1.5 py-1 flex flex-wrap items-baseline gap-x-1 gap-y-1 text-body-sm">
      {editable ? (
        <input
          value={drug.code}
          onChange={(e) => onChange({ code: e.target.value })}
          onClick={(e) => e.stopPropagation()}
          className={cn(
            valueInputBase,
            "w-[96px] text-[14px] font-mono font-bold text-[var(--color-brand-primary)]",
          )}
        />
      ) : (
        <span className="text-[14px] font-mono font-bold text-[var(--color-brand-primary)]">
          {drug.code}
        </span>
      )}
      {editable ? (
        <input
          value={drug.name}
          onChange={(e) => onChange({ name: e.target.value })}
          onClick={(e) => e.stopPropagation()}
          className={cn(
            valueInputBase,
            "min-w-[160px] flex-1 text-[13px] font-bold text-content-primary",
          )}
        />
      ) : (
        <span className="text-[13px] font-bold text-content-primary">
          {drug.name}
        </span>
      )}

      {divider}

      <span className="text-content-muted font-bold">1회 투여량</span>
      {editable ? (
        <input
          value={drug.dose}
          onChange={(e) => onChange({ dose: e.target.value })}
          onClick={(e) => e.stopPropagation()}
          className={cn(
            valueInputBase,
            "w-14 text-content-primary font-semibold",
          )}
        />
      ) : (
        <span className="text-content-primary font-semibold">
          {drug.dose}
        </span>
      )}
      <span className="text-content-muted">{drug.unit}</span>

      {divider}

      <span className="text-content-muted font-bold">횟수</span>
      {editable ? (
        <input
          value={drug.frequency}
          onChange={(e) => onChange({ frequency: e.target.value })}
          onClick={(e) => e.stopPropagation()}
          className={cn(
            valueInputBase,
            "w-10 text-content-primary font-semibold",
          )}
        />
      ) : (
        <span className="text-content-primary font-semibold">
          {drug.frequency}
        </span>
      )}
      <span className="text-content-muted">회</span>

      {divider}

      <span className="text-content-muted font-bold">용법</span>
      {editable ? (
        <input
          value={drug.method}
          onChange={(e) => onChange({ method: e.target.value })}
          onClick={(e) => e.stopPropagation()}
          className={cn(
            valueInputBase,
            "w-16 text-content-primary font-semibold",
          )}
        />
      ) : (
        <span className="text-content-primary font-semibold">
          {drug.method}
        </span>
      )}
    </div>
  );
}

// Mock Medical Dictionary for Quick Edit
const MEDICAL_SUGGESTIONS: Record<string, string[]> = {
  충우염: ["충수염"],
  타이래놀: ["타이레놀"],
  바이탈: ["V/S", "활력징후"],
  도뇨관: ["Foley", "폴리"],
  수액: ["N/S", "D/W", "Fluid"],
  충수염: ["급성 충수염", "Appendicitis"],
};

function WordWithSuggestion({
  word,
  onReplace,
  enabled = true,
}: {
  word: string;
  onReplace: (newWord: string) => void;
  enabled?: boolean;
}) {
  const [open, setOpen] = useState(false);

  const cleanWord = word.replace(/[.,]$/g, "");
  const currentSuggestions = MEDICAL_SUGGESTIONS[cleanWord];

  // 제안이 없거나 편집 불가 상태면 평문으로 렌더
  if (!enabled || !currentSuggestions) return <>{word} </>;

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <span
          onClick={(e) => e.stopPropagation()}
          className="cursor-pointer border-b-[1.5px] border-dotted border-[var(--color-brand-primary)]/40 hover:bg-[var(--color-brand-primary)]/10 text-[var(--color-brand-primary)] transition-all px-0.5 rounded-sm font-bold mx-0.5"
        >
          {word}
        </span>
      </PopoverTrigger>
      <PopoverContent
        className="w-48 p-1.5 z-[120] bg-white border border-border-base shadow-xl rounded-lg"
        onOpenAutoFocus={(e) => e.preventDefault()}
        onMouseDown={(e) => e.stopPropagation()}
        onClick={(e) => e.stopPropagation()}
        data-quick-edit-popover=""
      >
        <div className="text-[10px] font-bold text-content-muted uppercase tracking-wider px-2 py-1 border-b border-border-subtle mb-1 flex items-center justify-between">
          <span>수정 제안 (AI)</span>
          <AlertCircle className="size-3" />
        </div>
        <div className="flex flex-col gap-0.5">
          {currentSuggestions.map((suggestion) => (
            <button
              key={suggestion}
              onMouseDown={(e) => e.preventDefault()}
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                // 현재 단어의 clean 부분을 제안으로 치환. 팝오버는 유지.
                onReplace(word.replace(cleanWord, suggestion));
              }}
              className="flex items-center justify-between w-full px-2 py-1.5 text-body-sm font-bold text-content-secondary hover:bg-[var(--color-brand-surface)] hover:text-[var(--color-brand-primary)] rounded transition-all text-left group"
            >
              <span>{suggestion}</span>
              <Check className="w-3.5 h-3.5 opacity-0 group-hover:opacity-100 transition-opacity" />
            </button>
          ))}
        </div>
      </PopoverContent>
    </Popover>
  );
}

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

function TimePicker({
  value,
  onSelect,
  onClose,
  className = "",
}: {
  value: string;
  onSelect: (val: string) => void;
  onClose?: () => void;
  className?: string;
}) {
  const [open, setOpen] = React.useState(false);
  const [hour, minute] = (value || "00:00").split(":");

  const hours = Array.from({ length: 24 }).map((_, i) =>
    i.toString().padStart(2, "0"),
  );
  const minutes = Array.from({ length: 60 }).map((_, i) =>
    i.toString().padStart(2, "0"),
  );

  const handleOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (!newOpen && onClose) {
      onClose(); // 팝업이 닫힐 때만 정렬 실행
    }
  };

  const handleTimeChange = (
    e: React.MouseEvent,
    newHour: string,
    newMinute: string,
  ) => {
    e.preventDefault();
    e.stopPropagation();
    onSelect(`${newHour}:${newMinute}`);
  };

  return (
    <Popover open={open} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        <div
          onClick={(e) => e.stopPropagation()}
          className={cn(
            "flex items-center bg-[var(--color-surface-card)] px-2 py-1 rounded border border-[var(--color-border-base)] shadow-sm w-[72px] shrink-0 focus-within:ring-2 focus-within:ring-[var(--color-brand-primary)]/10 focus-within:border-[var(--color-brand-primary)] transition-all cursor-pointer",
            className,
          )}
        >
          <input
            type="text"
            value={value}
            onChange={(e) => {
              const val = e.target.value.replace(/[^\d:]/g, "");
              if (val.length <= 5) onSelect(val);
            }}
            onFocus={(e) => {
              e.stopPropagation();
              setOpen(true);
            }}
            className="bg-transparent border-none outline-none w-full text-center text-[13px] font-mono font-bold text-[var(--color-content-primary)] focus:text-[var(--color-brand-primary)] transition-colors cursor-pointer p-0 placeholder:text-[var(--color-content-muted)]"
            placeholder="00:00"
            maxLength={5}
          />
        </div>
      </PopoverTrigger>
      <PopoverContent
        className="w-[140px] p-0 z-[110] bg-[var(--color-surface-card)] border border-[var(--color-border-base)] shadow-xl"
        align="start"
        sideOffset={4}
        onFocusOutside={(e) => e.preventDefault()} // 내부 포커스 유지
      >
        <div
          className="flex h-[200px] divide-x border-border-subtle"
          onClick={(e) => e.stopPropagation()}
        >
          <ScrollArea className="flex-1">
            <div className="flex flex-col p-1">
              <div className="px-2 py-1 text-[10px] font-bold text-content-muted uppercase tracking-wider sticky top-0 bg-white z-10">
                Hour
              </div>
              {hours.map((h) => (
                <button
                  key={h}
                  onClick={(e) =>
                    handleTimeChange(e, h, minute || "00")
                  }
                  className={cn(
                    "px-2 py-1.5 text-sm font-mono rounded-sm text-left transition-colors",
                    hour === h
                      ? "bg-[var(--color-brand-surface)] text-[var(--color-brand-primary)] font-bold"
                      : "hover:bg-surface-hover text-content-secondary",
                  )}
                >
                  {h}
                </button>
              ))}
            </div>
          </ScrollArea>
          <ScrollArea className="flex-1">
            <div className="flex flex-col p-1">
              <div className="px-2 py-1 text-[10px] font-bold text-content-muted uppercase tracking-wider sticky top-0 bg-white z-10">
                Min
              </div>
              {minutes.map((m) => (
                <button
                  key={m}
                  onClick={(e) =>
                    handleTimeChange(e, hour || "00", m)
                  }
                  className={cn(
                    "px-2 py-1.5 text-sm font-mono rounded-sm text-left transition-colors",
                    minute === m
                      ? "bg-[var(--color-brand-surface)] text-[var(--color-brand-primary)] font-bold"
                      : "hover:bg-surface-hover text-content-secondary",
                  )}
                >
                  {m}
                </button>
              ))}
            </div>
          </ScrollArea>
        </div>
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
  const [activeTab, setActiveTab] = useState<"nursing" | "order" | "handover">("nursing");
  // EMRGrid는 현재 p1(김가민) 단일 환자 화면. patientId가 없거나 "p1"인 기록만 표시.
  const [records, setRecords] = useState<NursingRecord[]>(
    INITIAL_RECORDS.filter((r) => {
      const pid = (r as { patientId?: string }).patientId;
      return !pid || pid === "p1";
    }) as NursingRecord[],
  );
  const [orders, setOrders] = useState(INITIAL_ORDERS);
  const [newRecordText, setNewRecordText] = useState("");
  const [newRecordTime, setNewRecordTime] = useState("");
  const [inlineAddIndex, setInlineAddIndex] = useState<
    number | null
  >(null);
  const [selectedTimeHour, setSelectedTimeHour] = useState<
    number | null
  >(null);
  const [myRecordsOnly, setMyRecordsOnly] = useState(false);
  const [selectedDate, setSelectedDate] = useState<Date>(
    new Date(2026, 3, 13),
  ); // 2026.04.13

  // Refs for auto-scroll
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const recordRefs = useRef<{
    [key: number]: HTMLDivElement | null;
  }>({});

  // Per-row inline edit state
  const [editingRecordId, setEditingRecordId] = useState<
    number | null
  >(null);
  // 편집 중인 행의 임시 상태. "완료" 누를 때만 records에 반영된다.
  const [draftRecord, setDraftRecord] = useState<NursingRecord | null>(null);
  // Global edit session (enables per-row edit/confirm buttons)
  const [isGlobalEditing, setIsGlobalEditing] = useState(false);
  const [recordsSnapshot, setRecordsSnapshot] = useState<
    NursingRecord[] | null
  >(null);

  const [patientInfo, setPatientInfo] = useState(
    DEFAULT_PATIENT_INFO,
  );

  const handleUpdateRecord = (
    id: number,
    updates: Partial<NursingRecord>,
  ) => {
    setRecords((prev) =>
      prev.map((r) => (r.id === id ? { ...r, ...updates } : r)),
    );
  };

  // Auto-scroll to bottom logic
  useEffect(() => {
    const scrollToBottom = () => {
      if (activeTab !== "nursing" || !scrollContainerRef.current) return;
      
      const scrollContainer = scrollContainerRef.current;
      scrollContainer.scrollTo({
        top: scrollContainer.scrollHeight,
        behavior: "smooth",
      });
    };

    // Trigger scroll when records change or tab switches to nursing
    const timer = setTimeout(scrollToBottom, 100);
    return () => clearTimeout(timer);
  }, [records.length, activeTab, patientInfo.id]);

  const handleUpdatePatient = (
    field: keyof typeof patientInfo,
    val: string,
  ) => {
    setPatientInfo((prev) => ({ ...prev, [field]: val }));
  };

  const startGlobalEdit = () => {
    setRecordsSnapshot(JSON.parse(JSON.stringify(records)));
    setIsGlobalEditing(true);
  };

  const saveGlobalEdit = () => {
    setIsGlobalEditing(false);
    setRecordsSnapshot(null);
    setEditingRecordId(null);
  };

  const cancelGlobalEdit = () => {
    if (recordsSnapshot) setRecords(recordsSnapshot);
    setIsGlobalEditing(false);
    setRecordsSnapshot(null);
    setEditingRecordId(null);
  };

  const enterEdit = (record: NursingRecord) => {
    setDraftRecord({
      ...record,
      drug: record.drug ? { ...record.drug } : undefined,
    });
    setEditingRecordId(record.id);
  };

  const commitEdit = () => {
    if (draftRecord) {
      handleUpdateRecord(draftRecord.id, draftRecord);
    }
    setDraftRecord(null);
    setEditingRecordId(null);
  };

  const handleConfirmRecord = (id: number) => {
    setRecords((prev) =>
      prev.map((r) =>
        r.id === id ? { ...r, isConfirmed: true } : r,
      ),
    );
  };

  const handleDeleteRecord = (id: number) => {
    if (window.confirm("이 기록을 삭제하시겠습니까?")) {
      setRecords((prev) => prev.filter((r) => r.id !== id));
      if (editingRecordId === id) setEditingRecordId(null);
    }
  };

  const handleAddRecord = () => {
    if (!newRecordText.trim()) return;
    const now = new Date();

    // Use manually entered time or current time
    let time = newRecordTime.trim();
    if (!time || !/^\d{2}:\d{2}$/.test(time)) {
      time = `${now.getHours().toString().padStart(2, "0")}:${now.getMinutes().toString().padStart(2, "0")}`;
    }

    const newRecord = {
      id: Date.now(),
      time,
      category: "간호기록",
      content: newRecordText.trim(),
      status: "completed",
      writer: currentUser,
      isConfirmed: true,
    };

    setRecords((prev) =>
      [...prev, newRecord].sort((a, b) =>
        a.time.localeCompare(b.time),
      ),
    );
    setNewRecordText("");
    setNewRecordTime("");
    setInlineAddIndex(null);
  };

  const handleStartInlineAdd = (index: number) => {
    setInlineAddIndex(index);
    setNewRecordText("");

    if (filteredRecords.length > 0) {
      if (index === 0) {
        const firstTime = filteredRecords[0].time;
        const [h, m] = firstTime.split(":").map(Number);
        let totalMin = h * 60 + m - 5;
        if (totalMin < 0) totalMin = 0;
        setNewRecordTime(
          `${Math.floor(totalMin / 60)
            .toString()
            .padStart(
              2,
              "0",
            )}:${(totalMin % 60).toString().padStart(2, "0")}`,
        );
      } else if (index === filteredRecords.length) {
        const lastTime =
          filteredRecords[filteredRecords.length - 1].time;
        const [h, m] = lastTime.split(":").map(Number);
        let totalMin = h * 60 + m + 5;
        if (totalMin > 1439) totalMin = 1439;
        setNewRecordTime(
          `${Math.floor(totalMin / 60)
            .toString()
            .padStart(
              2,
              "0",
            )}:${(totalMin % 60).toString().padStart(2, "0")}`,
        );
      } else {
        const t1 = filteredRecords[index - 1].time;
        const t2 = filteredRecords[index].time;
        const [h1, m1] = t1.split(":").map(Number);
        const [h2, m2] = t2.split(":").map(Number);
        const total1 = h1 * 60 + m1;
        const total2 = h2 * 60 + m2;
        const mid = Math.floor((total1 + total2) / 2);
        setNewRecordTime(
          `${Math.floor(mid / 60)
            .toString()
            .padStart(
              2,
              "0",
            )}:${(mid % 60).toString().padStart(2, "0")}`,
        );
      }
    } else {
      const now = new Date();
      setNewRecordTime(
        `${now.getHours().toString().padStart(2, "0")}:${now.getMinutes().toString().padStart(2, "0")}`,
      );
    }
  };

  const recordPasses = (record: NursingRecord) => {
    if (selectedTimeHour !== null) {
      const hour = parseInt(record.time.split(":")[0], 10);
      if (hour < selectedTimeHour) return false;
    }
    if (myRecordsOnly && record.writer !== currentUser) return false;
    return true;
  };

  const filteredRecords = records.filter(recordPasses);

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
                <Edit2 className="w-4 h-4" />
                편집
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

          {activeTab === "nursing" && (
            <label className="ml-auto flex items-center gap-2 cursor-pointer select-none text-body-sm font-semibold text-[var(--color-content-tertiary)] hover:text-[var(--color-content-primary)] transition-colors">
              <input
                type="checkbox"
                checked={myRecordsOnly}
                onChange={(e) => setMyRecordsOnly(e.target.checked)}
                className="size-3.5 rounded border-[var(--color-border-base)] accent-[var(--color-brand-primary)] cursor-pointer"
              />
              내 기록만 보기
            </label>
          )}
        </div>

        <div className="flex flex-col h-full bg-[var(--color-surface-card)] border border-[var(--color-border-base)] rounded-lg shadow-md overflow-hidden relative">
          {activeTab === "nursing" ? (
            /* Table Layout - Nursing Records */
            <div
              ref={scrollContainerRef}
              className="flex-1 overflow-auto bg-[var(--color-surface-card)] min-h-0 relative text-body-base"
            >
              <div className="min-w-[800px] flex flex-col h-full">
                {/* Header Row - Solid & Slim */}
                <div className="grid grid-cols-[80px_1fr_110px_140px] gap-4 px-4 py-1.5 bg-[var(--color-surface-hover)] border-b border-[var(--color-border-base)] text-body-sm font-extrabold text-[var(--color-content-secondary)] sticky top-0 z-20 tracking-tight shadow-sm">
                  <div className="border-r border-[var(--color-border-base)] pr-4 text-center">
                    시간
                  </div>
                  <div className="border-r border-[var(--color-border-base)] pr-4">
                    기록 내용
                  </div>
                  <div className="border-r border-[var(--color-border-base)] pr-4 h-full flex items-center justify-center">
                    기록자
                  </div>
                  <div className="text-center"></div>
                </div>

                {/* Records */}
                <div className="flex flex-col flex-1 pb-10">
                  {filteredRecords.map((record, index) => {
                    const isMine = record.writer === currentUser;
                    const canEditThisRow = isMine;
                    const isEditingRow = editingRecordId === record.id && canEditThisRow && !isGlobalEditing;

                    return (
                      <React.Fragment key={record.id}>
                        {/* Plus button between rows */}
                      <div className="relative group/between h-0 z-30">
                        <div
                          className="absolute inset-x-0 -top-2 h-4 flex items-center justify-center opacity-0 group-hover/between:opacity-100 transition-opacity cursor-pointer overflow-visible"
                          onClick={() =>
                            handleStartInlineAdd(index)
                          }
                        >
                          <div className="w-full h-[1px] bg-[var(--color-brand-primary)]/20" />
                          <div className="absolute size-5 rounded-full bg-[var(--color-brand-primary)] text-white flex items-center justify-center shadow-sm hover:scale-110 transition-transform">
                            <Plus className="size-3.5" />
                          </div>
                        </div>
                      </div>

                      {inlineAddIndex === index && (
                        <div className="grid grid-cols-[80px_1fr_110px_140px] gap-4 px-4 py-2 border-y border-[var(--color-brand-primary)]/10 bg-[var(--color-brand-surface)]/30 items-center shadow-inner">
                          {/* Time Column */}
                          <div className="flex justify-center border-r border-brand-primary/10 pr-4">
                            <TimePicker
                              value={newRecordTime}
                              onSelect={setNewRecordTime}
                              className="h-8 w-full bg-white border-brand-primary/10 shadow-xs"
                            />
                          </div>
                          
                          {/* Content Column */}
                          <div className="pr-4 border-r border-brand-primary/10">
                            <textarea
                              autoFocus
                              placeholder="새로운 간호 기록을 입력하세요..."
                              value={newRecordText}
                              onChange={(e) =>
                                setNewRecordText(
                                  e.target.value,
                                )
                              }
                              className="w-full bg-white border border-brand-primary/10 rounded px-2 py-1.5 text-body-sm min-h-[40px] focus:outline-none focus:ring-1 focus:ring-brand-primary/20 transition-all resize-none shadow-xs"
                              rows={1}
                            />
                          </div>

                          {/* Writer Column - Perfectly Aligned */}
                          <div className="text-body-sm text-[var(--color-content-tertiary)] font-bold truncate h-full border-r border-brand-primary/10 pr-4 flex items-center justify-center">
                            <div className="flex items-center justify-center w-full gap-1 px-1.5 py-1">
                              <span className="truncate">{currentUser}</span>
                            </div>
                          </div>

                          {/* Actions Column */}
                          <div className="flex gap-1.5 justify-center">
                            <button
                              onClick={handleAddRecord}
                              disabled={!newRecordText.trim()}
                              className="px-3 py-1.5 bg-[var(--color-brand-primary)] text-white text-[11px] font-bold rounded shadow-sm hover:bg-[var(--color-brand-hover)] disabled:opacity-50 transition-colors whitespace-nowrap"
                            >
                              추가
                            </button>
                            <button
                              onClick={() =>
                                setInlineAddIndex(null)
                              }
                              className="px-3 py-1.5 bg-white border border-border-base text-[11px] font-bold rounded shadow-sm hover:bg-surface-hover transition-colors whitespace-nowrap"
                            >
                              취소
                            </button>
                          </div>
                        </div>
                      )}

                      <div
                        ref={(el) => {
                          recordRefs.current[record.id] = el;
                        }}
                        className={cn(
                          "grid grid-cols-[80px_1fr_110px_140px] gap-4 px-4 py-1 border-b border-[var(--color-border-base)]/50 items-start hover:bg-[var(--color-surface-hover)]/40 transition-all group relative",
                          isEditingRow &&
                            "bg-[var(--color-brand-surface)]/20 hover:bg-[var(--color-brand-surface)]/20 shadow-inner",
                          !record.isConfirmed &&
                            "before:absolute before:left-0 before:top-0 before:bottom-0 before:w-[4px] before:bg-[var(--color-brand-primary)]/30",
                        )}
                      >
                        {/* Content Column */}
                        <div className="pt-0.5 border-r border-[var(--color-border-base)]/50 pr-4 min-w-0">
                          {isEditingRow ? (
                            <TimePicker
                              value={draftRecord?.time ?? record.time}
                              onSelect={(newTime) => {
                                if (draftRecord)
                                  setDraftRecord({
                                    ...draftRecord,
                                    time: newTime,
                                  });
                              }}
                              className="w-full border-transparent bg-surface-base/50 shadow-none px-1 h-7 hover:bg-white hover:border-border-subtle transition-all"
                            />
                          ) : (
                            <div className="w-full text-center font-mono font-bold text-[var(--color-content-primary)] h-7 flex items-center justify-center text-[13px]">
                              {record.time}
                            </div>
                          )}
                        </div>

                        {/* Content Column */}
                        <div className="min-w-0 pr-6 border-r border-[var(--color-border-base)]/50 py-1.5 relative">
                          {(record as { source?: string }).source === "nfc" &&
                          (record as { drug?: DrugInfo }).drug ? (
                            <NfcDrugContent
                              drug={
                                (isEditingRow && draftRecord?.drug
                                  ? draftRecord.drug
                                  : (record as { drug?: DrugInfo }).drug) as DrugInfo
                              }
                              editable={isEditingRow}
                              onChange={(patch) => {
                                if (!draftRecord?.drug) return;
                                setDraftRecord({
                                  ...draftRecord,
                                  drug: { ...draftRecord.drug, ...patch },
                                });
                              }}
                            />
                          ) : isEditingRow ? (
                            <div className="relative px-1.5 py-1 bg-surface-base/30 rounded focus-within:bg-white focus-within:ring-1 focus-within:ring-brand-primary/20 transition-all">
                              <textarea
                                autoFocus={
                                  editingRecordId === record.id
                                }
                                ref={(el) => {
                                  if (el) {
                                    el.style.height = "auto";
                                    el.style.height = `${el.scrollHeight}px`;
                                  }
                                }}
                                value={draftRecord?.content ?? record.content}
                                onChange={(e) => {
                                  if (draftRecord)
                                    setDraftRecord({
                                      ...draftRecord,
                                      content: e.target.value,
                                    });
                                  e.target.style.height = "auto";
                                  e.target.style.height = `${e.target.scrollHeight}px`;
                                }}
                                className="w-full bg-transparent text-body-sm leading-[1.6] text-content-primary resize-none outline-none overflow-hidden block p-0 m-0 min-h-[1.6em]"
                                rows={1}
                              />
                            </div>
                          ) : (
                            <div className="relative text-body-sm leading-[1.6] text-content-secondary whitespace-pre-wrap px-1.5 py-1 min-h-[1.6em] break-all">
                              {record.content
                                .split(" ")
                                .map((word, wordIdx) => (
                                  <React.Fragment key={wordIdx}>
                                    <WordWithSuggestion
                                      word={word}
                                      enabled={!record.isConfirmed && isMine}
                                      onReplace={(newWord) => {
                                        const words = record.content.split(" ");
                                        words[wordIdx] = newWord;
                                        handleUpdateRecord(record.id, { content: words.join(" ") });
                                      }}
                                    />
                                  </React.Fragment>
                                ))}
                            </div>
                          )}                        </div>

                        {/* Writer Column — 로그인 사용자 기준 자동 기입, 수정 불가 */}
                        <div className="text-body-sm text-[var(--color-content-tertiary)] font-bold pt-1.5 truncate h-full border-r border-[var(--color-border-base)]/50 pr-4 flex items-center justify-center">
                          <div className="flex items-center justify-center w-full gap-1 px-1.5 py-1">
                            <span className="truncate">
                              {record.writer}
                            </span>
                          </div>
                        </div>

                        {/* Actions Column */}
                        <div className="pt-1 h-full flex items-center justify-center gap-1.5">
                          {isMine ? (
                            canEditThisRow && (
                              <>
                                {isGlobalEditing ? (
                                  <Button
                                    variant="brandOutline"
                                    size="sm"
                                    className="h-7 px-2.5 rounded text-[12px]"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleDeleteRecord(record.id);
                                    }}
                                  >
                                    삭제
                                  </Button>
                                ) : (
                                  <Button
                                    variant="brandOutline"
                                    size="sm"
                                    className="h-7 px-2.5 rounded text-[12px]"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      if (editingRecordId === record.id) {
                                        commitEdit();
                                      } else {
                                        enterEdit(record);
                                      }
                                    }}
                                  >
                                    {editingRecordId === record.id
                                      ? "완료"
                                      : "수정"}
                                  </Button>
                                )}
                                {!record.isConfirmed && !isGlobalEditing && (
                                  <Button
                                    variant="brand"
                                    size="sm"
                                    className="h-7 px-2.5 rounded text-[12px]"
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      // draft가 있으면 같이 커밋 + 확정 처리
                                      if (
                                        editingRecordId === record.id &&
                                        draftRecord
                                      ) {
                                        handleUpdateRecord(record.id, {
                                          ...draftRecord,
                                          isConfirmed: true,
                                        });
                                        setDraftRecord(null);
                                        setEditingRecordId(null);
                                      } else {
                                        handleConfirmRecord(record.id);
                                      }
                                    }}
                                  >
                                    확정
                                  </Button>
                                )}
                              </>
                            )
                          ) : (
                            !record.isConfirmed && (
                              <div className="px-3 py-1.5 text-[11px] font-bold text-content-muted bg-slate-100 border border-border-subtle rounded-md whitespace-nowrap opacity-70 cursor-not-allowed">
                                확정 대기
                              </div>
                            )
                          )}
                        </div>
                      </div>
                    </React.Fragment>
                  );
                })}

                  <div className="relative group/between h-0 z-30">
                    <div
                      className="absolute inset-x-0 -top-2 h-4 flex items-center justify-center opacity-0 group-hover/between:opacity-100 transition-opacity cursor-pointer overflow-visible"
                      onClick={() =>
                        handleStartInlineAdd(
                          filteredRecords.length,
                        )
                      }
                    >
                      <div className="w-full h-[1px] bg-[var(--color-brand-primary)]/20" />
                      <div className="absolute size-5 rounded-full bg-[var(--color-brand-primary)] text-white flex items-center justify-center shadow-sm hover:scale-110 transition-transform">
                        <Plus className="size-3.5" />
                      </div>
                    </div>
                  </div>

                  {inlineAddIndex === filteredRecords.length && (
                    <div className="grid grid-cols-[80px_1fr_110px_140px] gap-4 px-4 py-2 border-y border-[var(--color-brand-primary)]/10 bg-[var(--color-brand-surface)]/30 items-center shadow-inner">
                      <div className="flex justify-center border-r border-brand-primary/10 pr-4">
                        <TimePicker
                          value={newRecordTime}
                          onSelect={setNewRecordTime}
                          className="h-8 w-full bg-white border-brand-primary/10 shadow-xs"
                        />
                      </div>
                      <div className="pr-4 border-r border-brand-primary/10">
                        <textarea
                          autoFocus
                          placeholder="새로운 기록을 입력하세요..."
                          value={newRecordText}
                          onChange={(e) =>
                            setNewRecordText(
                              e.target.value,
                            )
                          }
                          className="w-full bg-white border border-brand-primary/10 rounded px-2 py-1.5 text-body-sm min-h-[40px] focus:outline-none focus:ring-1 focus:ring-brand-primary/20 transition-all resize-none shadow-xs"
                          rows={1}
                        />
                      </div>
                      <div className="text-body-sm text-[var(--color-content-tertiary)] font-bold truncate h-full border-r border-brand-primary/10 pr-4 flex items-center justify-center">
                        <div className="flex items-center justify-center w-full gap-1 px-1.5 py-1">
                          <span className="truncate">{currentUser}</span>
                        </div>
                      </div>
                      <div className="flex gap-1.5 justify-center">
                        <button
                          onClick={handleAddRecord}
                          disabled={!newRecordText.trim()}
                          className="px-3 py-1.5 bg-[var(--color-brand-primary)] text-white text-[11px] font-bold rounded shadow-sm hover:bg-[var(--color-brand-hover)] disabled:opacity-50 transition-colors whitespace-nowrap"
                        >
                          추가
                        </button>
                        <button
                          onClick={() => setInlineAddIndex(null)}
                          className="px-3 py-1.5 bg-white border border-border-base text-[11px] font-bold rounded shadow-sm hover:bg-surface-hover transition-colors whitespace-nowrap"
                        >
                          취소
                        </button>
                      </div>
                    </div>
                  )}

                  <div className="h-20" />
                </div>
              </div>
            </div>
          ) : activeTab === "order" ? (
            /* Table Layout - Doctor Orders */
            <div className="flex-1 overflow-auto bg-[var(--color-surface-card)] min-h-0 relative text-body-base">
              <div className="min-w-[1100px] flex flex-col h-full">
                {/* Header Row - Doctor Orders (Matched with Nursing Log Header) */}
                <div className="grid grid-cols-[80px_100px_1fr_80px_60px_60px_80px_100px_180px] gap-2 px-4 py-1.5 bg-[var(--color-surface-hover)] border-b border-[var(--color-border-base)] text-body-sm font-extrabold text-[var(--color-content-secondary)] sticky top-0 z-20 tracking-tight shadow-sm">
                  <div className="text-center border-r border-[var(--color-border-base)]/50">구분</div>
                  <div className="border-r border-[var(--color-border-base)]/50 pl-2">처방코드</div>
                  <div className="border-r border-[var(--color-border-base)]/50 pl-2">처방명칭</div>
                  <div className="text-center border-r border-[var(--color-border-base)]/50">1회량</div>
                  <div className="text-center border-r border-[var(--color-border-base)]/50">횟수</div>
                  <div className="text-center border-r border-[var(--color-border-base)]/50">단위</div>
                  <div className="text-center border-r border-[var(--color-border-base)]/50">용법</div>
                  <div className="text-center border-r border-[var(--color-border-base)]/50">진행상태</div>
                  <div className="pl-2">참고사항</div>
                </div>

                {/* Order Rows (Matched with Nursing Log Row Style) */}
                <div className="flex flex-col flex-1 pb-10">
                  {orders.map((order) => (
                    <div 
                      key={order.id}
                      className="grid grid-cols-[80px_100px_1fr_80px_60px_60px_80px_100px_180px] gap-2 px-4 py-3 border-b border-[var(--color-border-base)]/50 items-center hover:bg-[var(--color-surface-hover)]/30 transition-all text-body-sm text-[var(--color-content-secondary)]"
                    >
                      <div className="text-center font-bold text-[var(--color-content-tertiary)]">
                        {order.category}
                      </div>
                      <div className="font-mono font-bold text-[var(--color-content-primary)] pl-2">{order.code}</div>
                      <div className="font-medium text-[var(--color-content-secondary)] truncate pl-2">{order.name}</div>
                      <div className="text-center font-mono font-bold">{order.dose}</div>
                      <div className="text-center font-mono font-bold">{order.frequency}</div>
                      <div className="text-center text-[var(--color-content-tertiary)]">{order.unit}</div>
                      <div className="text-center text-[var(--color-content-primary)] font-bold">{order.method}</div>
                      <div className="flex justify-center">
                        <span className={cn(
                          "text-[13px] font-semibold",
                          order.status === "진행" ? "text-[var(--color-brand-primary)]" :
                          order.status === "완료" ? "text-slate-500" :
                          order.status === "검사중" ? "text-amber-600" :
                          "text-slate-500"
                        )}>
                          {order.status}
                        </span>
                      </div>
                      <div className="text-[var(--color-content-tertiary)] text-[12px] truncate pl-2">
                        {order.remarks}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
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
