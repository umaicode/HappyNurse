"use client";

import { Search, LogOut, ChevronRight, Settings, Loader2 } from "lucide-react";
import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { format } from "date-fns";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useAuthStore } from "@/features/auth/stores/auth";
import { useNursingNotes } from "@/features/dashboard/hooks/useNursingNotes";
import { formatHHmm } from "@/lib/time";
// mockup: popover 시연용. 실 응답이 비고 환자 이름이 "문현지" 일 때만 fallback. 백엔드 합의 후 제거.
import {
  MOCK_NURSING_NOTES,
  MOCK_UNCONFIRMED_PATIENT_NAME,
} from "@/mockup/nursing-notes";
import {
  formatBirthShort,
  formatGenderShort,
  groupByRoom,
} from "@/lib/patient-display";
import type { WardPatient } from "@/features/patient/types/ward-patient";

interface PatientSidebarProps {
  patients: WardPatient[];
  isLoading?: boolean;
  selectedPatientId: number | null;
  onSelectPatient: (patientId: number) => void;
  onOpenAssignModal?: () => void;
  // EMRGrid 와 공유하는 일자 — 확정 전 기록 popover 안에서 동일 일자로 fetch.
  selectedDate: Date;
  // 확정 전 기록 항목 클릭 시 — 환자 선택 + 간호기록 탭 점프 + focus.
  onJumpToUnconfirmed?: (patientId: number, recordId: number) => void;
}

export function PatientSidebar({
  patients,
  isLoading,
  selectedPatientId,
  onSelectPatient,
  onOpenAssignModal,
  selectedDate,
  onJumpToUnconfirmed,
}: PatientSidebarProps) {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const reset = useAuthStore((state) => state.reset);
  const [searchQuery, setSearchQuery] = useState("");
  const [isMyPatientsOpen, setIsMyPatientsOpen] = useState(true);
  const [isAllPatientsOpen, setIsAllPatientsOpen] = useState(true);

  const duplicateNames = useMemo(
    () =>
      patients.reduce<Record<string, number>>((acc, p) => {
        acc[p.name] = (acc[p.name] || 0) + 1;
        return acc;
      }, {}),
    [patients],
  );

  const filteredPatients = useMemo(
    () => patients.filter((p) => p.name.includes(searchQuery)),
    [patients, searchQuery],
  );

  // 내 담당 환자 — 호실별 그룹핑
  const myPatientsByRoom = useMemo(
    () => groupByRoom(filteredPatients.filter((p) => p.isMyPatient)),
    [filteredPatients],
  );

  // 전체 환자 — 담당 여부 무관하게 모든 환자 호실별 그룹핑
  const allPatientsByRoom = useMemo(
    () => groupByRoom(filteredPatients),
    [filteredPatients],
  );

  return (
    <div className="flex flex-col h-full bg-[var(--color-surface-base)]">
      {/* Brand & Search Header */}
      <div className="p-3 border-b border-border-base flex flex-col gap-3">
        <div className="flex items-center px-1">
          <img
            src="/images/logo_ic.png"
            alt="해피너스 로고"
            className="h-5 w-auto object-contain"
          />
        </div>

        <div className="relative">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-content-muted z-10" />
          <Input
            type="text"
            placeholder="환자명 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-8 bg-white border-border-subtle shadow-sm h-8 text-body-sm focus-visible:ring-1 focus-visible:ring-[var(--color-brand-primary)]"
          />
        </div>
      </div>

      <div className="flex-1 overflow-y-auto overflow-x-hidden flex flex-col">
        {/* My Patients */}
        <Collapsible
          open={isMyPatientsOpen}
          onOpenChange={setIsMyPatientsOpen}
          className="w-full"
        >
          <div className="w-full px-4 py-2.5 flex items-center justify-between bg-white border-b border-border-subtle hover:bg-slate-50 transition-colors">
            <CollapsibleTrigger className="flex-1 flex items-center justify-between gap-2 group">
              <span className="text-[14px] font-black text-[var(--color-brand-primary)]">
                내 담당 환자
              </span>
              <ChevronRight
                className={cn(
                  "w-4 h-4 text-content-muted transition-transform duration-200",
                  isMyPatientsOpen && "rotate-90",
                )}
              />
            </CollapsibleTrigger>
            {onOpenAssignModal && (
              <button
                type="button"
                onClick={onOpenAssignModal}
                aria-label="담당 환자 설정"
                className="ml-2 flex h-7 w-7 items-center justify-center rounded-md text-content-muted hover:bg-[var(--color-brand-surface)] hover:text-[var(--color-brand-primary)] transition"
              >
                <Settings className="h-4 w-4" />
              </button>
            )}
          </div>
          <CollapsibleContent>
            <div className="flex flex-col bg-white">
              {isLoading ? (
                <div className="px-4 py-6 text-center text-[12px] font-medium text-content-muted">
                  환자 정보를 불러오는 중...
                </div>
              ) : myPatientsByRoom.length === 0 ? (
                <div className="px-4 py-6 text-center text-[12px] font-medium text-content-muted">
                  담당 환자가 없습니다
                </div>
              ) : (
                myPatientsByRoom.map(({ roomName, items }) => (
                  <div key={roomName} className="flex flex-col">
                    <div className="px-4 py-1.5 bg-slate-100/70 border-b border-border-subtle text-[11px] font-bold text-content-tertiary tracking-wider uppercase">
                      {roomName}
                    </div>
                    {items.map((patient) => (
                      <PatientItem
                        key={patient.encounterId}
                        patient={patient}
                        isActive={selectedPatientId === patient.patientId}
                        isDuplicate={duplicateNames[patient.name] > 1}
                        selectedDate={selectedDate}
                        onClick={() => onSelectPatient(patient.patientId)}
                        onJumpToUnconfirmed={onJumpToUnconfirmed}
                      />
                    ))}
                  </div>
                ))
              )}
            </div>
          </CollapsibleContent>
        </Collapsible>

        {/* All Other Patients */}
        <Collapsible
          open={isAllPatientsOpen}
          onOpenChange={setIsAllPatientsOpen}
          className="w-full"
        >
          <CollapsibleTrigger className="w-full px-4 py-2.5 flex items-center justify-between bg-slate-50/50 border-b border-border-subtle group hover:bg-slate-50 transition-colors">
            <span className="text-[14px] font-black text-content-secondary">
              전체 환자
            </span>
            <ChevronRight
              className={cn(
                "w-4 h-4 text-content-muted transition-transform duration-200",
                isAllPatientsOpen && "rotate-90",
              )}
            />
          </CollapsibleTrigger>
          <CollapsibleContent>
            <div className="flex flex-col bg-white">
              {allPatientsByRoom.map(({ roomName, items }) => (
                <div key={roomName} className="flex flex-col">
                  <div className="px-4 py-1.5 bg-slate-100/70 border-b border-border-subtle text-[11px] font-bold text-content-tertiary tracking-wider uppercase">
                    {roomName}
                  </div>
                  {items.map((patient) => (
                    <PatientItem
                      key={patient.encounterId}
                      patient={patient}
                      isActive={selectedPatientId === patient.patientId}
                      isDuplicate={duplicateNames[patient.name] > 1}
                      onClick={() => onSelectPatient(patient.patientId)}
                    />
                  ))}
                </div>
              ))}
            </div>
          </CollapsibleContent>
        </Collapsible>
      </div>

      {/* User profile + logout */}
      <div className="p-3 border-t border-border-base bg-white">
        <div className="flex items-center justify-between gap-3 p-2.5 bg-[var(--color-surface-base)] rounded-xl border border-border-subtle/50 transition-all hover:border-[var(--color-brand-primary)]/20">
          <div className="flex flex-col min-w-0 pl-1">
            <span className="text-[14px] font-black text-content-primary truncate leading-tight">
              {user?.name ?? ""}
            </span>
            <span className="text-[10px] font-bold text-content-muted uppercase tracking-wider mt-0.5">
              {user?.wardName ?? ""}
            </span>
          </div>

          <button
            onClick={() => {
              reset();
              router.push("/login");
            }}
            className="p-2 text-[var(--color-brand-primary)] hover:bg-[var(--color-brand-surface)] rounded-xl transition-all shadow-xs border border-[var(--color-brand-primary)]/10"
            title="로그아웃"
          >
            <LogOut className="size-5" />
          </button>
        </div>
      </div>
    </div>
  );
}

interface PatientItemProps {
  patient: WardPatient;
  isActive: boolean;
  isDuplicate: boolean;
  // 담당 환자 영역에서만 전달 — 둘 다 있을 때만 카운트 뱃지가 popover 트리거로 동작.
  selectedDate?: Date;
  onClick: () => void;
  onJumpToUnconfirmed?: (patientId: number, recordId: number) => void;
}

function PatientItem({
  patient,
  isActive,
  isDuplicate,
  selectedDate,
  onClick,
  onJumpToUnconfirmed,
}: PatientItemProps) {
  const [isPopoverOpen, setIsPopoverOpen] = useState(false);
  const popoverEnabled =
    onJumpToUnconfirmed !== undefined && selectedDate !== undefined;

  return (
    <div
      className={cn(
        "flex items-center justify-between w-full text-left transition-colors relative border-b border-border-subtle/20",
        isActive
          ? "bg-[var(--color-brand-surface)]/60 border-l-[4px] border-l-[var(--color-brand-primary)]"
          : "hover:bg-slate-50 bg-white border-l-[4px] border-l-transparent",
      )}
    >
      <button
        type="button"
        onClick={onClick}
        className="flex flex-col gap-1 min-w-0 flex-1 px-4 py-2.5 text-left"
      >
        <div className="flex items-center gap-3">
          <div className="relative inline-block shrink-0">
            <span
              className={cn(
                "text-base tracking-tight truncate",
                isActive
                  ? "font-bold text-[var(--color-sub-primary)]"
                  : "font-semibold text-content-secondary",
              )}
            >
              {patient.name}
            </span>
            {isDuplicate && (
              <div
                className="absolute top-0 -right-1 size-1 rounded-full bg-[var(--color-brand-primary)] shadow-[0_0_4px_var(--color-brand-primary)]/40"
                title="동명이인"
              />
            )}
          </div>
          <div
            className={cn(
              "flex items-center gap-1 text-[13px] font-mono shrink-0",
              isActive
                ? "text-[var(--color-sub-primary)]/70 font-bold"
                : "text-content-muted",
            )}
          >
            <span>{formatGenderShort(patient.gender)}</span>
            <span>/</span>
            <span>{formatBirthShort(patient.birthDate)}</span>
          </div>
        </div>
      </button>
      {patient.unconfirmedNursingCount > 0 && (
        popoverEnabled ? (
          <Popover open={isPopoverOpen} onOpenChange={setIsPopoverOpen}>
            <PopoverTrigger asChild>
              <button
                type="button"
                title={`확정 전 기록 ${patient.unconfirmedNursingCount}건`}
                className="flex items-center justify-center min-w-[22px] h-5 px-1.5 mr-3 bg-status-warning-surface text-status-warning text-[11px] font-bold rounded-full shrink-0 hover:bg-status-warning hover:text-white transition-colors"
              >
                {patient.unconfirmedNursingCount}
              </button>
            </PopoverTrigger>
            <PopoverContent
              side="right"
              align="start"
              sideOffset={8}
              className="w-72 p-0 z-[100] shadow-xl border border-border-base bg-white"
            >
              <UnconfirmedNotesContent
                patient={patient}
                selectedDate={selectedDate as Date}
                onJump={(recordId) => {
                  (onJumpToUnconfirmed as (patientId: number, recordId: number) => void)(
                    patient.patientId,
                    recordId,
                  );
                  setIsPopoverOpen(false);
                }}
              />
            </PopoverContent>
          </Popover>
        ) : (
          <span
            title={`확정 전 기록 ${patient.unconfirmedNursingCount}건`}
            className="flex items-center justify-center min-w-[22px] h-5 px-1.5 mr-3 bg-[var(--color-brand-surface)] text-[var(--color-brand-primary)] text-[11px] font-bold rounded-full shrink-0"
          >
            {patient.unconfirmedNursingCount}
          </span>
        )
      )}
    </div>
  );
}

function UnconfirmedNotesContent({
  patient,
  selectedDate,
  onJump,
}: {
  patient: WardPatient;
  selectedDate: Date;
  onJump: (recordId: number) => void;
}) {
  const dateIso = format(selectedDate, "yyyy-MM-dd");
  const { data, isPending, isError } = useNursingNotes(
    patient.encounterId,
    dateIso,
  );
  // 간호 기록(STT_NOTE) 의 draft 만 — 백엔드 응답이 같은 일자만 내려주므로 selectedDate 기준.
  // mockup: "문현지" + 실 응답 비었을 때 MOCK_NURSING_NOTES fallback (시연용).
  const draftNotes = useMemo(() => {
    const real = data ?? [];
    const useMock =
      real.length === 0 && patient.name === MOCK_UNCONFIRMED_PATIENT_NAME;
    const source = useMock ? MOCK_NURSING_NOTES : real;
    return source
      .filter(
        (note): note is Extract<typeof note, { type: "STT_NOTE" }> =>
          note.type === "STT_NOTE" && note.status === "draft",
      )
      .sort(
        (a, b) =>
          new Date(a.occurredAt).getTime() - new Date(b.occurredAt).getTime(),
      );
  }, [data, patient.name]);

  return (
    <>
      <div className="px-3 py-2 border-b border-border-base flex items-center justify-between">
        <span className="text-body-sm font-bold text-content-primary truncate">
          {patient.name} · 확정 전 기록
        </span>
        <span className="text-body-micro font-mono text-content-tertiary shrink-0 ml-2">
          {dateIso}
        </span>
      </div>
      <div className="max-h-[360px] overflow-y-auto">
        {isPending ? (
          <div className="flex flex-col items-center justify-center py-8 gap-2 text-content-muted">
            <Loader2 className="size-4 animate-spin" />
            <p className="text-body-micro">불러오는 중...</p>
          </div>
        ) : isError ? (
          <div className="px-4 py-8 text-center text-body-xs font-medium text-content-muted">
            기록을 불러오지 못했습니다
          </div>
        ) : draftNotes.length === 0 ? (
          <div className="px-4 py-8 text-center text-body-xs font-medium text-content-muted">
            이 날짜에 확정 전 간호기록이 없습니다
          </div>
        ) : (
          draftNotes.map((note) => (
            <button
              key={note.nursingRecordId}
              type="button"
              onClick={() => onJump(note.nursingRecordId)}
              className="flex flex-col w-full px-3 py-2 hover:bg-surface-hover transition-colors text-left border-b border-border-subtle/50 last:border-b-0 gap-0.5"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-mono text-body-xs font-bold text-content-primary shrink-0">
                  {formatHHmm(note.occurredAt)}
                </span>
                <span className="text-body-micro text-content-muted truncate">
                  {note.authorName}
                </span>
              </div>
              <p className="text-body-xs text-content-secondary line-clamp-2 leading-snug">
                {note.content}
              </p>
            </button>
          ))
        )}
      </div>
    </>
  );
}
