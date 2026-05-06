'use client'

import { Plus, Loader2 } from "lucide-react";
import * as React from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import { cn } from "@/lib/utils";
import { formatHHmm, splitDateLabel } from "@/lib/time";
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
} from "../hooks/useMedicationAdministrationMutations";
import {
  NOTE_TYPE_LABEL,
  NOTE_TYPE_TONE,
  RECORD_STATUS_LABEL,
  RECORD_STATUS_TONE,
  type MedicationItem,
  type NursingNoteItem,
} from "../types/nursing-note";

type NursingTabProps = {
  encounterId: number | null;
  // ISO date (yyyy-MM-dd) — 백엔드 필수 파라미터, EMRGrid 의 selectedDate 에서 변환
  date: string | null;
  currentUser: string;
  myRecordsOnly: boolean;
};

export function NursingTab({
  encounterId,
  date,
  currentUser,
  myRecordsOnly,
}: NursingTabProps) {
  const { data, isPending, isError } = useNursingNotes(encounterId, date);
  const notes = data ?? [];

  // 백엔드는 occurredAt desc 로 내려주지만, 화면은 시간 asc (오래된 위 / 최신 아래) 로 표시 후
  // 현재 시각 근처 카드를 가운데로 자동 스크롤한다 (PatientAlerts 와 동일 패턴).
  const filteredNotes = useMemo(() => {
    return notes
      .filter((note) => {
        if (myRecordsOnly && note.authorName !== currentUser) return false;
        return true;
      })
      .sort(
        (a, b) =>
          new Date(a.occurredAt).getTime() - new Date(b.occurredAt).getTime(),
      );
  }, [notes, myRecordsOnly, currentUser]);

  const itemRefs = useRef<Map<string, HTMLDivElement | null>>(new Map());
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (filteredNotes.length === 0) return;
    const now = Date.now();
    const closest = filteredNotes.reduce((best, note) => {
      const distance = Math.abs(new Date(note.occurredAt).getTime() - now);
      const bestDistance = Math.abs(new Date(best.occurredAt).getTime() - now);
      return distance < bestDistance ? note : best;
    }, filteredNotes[0]);
    const element = itemRefs.current.get(rowKey(closest));
    if (element) {
      element.scrollIntoView({ block: "center" });
    }
  }, [filteredNotes]);

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
        <div className="grid grid-cols-[110px_1fr_80px_110px_120px_120px] gap-4 px-4 py-1.5 bg-surface-hover border-b border-border-base text-body-sm font-extrabold text-content-secondary sticky top-0 z-20 tracking-tight shadow-sm">
          <div className="border-r border-border-base pr-4 text-center">시간</div>
          <div className="border-r border-border-base pr-4">기록 내용</div>
          <div className="border-r border-border-base pr-4 text-center">구분</div>
          <div className="border-r border-border-base pr-4 h-full flex items-center justify-center">기록자</div>
          <div className="text-center border-r border-border-base pr-4">상태</div>
          <div className="text-center">동작</div>
        </div>

        {/* Body */}
        <div className="flex flex-col flex-1 pb-10">
          {encounterId === null ? (
            <EmptyState message="환자를 선택하면 간호 기록이 표시됩니다." />
          ) : date === null ? (
            <EmptyState message="간호 기록 전체 보기는 백엔드 endpoint 추가 후 활성화됩니다. 날짜를 선택해주세요." />
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
                      isDateView={date !== null}
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
                    notes.length === 0
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
    <div className="grid grid-cols-[110px_1fr_80px_110px_120px_120px] gap-4 px-4 py-2 border-y border-brand-primary/10 bg-brand-surface/30 items-center shadow-inner">
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
      <div className="text-center text-body-micro text-content-muted">-</div>
      <div className="flex gap-1.5 justify-center">
        <button
          type="button"
          onClick={handleSubmit}
          disabled={createMutation.isPending || !content.trim()}
          className="px-3 py-1.5 bg-brand-primary text-white text-[11px] font-bold rounded shadow-sm hover:bg-brand-hover disabled:opacity-50 transition-colors whitespace-nowrap"
        >
          {createMutation.isPending ? "추가 중..." : "추가"}
        </button>
        <button
          type="button"
          onClick={() => {
            setContent("");
            onClose();
          }}
          className="px-3 py-1.5 bg-white border border-border-base text-[11px] font-bold rounded shadow-sm hover:bg-surface-hover transition-colors whitespace-nowrap"
        >
          취소
        </button>
      </div>
    </div>
  );
}

function NoteRow({
  note,
  encounterId,
  isDateView,
  rowRef,
}: {
  note: NursingNoteItem;
  encounterId: number;
  // 특정 날짜로 조회 중이면 시간만 (HH:mm), 전체 조회면 날짜+시간 (MM.dd / HH:mm)
  isDateView: boolean;
  rowRef?: (element: HTMLDivElement | null) => void;
}) {
  const isMedication = note.type === "MEDICATION";
  const dateLabel = splitDateLabel(note.occurredAt);

  // STT_NOTE 한정 인라인 편집 — MEDICATION 은 시각/용량 모달이 별도라 여기선 편집 X.
  const [isEditing, setIsEditing] = useState(false);
  const [draftContent, setDraftContent] = useState("");
  const updateMutation = useUpdateNursingRecord(encounterId);

  const startEdit = () => {
    if (note.type !== "STT_NOTE") return;
    setDraftContent(note.content);
    setIsEditing(true);
  };

  const cancelEdit = () => {
    setIsEditing(false);
    setDraftContent("");
  };

  const submitEdit = () => {
    if (note.type !== "STT_NOTE") return;
    const trimmed = draftContent.trim();
    if (!trimmed) return;
    updateMutation.mutate(
      { nursingRecordId: note.nursingRecordId, request: { content: trimmed } },
      {
        onSuccess: () => {
          setIsEditing(false);
          setDraftContent("");
        },
      },
    );
  };

  return (
    <div
      ref={rowRef}
      className={cn(
        "grid grid-cols-[110px_1fr_80px_110px_120px_120px] gap-4 px-4 py-2 border-b border-border-base/50 items-start hover:bg-surface-hover/40 transition-all relative",
        note.status === "draft" &&
          "before:absolute before:left-0 before:top-0 before:bottom-0 before:w-[4px] before:bg-brand-primary/30",
        isEditing && "bg-brand-surface/15",
      )}
    >
      {/* 시간 */}
      <div className="py-1.5 border-r border-border-base/50 pr-4 min-w-0">
        {isDateView ? (
          <div className="w-full text-center font-mono font-extrabold text-[15px] text-content-primary leading-[1.6]">
            {formatHHmm(note.occurredAt)}
          </div>
        ) : (
          <div className="text-center font-mono">
            <div className="text-body-xs font-bold text-content-primary leading-tight">
              {dateLabel.month}.{dateLabel.day}
            </div>
            <div className="text-[15px] font-extrabold text-content-primary leading-tight mt-0.5">
              {dateLabel.time}
            </div>
          </div>
        )}
      </div>

      {/* 기록 내용 */}
      <div className="min-w-0 pr-6 border-r border-border-base/50 py-1.5 relative">
        {isMedication ? (
          <MedicationContent note={note} />
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

      {/* 상태 */}
      <div className="pt-1.5 h-full flex items-center justify-center border-r border-border-base/50 pr-4">
        <span
          className={cn(
            "text-body-xs font-semibold",
            RECORD_STATUS_TONE[note.status] ?? "text-status-neutral",
          )}
        >
          {RECORD_STATUS_LABEL[note.status] ?? note.status}
        </span>
      </div>

      {/* 동작 */}
      <div className="pt-1 h-full flex items-center justify-center gap-1.5 flex-wrap">
        {note.editable ? (
          isMedication ? (
            <MedicationActions note={note} encounterId={encounterId} />
          ) : isEditing ? (
            <>
              <Button
                variant="brand"
                size="sm"
                className="h-7 px-2.5 rounded text-body-micro"
                disabled={updateMutation.isPending || !draftContent.trim()}
                onClick={submitEdit}
              >
                {updateMutation.isPending ? "저장 중..." : "완료"}
              </Button>
              <Button
                variant="brandOutline"
                size="sm"
                className="h-7 px-2.5 rounded text-body-micro"
                onClick={cancelEdit}
              >
                취소
              </Button>
            </>
          ) : (
            <SttNoteActions
              note={note as Extract<NursingNoteItem, { type: "STT_NOTE" }>}
              encounterId={encounterId}
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
  onStartEdit,
}: {
  note: Extract<NursingNoteItem, { type: "STT_NOTE" }>;
  encounterId: number;
  onStartEdit: () => void;
}) {
  const confirmMutation = useConfirmNursingRecord(encounterId);
  const deleteMutation = useDeleteNursingRecord(encounterId);

  return (
    <>
      {note.status === "draft" && (
        <Button
          variant="brand"
          size="sm"
          className="h-7 px-2.5 rounded text-body-micro"
          disabled={confirmMutation.isPending}
          onClick={() => confirmMutation.mutate(note.nursingRecordId)}
        >
          확정
        </Button>
      )}
      <Button
        variant="brandOutline"
        size="sm"
        className="h-7 px-2.5 rounded text-body-micro"
        onClick={onStartEdit}
      >
        수정
      </Button>
      <Button
        variant="brandOutline"
        size="sm"
        className="h-7 px-2.5 rounded text-body-micro"
        disabled={deleteMutation.isPending}
        onClick={() => {
          if (window.confirm("이 기록을 삭제하시겠습니까?")) {
            deleteMutation.mutate(note.nursingRecordId);
          }
        }}
      >
        삭제
      </Button>
    </>
  );
}

function MedicationActions({
  note,
  encounterId,
}: {
  note: Extract<NursingNoteItem, { type: "MEDICATION" }>;
  encounterId: number;
}) {
  const confirmMutation = useConfirmMedicationGroup(encounterId);
  const deleteMutation = useDeleteMedicationGroup(encounterId);

  return (
    <>
      {note.status === "draft" && (
        <Button
          variant="brand"
          size="sm"
          className="h-7 px-2.5 rounded text-body-micro"
          disabled={confirmMutation.isPending}
          onClick={() => confirmMutation.mutate(note.taggingId)}
        >
          확정
        </Button>
      )}
      <Button
        variant="brandOutline"
        size="sm"
        className="h-7 px-2.5 rounded text-body-micro"
        disabled={deleteMutation.isPending}
        onClick={() => {
          if (window.confirm("이 투약 그룹을 취소하시겠습니까?")) {
            deleteMutation.mutate(note.taggingId);
          }
        }}
      >
        취소
      </Button>
    </>
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
}: {
  note: Extract<NursingNoteItem, { type: "MEDICATION" }>;
}) {
  return (
    <ul className="px-1.5 py-1 flex flex-col gap-1">
      {note.medications.map((medication) => (
        <MedicationRow key={medication.medicationAdminId} medication={medication} />
      ))}
    </ul>
  );
}

function MedicationRow({ medication }: { medication: MedicationItem }) {
  return (
    <li className="flex flex-wrap items-baseline gap-x-1.5 gap-y-0.5 text-body-sm">
      <span className="font-mono font-bold text-brand-primary text-body-xs">
        {medication.productCode}
      </span>
      <span className="font-bold text-content-primary">{medication.productName}</span>
      <span className="text-content-muted">·</span>
      <span className="font-mono font-semibold text-content-primary">
        {medication.dosageQuantity}
      </span>
      <span className="text-content-muted">{medication.dosageUnit}</span>
      <span className="text-content-muted">·</span>
      <span className="font-mono font-semibold text-content-primary">{medication.frequency}</span>
      <span className="text-content-muted">회</span>
      <span className="text-content-muted">·</span>
      <span className="font-semibold text-content-primary">{medication.route}</span>
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
