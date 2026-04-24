'use client'

import * as React from "react";
import { useState, useRef, useEffect } from "react";
import { Check, Plus, AlertCircle } from "lucide-react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";
import type { DrugInfo, NursingRecord } from "../types/record";
import { MEDICAL_SUGGESTIONS } from "@/mockup/medical-dictionary";

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
    <Popover open={open} onOpenChange={handleOpenChange} modal={true}>
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

type NursingTabProps = {
  records: NursingRecord[];
  currentUser: string;
  myRecordsOnly: boolean;
  selectedTimeHour: number | null;
  isGlobalEditing: boolean;
  patientId: string;
  onAddRecord: (record: NursingRecord) => void;
  onUpdateRecord: (id: number, updates: Partial<NursingRecord>) => void;
  onDeleteRecord: (id: number) => void;
};

export function NursingTab({
  records,
  currentUser,
  myRecordsOnly,
  selectedTimeHour,
  isGlobalEditing,
  patientId,
  onAddRecord,
  onUpdateRecord,
  onDeleteRecord,
}: NursingTabProps) {
  const [newRecordText, setNewRecordText] = useState("");
  const [newRecordTime, setNewRecordTime] = useState("");
  const [inlineAddIndex, setInlineAddIndex] = useState<number | null>(null);

  // Refs for auto-scroll
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const recordRefs = useRef<{ [key: number]: HTMLDivElement | null }>({});

  // Per-row inline edit state
  const [editingRecordId, setEditingRecordId] = useState<number | null>(null);
  // 편집 중인 행의 임시 상태. "완료" 누를 때만 records에 반영된다.
  const [draftRecord, setDraftRecord] = useState<NursingRecord | null>(null);

  const recordPasses = (record: NursingRecord) => {
    if (selectedTimeHour !== null) {
      const hour = parseInt(record.time.split(":")[0], 10);
      if (hour < selectedTimeHour) return false;
    }
    if (myRecordsOnly && record.writer !== currentUser) return false;
    return true;
  };

  const filteredRecords = records.filter(recordPasses);

  // Auto-scroll to bottom when records change, tab mounts, or patient changes
  useEffect(() => {
    const timer = setTimeout(() => {
      if (!scrollContainerRef.current) return;
      scrollContainerRef.current.scrollTo({
        top: scrollContainerRef.current.scrollHeight,
        behavior: "smooth",
      });
    }, 100);
    return () => clearTimeout(timer);
  }, [records.length, patientId]);

  // 전역 편집 종료 시 행 편집 상태도 초기화
  useEffect(() => {
    if (!isGlobalEditing) {
      setEditingRecordId(null);
      setDraftRecord(null);
    }
  }, [isGlobalEditing]);

  const enterEdit = (record: NursingRecord) => {
    setDraftRecord({
      ...record,
      drug: record.drug ? { ...record.drug } : undefined,
    });
    setEditingRecordId(record.id);
  };

  const commitEdit = () => {
    if (draftRecord) {
      onUpdateRecord(draftRecord.id, draftRecord);
    }
    setDraftRecord(null);
    setEditingRecordId(null);
  };

  const handleDelete = (id: number) => {
    if (window.confirm("이 기록을 삭제하시겠습니까?")) {
      onDeleteRecord(id);
      if (editingRecordId === id) {
        setEditingRecordId(null);
        setDraftRecord(null);
      }
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

    const newRecord: NursingRecord = {
      id: Date.now(),
      time,
      category: "간호기록",
      content: newRecordText.trim(),
      status: "completed",
      writer: currentUser,
      isConfirmed: true,
    };

    onAddRecord(newRecord);
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

  return (
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
            const isEditingRow =
              editingRecordId === record.id &&
              canEditThisRow &&
              !isGlobalEditing;

            return (
              <React.Fragment key={record.id}>
                {/* Plus button between rows */}
                <div className="relative group/between h-0 z-30">
                  <div
                    className="absolute inset-x-0 -top-2 h-4 flex items-center justify-center opacity-0 group-hover/between:opacity-100 transition-opacity cursor-pointer overflow-visible"
                    onClick={() => handleStartInlineAdd(index)}
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
                        onChange={(e) => setNewRecordText(e.target.value)}
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
                        onClick={() => setInlineAddIndex(null)}
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
                  {/* Time Column */}
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
                    {record.source === "nfc" && record.drug ? (
                      <NfcDrugContent
                        drug={
                          (isEditingRow && draftRecord?.drug
                            ? draftRecord.drug
                            : record.drug) as DrugInfo
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
                          autoFocus={editingRecordId === record.id}
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
                                  onUpdateRecord(record.id, {
                                    content: words.join(" "),
                                  });
                                }}
                              />
                            </React.Fragment>
                          ))}
                      </div>
                    )}
                  </div>

                  {/* Writer Column — 로그인 사용자 기준 자동 기입, 수정 불가 */}
                  <div className="text-body-sm text-[var(--color-content-tertiary)] font-bold pt-1.5 truncate h-full border-r border-[var(--color-border-base)]/50 pr-4 flex items-center justify-center">
                    <div className="flex items-center justify-center w-full gap-1 px-1.5 py-1">
                      <span className="truncate">{record.writer}</span>
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
                                handleDelete(record.id);
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
                              {editingRecordId === record.id ? "완료" : "수정"}
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
                                  onUpdateRecord(record.id, {
                                    ...draftRecord,
                                    isConfirmed: true,
                                  });
                                  setDraftRecord(null);
                                  setEditingRecordId(null);
                                } else {
                                  onUpdateRecord(record.id, {
                                    isConfirmed: true,
                                  });
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
              onClick={() => handleStartInlineAdd(filteredRecords.length)}
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
                  onChange={(e) => setNewRecordText(e.target.value)}
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
  );
}
