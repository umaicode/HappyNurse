'use client'

import { Plus, Loader2 } from "lucide-react";
import * as React from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { formatHHmm } from "@/lib/time";
import { Button } from "@/components/ui/button";
import { useNursingNotes } from "../hooks/useNursingNotes";
import {
  useConfirmNursingRecord,
  useCreateNursingRecord,
  useDeleteNursingRecord,
  useUpdateNursingRecord,
} from "../hooks/useNursingRecordMutations";
import {
  useConfirmMedicationGroup,
  useDeleteMedicationGroup,
  useUpdateMedicationGroup,
} from "../hooks/useMedicationAdministrationMutations";
import {
  NOTE_TYPE_LABEL,
  NOTE_TYPE_TONE,
  type MedicationItem,
  type NursingNoteItem,
} from "../types/nursing-note";
// mockup: 투약 컬럼 시각 검증. API 데이터 충분해지면 import 와 fallback 제거.
import { MOCK_NURSING_NOTES } from "@/mockup/nursing-notes";

type NursingTabProps = {
  encounterId: number | null;
  // ISO date (yyyy-MM-dd) — 백엔드 필수 파라미터, EMRGrid 의 selectedDate 에서 변환
  date: string;
  currentUser: string;
  myRecordsOnly: boolean;
  // EMRGrid 헤더의 "편집" 토글 — 꺼져 있으면 동작 컬럼은 draft 행의 "확정"만 노출.
  isEditMode: boolean;
  // 사이드바의 "확정 전 기록" 항목 클릭 시 해당 record 로 자동 스크롤. 한 번 처리 후 onFocusHandled 호출.
  focusRecordId?: number | null;
  onFocusHandled?: () => void;
};

export function NursingTab({
  encounterId,
  date,
  currentUser,
  myRecordsOnly,
  isEditMode,
  focusRecordId,
  onFocusHandled,
}: NursingTabProps) {
  const { data, isPending, isError } = useNursingNotes(encounterId, date);

  // 백엔드는 occurredAt desc 로 내려주지만, 화면은 시간 asc (오래된 위 / 최신 아래) 로 표시 후
  // 현재 시각 근처 카드를 가운데로 자동 스크롤한다 (PatientAlerts 와 동일 패턴).
  // mockup fallback — 실 응답 비었을 때만 노출.
  const filteredNotes = useMemo(() => {
    const notes = !!data && data.length === 0 ? MOCK_NURSING_NOTES : (data ?? []);
    return notes
      .filter((note) => {
        if (myRecordsOnly && note.authorName !== currentUser) return false;
        return true;
      })
      .sort(
        (a, b) =>
          new Date(a.occurredAt).getTime() - new Date(b.occurredAt).getTime(),
      );
  }, [data, myRecordsOnly, currentUser]);

  const itemRefs = useRef<Map<string, HTMLDivElement | null>>(new Map());
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (filteredNotes.length === 0) return;
    // focusRecordId 가 있으면 그 행 우선, 없으면 현재 시각 근처 카드를 가운데로.
    let target: HTMLDivElement | null | undefined = null;
    if (focusRecordId != null) {
      const focused = filteredNotes.find(
        (note) =>
          note.type === "STT_NOTE" && note.nursingRecordId === focusRecordId,
      );
      if (focused) {
        target = itemRefs.current.get(rowKey(focused));
      }
    }
    if (!target) {
      const now = Date.now();
      const closest = filteredNotes.reduce((best, note) => {
        const distance = Math.abs(new Date(note.occurredAt).getTime() - now);
        const bestDistance = Math.abs(
          new Date(best.occurredAt).getTime() - now,
        );
        return distance < bestDistance ? note : best;
      }, filteredNotes[0]);
      target = itemRefs.current.get(rowKey(closest));
    }
    if (target) {
      target.scrollIntoView({ block: "center" });
    }
    if (focusRecordId != null) {
      onFocusHandled?.();
    }
  }, [filteredNotes, focusRecordId, onFocusHandled]);

  // 인라인 추가 — `+` 버튼 호버/클릭 시 폼이 그 위치에 펼쳐진다.
  // 백엔드 NursingRecordManualCreateRequest 는 { encounterId, content } 만 받으므로 시각은 서버 자동.
  const [inlineAddIndex, setInlineAddIndex] = useState<number | null>(null);

  return (
    <div
      ref={scrollContainerRef}
      className="flex-1 overflow-auto bg-surface-card min-h-0 relative text-body-base"
    >
      <div className="min-w-[800px] flex flex-col h-full">
        {/* Header Row */}
        <div className="grid grid-cols-[90px_1fr_70px_90px_110px] gap-4 px-4 py-1.5 bg-surface-hover border-b border-border-base text-body-sm font-extrabold text-content-secondary sticky top-0 z-20 tracking-tight shadow-sm">
          <div className="border-r border-border-base pr-4 text-center">시간</div>
          <div className="border-r border-border-base pr-4">기록 내용</div>
          <div className="border-r border-border-base pr-4 text-center">구분</div>
          <div className="border-r border-border-base pr-4 h-full flex items-center justify-center">기록자</div>
          <div className="text-center">동작</div>
        </div>

        {/* Body */}
        <div className="flex flex-col flex-1 pb-10">
          {encounterId === null ? (
            <EmptyState message="환자를 선택하면 간호 기록이 표시됩니다." />
          ) : isPending ? (
            <LoadingState />
          ) : isError ? (
            <EmptyState message="간호 기록을 불러오지 못했습니다." />
          ) : (
            <>
              {filteredNotes.map((note, index) => {
                const key = rowKey(note);
                return (
                  <React.Fragment key={key}>
                    <BetweenRowAdd
                      onClick={() => setInlineAddIndex(index)}
                    />

                    {inlineAddIndex === index && encounterId !== null && (
                      <InlineAddForm
                        encounterId={encounterId}
                        currentUser={currentUser}
                        onClose={() => setInlineAddIndex(null)}
                      />
                    )}

                    <NoteRow
                      note={note}
                      encounterId={encounterId}
                      isEditMode={isEditMode}
                      rowRef={(element) => {
                        if (element) itemRefs.current.set(key, element);
                        else itemRefs.current.delete(key);
                      }}
                    />
                  </React.Fragment>
                );
              })}

              {/* 마지막 행 아래 */}
              <BetweenRowAdd
                onClick={() => setInlineAddIndex(filteredNotes.length)}
              />
              {inlineAddIndex === filteredNotes.length &&
                encounterId !== null && (
                  <InlineAddForm
                    encounterId={encounterId}
                    currentUser={currentUser}
                    onClose={() => setInlineAddIndex(null)}
                  />
                )}

              {filteredNotes.length === 0 && (
                <EmptyState
                  message={
                    (data?.length ?? 0) === 0
                      ? "등록된 간호 기록이 없습니다."
                      : "필터 조건에 맞는 기록이 없습니다."
                  }
                />
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function rowKey(note: NursingNoteItem): string {
  return note.type === "STT_NOTE"
    ? `stt-${note.nursingRecordId}`
    : `med-${note.taggingId}`;
}

// ISO datetime 의 HH:mm 만 새 값으로 교체 (날짜/초/타임존 보존).
function replaceTimeInIso(originalIso: string, hhmm: string): string {
  const tIndex = originalIso.indexOf("T");
  if (tIndex === -1) return `${originalIso}T${hhmm}:00`;
  return (
    originalIso.substring(0, tIndex + 1) + hhmm + originalIso.substring(tIndex + 6)
  );
}

// "H:m" / "1:" / "" 등 부분 입력을 "HH:mm" 으로 정규화. 숫자 외 또는 공란 → "00".
function normalizeHHmm(value: string): string {
  const [hRaw, mRaw] = value.split(":");
  const h = (hRaw ?? "").padStart(2, "0") || "00";
  const m = (mRaw ?? "").padStart(2, "0") || "00";
  return `${h}:${m}`;
}

function TimeInput({
  value,
  onChange,
}: {
  value: string;
  onChange: (next: string) => void;
}) {
  const [hour, minute] = (() => {
    const parts = value.split(":");
    return [parts[0] ?? "", parts[1] ?? ""];
  })();

  const update = (h: string, m: string) => {
    onChange(`${h}:${m}`);
  };

  const cellClass =
    "w-9 text-center bg-transparent focus:outline-none font-mono font-extrabold text-[15px] text-content-primary";

  return (
    <div className="flex items-center justify-center gap-0.5 w-full leading-tight border border-border-base rounded px-1 py-1 bg-white focus-within:ring-1 focus-within:ring-content-primary/20">
      <input
        type="text"
        inputMode="numeric"
        maxLength={2}
        value={hour}
        placeholder="HH"
        onFocus={(event) => event.target.select()}
        onChange={(event) => {
          const digits = event.target.value.replace(/\D/g, "");
          if (digits.length > 0 && parseInt(digits, 10) > 23) return;
          update(digits, minute);
        }}
        onBlur={() => update(hour ? hour.padStart(2, "0") : "00", minute)}
        className={cellClass}
      />
      <span className="font-mono font-bold text-content-muted">:</span>
      <input
        type="text"
        inputMode="numeric"
        maxLength={2}
        value={minute}
        placeholder="mm"
        onFocus={(event) => event.target.select()}
        onChange={(event) => {
          const digits = event.target.value.replace(/\D/g, "");
          if (digits.length > 0 && parseInt(digits, 10) > 59) return;
          update(hour, digits);
        }}
        onBlur={() => update(hour, minute ? minute.padStart(2, "0") : "00")}
        className={cellClass}
      />
    </div>
  );
}

function BetweenRowAdd({ onClick }: { onClick: () => void }) {
  return (
    <div className="relative group/between h-0 z-30">
      <div
        className="absolute inset-x-0 -top-2 h-4 flex items-center justify-center opacity-0 group-hover/between:opacity-100 transition-opacity cursor-pointer overflow-visible"
        onClick={onClick}
      >
        <div className="w-full h-[1px] bg-brand-primary/20" />
        <div className="absolute size-5 rounded-full bg-brand-primary text-white flex items-center justify-center shadow-sm hover:scale-110 transition-transform">
          <Plus className="size-3.5" />
        </div>
      </div>
    </div>
  );
}

function InlineAddForm({
  encounterId,
  currentUser,
  onClose,
}: {
  encounterId: number;
  currentUser: string;
  onClose: () => void;
}) {
  const [content, setContent] = useState("");
  const createMutation = useCreateNursingRecord(encounterId);

  const handleSubmit = () => {
    const trimmed = content.trim();
    if (!trimmed) return;
    createMutation.mutate(
      { encounterId, content: trimmed },
      {
        onSuccess: () => {
          setContent("");
          onClose();
        },
      },
    );
  };

  return (
    <div className="grid grid-cols-[90px_1fr_70px_90px_110px] gap-4 px-4 py-2 border-y border-brand-primary/10 bg-brand-surface/30 items-center shadow-inner">
      <div className="text-center text-body-micro font-mono text-content-muted">
        자동
      </div>
      <div className="pr-4">
        <textarea
          autoFocus
          placeholder="새 간호 기록 본문을 입력하세요..."
          value={content}
          onChange={(event) => setContent(event.target.value)}
          rows={1}
          className="w-full bg-white border border-brand-primary/10 rounded px-2 py-1.5 text-body-sm focus:outline-none focus:ring-1 focus:ring-brand-primary/20 transition-all resize-none shadow-xs"
        />
      </div>
      <div className="text-center text-body-xs font-semibold text-content-tertiary">
        {NOTE_TYPE_LABEL.STT_NOTE}
      </div>
      <div className="text-body-sm text-content-tertiary font-bold truncate text-center">
        {currentUser}
      </div>
      <div className="flex gap-1 justify-center">
        <Button
          variant="neutral"
          size="sm"
          className="h-7 px-2.5 rounded text-body-micro font-bold"
          onClick={handleSubmit}
          disabled={createMutation.isPending || !content.trim()}
        >
          {createMutation.isPending ? "추가 중..." : "추가"}
        </Button>
        <Button
          variant="neutral"
          size="sm"
          className="h-7 px-2.5 rounded text-body-micro font-bold"
          onClick={() => {
            setContent("");
            onClose();
          }}
        >
          취소
        </Button>
      </div>
    </div>
  );
}

function NoteRow({
  note,
  encounterId,
  isEditMode,
  rowRef,
}: {
  note: NursingNoteItem;
  encounterId: number;
  // 편집 모드 (수정/삭제 노출 여부). false 면 draft 행의 "확정"만.
  isEditMode: boolean;
  rowRef?: (element: HTMLDivElement | null) => void;
}) {
  const isMedication = note.type === "MEDICATION";

  // STT_NOTE: content + occurredAt / MEDICATION: dosageQuantity + effectiveDatetime
  const [isEditing, setIsEditing] = useState(false);
  const [draftContent, setDraftContent] = useState("");
  const [draftTime, setDraftTime] = useState(""); // "HH:mm" 형식
  const [medicationDrafts, setMedicationDrafts] = useState<
    Record<number, number>
  >({});
  const updateNoteMutation = useUpdateNursingRecord(encounterId);
  const updateMedicationMutation = useUpdateMedicationGroup(encounterId);

  const startEdit = () => {
    setDraftTime(formatHHmm(note.occurredAt));
    if (note.type === "STT_NOTE") {
      setDraftContent(note.content);
    } else {
      setMedicationDrafts(
        Object.fromEntries(
          note.medications.map((medication) => [
            medication.medicationAdminId,
            medication.dosageQuantity,
          ]),
        ),
      );
    }
    setIsEditing(true);
  };

  const cancelEdit = () => {
    setIsEditing(false);
    setDraftContent("");
    setDraftTime("");
    setMedicationDrafts({});
  };

  const submitEdit = () => {
    const originalTime = formatHHmm(note.occurredAt);
    const normalizedTime = normalizeHHmm(draftTime);
    const timeChanged = normalizedTime !== originalTime;
    const newOccurredAt = timeChanged
      ? replaceTimeInIso(note.occurredAt, normalizedTime)
      : null;

    if (note.type === "STT_NOTE") {
      const trimmed = draftContent.trim();
      const contentChanged = trimmed !== note.content;
      if (!trimmed) return;
      if (!contentChanged && !timeChanged) {
        cancelEdit();
        return;
      }
      updateNoteMutation.mutate(
        {
          nursingRecordId: note.nursingRecordId,
          request: {
            ...(contentChanged ? { content: trimmed } : {}),
            // 백엔드 명세상 시간 필드는 confirmedAt — UI 의 시간 컬럼이 confirmedAt 에 매핑됨.
            ...(newOccurredAt ? { confirmedAt: newOccurredAt } : {}),
          },
        },
        {
          onSuccess: () => {
            setIsEditing(false);
            setDraftContent("");
            setDraftTime("");
          },
        },
      );
      return;
    }
    // MEDICATION — 변경된 약물 + 시간 (effectiveDatetime).
    const changedMeds = note.medications
      .filter(
        (medication) =>
          medicationDrafts[medication.medicationAdminId] !== undefined &&
          medicationDrafts[medication.medicationAdminId] !==
            medication.dosageQuantity,
      )
      .map((medication) => ({
        medicationAdminId: medication.medicationAdminId,
        dosageQuantity: medicationDrafts[medication.medicationAdminId],
        dosageUnit: medication.dosageUnit,
      }));
    if (changedMeds.length === 0 && !timeChanged) {
      cancelEdit();
      return;
    }
    updateMedicationMutation.mutate(
      {
        taggingId: note.taggingId,
        request: {
          ...(changedMeds.length > 0 ? { medications: changedMeds } : {}),
          ...(newOccurredAt ? { effectiveDatetime: newOccurredAt } : {}),
        },
      },
      {
        onSuccess: () => {
          setIsEditing(false);
          setDraftTime("");
          setMedicationDrafts({});
        },
      },
    );
  };

  return (
    <div
      ref={rowRef}
      className={cn(
        "grid grid-cols-[90px_1fr_70px_90px_110px] gap-4 px-4 py-2 border-b border-border-base/50 items-start hover:bg-surface-hover/40 transition-all relative",
        note.status === "draft" &&
          "before:absolute before:left-0 before:top-0 before:bottom-0 before:w-[4px] before:bg-brand-primary/30",
        isEditing && "bg-brand-surface/15",
      )}
    >
      {/* 시간 — 편집 모드에선 HH : mm 분리 입력. STT_NOTE = confirmedAt / MEDICATION = effectiveDatetime 으로 PATCH. */}
      <div className="py-1.5 border-r border-border-base/50 pr-4 min-w-0">
        {isEditing ? (
          <TimeInput value={draftTime} onChange={setDraftTime} />
        ) : (
          <div className="w-full text-center font-mono font-extrabold text-[15px] text-content-primary leading-[1.6]">
            {formatHHmm(note.occurredAt)}
          </div>
        )}
      </div>

      {/* 기록 내용 */}
      <div className="min-w-0 pr-6 border-r border-border-base/50 py-1.5 relative">
        {isMedication ? (
          <MedicationContent
            note={note}
            isEditing={isEditing}
            drafts={medicationDrafts}
            onChangeDraft={(medicationAdminId, value) =>
              setMedicationDrafts((prev) => ({
                ...prev,
                [medicationAdminId]: value,
              }))
            }
          />
        ) : isEditing ? (
          <textarea
            autoFocus
            value={draftContent}
            onChange={(event) => setDraftContent(event.target.value)}
            ref={(element) => {
              if (element) {
                element.style.height = "auto";
                element.style.height = `${element.scrollHeight}px`;
              }
            }}
            onInput={(event) => {
              const target = event.currentTarget;
              target.style.height = "auto";
              target.style.height = `${target.scrollHeight}px`;
            }}
            className="w-full bg-white border border-brand-primary/30 rounded px-2 py-1.5 text-body-sm leading-[1.6] text-content-primary resize-none focus:outline-none focus:ring-1 focus:ring-brand-primary/20 shadow-xs"
            rows={1}
          />
        ) : (
          <SttContent content={note.content} />
        )}
      </div>

      {/* 구분 */}
      <div className="pt-1.5 h-full flex items-center justify-center border-r border-border-base/50 pr-4">
        <span
          className={cn(
            "text-body-xs font-semibold",
            NOTE_TYPE_TONE[note.type] ?? "text-content-tertiary",
          )}
        >
          {NOTE_TYPE_LABEL[note.type] ?? note.type}
        </span>
      </div>

      {/* 기록자 */}
      <div className="text-body-sm text-content-tertiary pt-1.5 truncate h-full border-r border-border-base/50 pr-4 flex items-center justify-center">
        <span className="truncate font-bold">{note.authorName}</span>
      </div>

      {/* 동작 — 확정은 위, 수정/삭제는 아래 줄 */}
      <div className="pt-1 h-full flex flex-col items-center justify-center gap-1">
        {note.editable ? (
          isEditing ? (
            <div className="flex gap-1">
              <Button
                variant="neutral"
                size="sm"
                className="h-7 px-2.5 rounded text-body-micro font-bold"
                disabled={
                  isMedication
                    ? updateMedicationMutation.isPending
                    : updateNoteMutation.isPending || !draftContent.trim()
                }
                onClick={submitEdit}
              >
                {(isMedication
                  ? updateMedicationMutation.isPending
                  : updateNoteMutation.isPending)
                  ? "저장 중..."
                  : "완료"}
              </Button>
              <Button
                variant="neutral"
                size="sm"
                className="h-7 px-2.5 rounded text-body-micro font-bold"
                onClick={cancelEdit}
              >
                취소
              </Button>
            </div>
          ) : isMedication ? (
            <MedicationActions
              note={note}
              encounterId={encounterId}
              isEditMode={isEditMode}
              onStartEdit={startEdit}
            />
          ) : (
            <SttNoteActions
              note={note as Extract<NursingNoteItem, { type: "STT_NOTE" }>}
              encounterId={encounterId}
              isEditMode={isEditMode}
              onStartEdit={startEdit}
            />
          )
        ) : (
          <span className="text-[11px] text-content-muted">-</span>
        )}
      </div>
    </div>
  );
}

function SttNoteActions({
  note,
  encounterId,
  isEditMode,
  onStartEdit,
}: {
  note: Extract<NursingNoteItem, { type: "STT_NOTE" }>;
  encounterId: number;
  isEditMode: boolean;
  onStartEdit: () => void;
}) {
  const confirmMutation = useConfirmNursingRecord(encounterId);
  const deleteMutation = useDeleteNursingRecord(encounterId);

  return (
    <>
      {note.status === "draft" && <ConfirmButton onClick={() => confirmMutation.mutate(note.nursingRecordId)} disabled={confirmMutation.isPending} />}
      {isEditMode && (
        <div className="flex gap-1">
          <Button
            variant="neutral"
            size="sm"
            className="h-7 px-2.5 rounded text-body-micro font-bold"
            onClick={onStartEdit}
          >
            수정
          </Button>
          <Button
            variant="neutral"
            size="sm"
            className="h-7 px-2.5 rounded text-body-micro font-bold"
            disabled={deleteMutation.isPending}
            onClick={() => {
              if (window.confirm("이 기록을 삭제하시겠습니까?")) {
                deleteMutation.mutate(note.nursingRecordId);
              }
            }}
          >
            삭제
          </Button>
        </div>
      )}
    </>
  );
}

function MedicationActions({
  note,
  encounterId,
  isEditMode,
  onStartEdit,
}: {
  note: Extract<NursingNoteItem, { type: "MEDICATION" }>;
  encounterId: number;
  isEditMode: boolean;
  onStartEdit: () => void;
}) {
  const confirmMutation = useConfirmMedicationGroup(encounterId);
  const deleteMutation = useDeleteMedicationGroup(encounterId);

  return (
    <>
      {note.status === "draft" && <ConfirmButton onClick={() => confirmMutation.mutate(note.taggingId)} disabled={confirmMutation.isPending} />}
      {isEditMode && (
        <div className="flex gap-1">
          <Button
            variant="neutral"
            size="sm"
            className="h-7 px-2.5 rounded text-body-micro font-bold"
            onClick={onStartEdit}
          >
            수정
          </Button>
          <Button
            variant="neutral"
            size="sm"
            className="h-7 px-2.5 rounded text-body-micro font-bold"
            disabled={deleteMutation.isPending}
            onClick={() => {
              if (window.confirm("이 투약 기록을 삭제하시겠습니까?")) {
                deleteMutation.mutate(note.taggingId);
              }
            }}
          >
            삭제
          </Button>
        </div>
      )}
    </>
  );
}

function ConfirmButton({
  onClick,
  disabled,
}: {
  onClick: () => void;
  disabled: boolean;
}) {
  return (
    <Button
      variant="neutral"
      size="sm"
      className="h-7 px-2.5 rounded text-body-micro font-bold"
      onClick={onClick}
      disabled={disabled}
    >
      확정
    </Button>
  );
}

function SttContent({ content }: { content: string }) {
  return (
    <div className="text-body-sm font-medium leading-[1.6] text-content-primary whitespace-pre-wrap break-all">
      {content}
    </div>
  );
}

function MedicationContent({
  note,
  isEditing,
  drafts,
  onChangeDraft,
}: {
  note: Extract<NursingNoteItem, { type: "MEDICATION" }>;
  isEditing: boolean;
  drafts: Record<number, number>;
  onChangeDraft: (medicationAdminId: number, value: number) => void;
}) {
  return (
    <ul className="flex flex-col gap-1.5">
      {note.medications.map((medication) => (
        <MedicationRow
          key={medication.medicationAdminId}
          medication={medication}
          isEditing={isEditing}
          draftValue={drafts[medication.medicationAdminId]}
          onChangeDraft={(value) =>
            onChangeDraft(medication.medicationAdminId, value)
          }
        />
      ))}
    </ul>
  );
}

function MedicationRow({
  medication,
  isEditing,
  draftValue,
  onChangeDraft,
}: {
  medication: MedicationItem;
  isEditing: boolean;
  draftValue: number | undefined;
  onChangeDraft: (value: number) => void;
}) {
  return (
    <li className="flex flex-col gap-0.5 px-2 py-1.5 rounded bg-brand-surface/30 border-l-2 border-brand-primary/30">
      <div className="flex items-baseline gap-2 min-w-0">
        <span className="font-bold text-content-primary text-body-sm truncate">
          {medication.productName}
        </span>
        <span className="font-mono text-body-micro text-content-muted shrink-0">
          {medication.productCode}
        </span>
      </div>
      <div className="flex items-baseline gap-1.5 text-body-xs">
        {isEditing ? (
          <input
            type="number"
            inputMode="decimal"
            min={0}
            step="any"
            value={draftValue ?? medication.dosageQuantity}
            onChange={(event) => {
              const next = Number(event.target.value);
              if (!Number.isFinite(next) || next < 0) return;
              onChangeDraft(next);
            }}
            className="w-16 px-1.5 py-0.5 rounded bg-white border border-brand-primary/40 font-mono font-bold text-brand-primary focus:outline-none focus:ring-1 focus:ring-brand-primary/30"
          />
        ) : (
          <span className="font-mono font-bold text-brand-primary">
            {medication.dosageQuantity}
          </span>
        )}
        <span className="text-content-muted font-medium">{medication.dosageUnit}</span>
        <span className="text-border-base">·</span>
        <span className="font-mono font-bold text-content-primary">{medication.frequency}</span>
        <span className="text-content-muted">회</span>
        <span className="text-border-base">·</span>
        <span className="font-bold text-content-secondary">{medication.route}</span>
      </div>
    </li>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="flex flex-col items-center justify-center flex-1 gap-2 py-16 text-content-muted">
      <p className="text-body-sm font-bold">{message}</p>
    </div>
  );
}

function LoadingState() {
  return (
    <div className="flex flex-col items-center justify-center flex-1 gap-2 py-16 text-content-muted">
      <Loader2 className="w-6 h-6 animate-spin opacity-60" />
      <p className="text-body-sm">불러오는 중...</p>
    </div>
  );
}
