"use client";

import { Search, LogOut, ChevronRight, Settings, Loader2 } from "lucide-react";
import { useEffect, useState, useMemo } from "react";
import { useLogout } from "@/features/auth/hooks/useLogout";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
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
import { extendSession } from "@/features/auth/api";
import { useDraftNursingNotes } from "@/features/dashboard/hooks/useDraftNursingNotes";
import { formatMonthDayHHmm } from "@/lib/time";
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
  // 확정 전 기록 항목 클릭 시 — 환자 선택 + 간호기록 탭 점프 + focus + 해당 일자로 이동.
  onJumpToUnconfirmed?: (
    patientId: number,
    recordId: number,
    occurredAt: string,
  ) => void;
}

export function PatientSidebar({
  patients,
  isLoading,
  selectedPatientId,
  onSelectPatient,
  onOpenAssignModal,
  onJumpToUnconfirmed,
}: PatientSidebarProps) {
  const user = useAuthStore((state) => state.user);
  const expiresAt = useAuthStore((state) => state.expiresAt);
  const refreshExpiresAt = useAuthStore((state) => state.refreshExpiresAt);
  const logout = useLogout();
  const [searchQuery, setSearchQuery] = useState("");
  const [isExtending, setIsExtending] = useState(false);

  // 연장 버튼 — /auth/refresh 호출 + 성공 시 expiresAt 을 now+15분 으로 재계산.
  // 실패 시 인터셉터가 401 처리 우회하므로 그냥 무시 (15 분 idle 타이머가 자동 로그아웃 담당).
  const handleExtend = async () => {
    if (isExtending) return;
    setIsExtending(true);
    try {
      await extendSession();
      refreshExpiresAt();
    } catch {
      // 무시 — useAuth 의 idle 타이머가 만료 처리.
    } finally {
      setIsExtending(false);
    }
  };
  // 1초 tick — 카운트다운 mm:ss 갱신. expiresAt 이 null 이면 동작 안 함.
  const [nowMs, setNowMs] = useState<number>(() => Date.now());
  useEffect(() => {
    if (expiresAt === null) return;
    const handle = setInterval(() => setNowMs(Date.now()), 1000);
    return () => clearInterval(handle);
  }, [expiresAt]);
  const remainingLabel = useMemo(() => {
    if (expiresAt === null) return null;
    const remainingSeconds = Math.max(
      0,
      Math.floor((expiresAt - nowMs) / 1000),
    );
    const minutes = Math.floor(remainingSeconds / 60);
    const seconds = remainingSeconds % 60;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
  }, [expiresAt, nowMs]);

  const [isMyPatientsOpen, setIsMyPatientsOpen] = useState(true);
  const [isAllPatientsOpen, setIsAllPatientsOpen] = useState(true);

  const duplicateNames = useMemo(
    () =>
      patients.reduce<Record<string, number>>((accumulator, patient) => {
        accumulator[patient.name] = (accumulator[patient.name] || 0) + 1;
        return accumulator;
      }, {}),
    [patients],
  );

  const filteredPatients = useMemo(
    () => patients.filter((patient) => patient.name.includes(searchQuery)),
    [patients, searchQuery],
  );

  // 내 담당 환자 — 호실별 그룹핑
  const myPatientsByRoom = useMemo(
    () => groupByRoom(filteredPatients.filter((patient) => patient.isMyPatient)),
    [filteredPatients],
  );

  // 전체 환자 — 담당 여부 무관하게 모든 환자 호실별 그룹핑
  const allPatientsByRoom = useMemo(
    () => groupByRoom(filteredPatients),
    [filteredPatients],
  );

  return (
    <div className="flex flex-col h-full bg-surface-base">
      {/* Brand & Search Header */}
      <div className="p-3 border-b border-border-base flex flex-col gap-3">
        <div className="flex items-center justify-between gap-2 px-1">
          <img
            src="/images/logo_ic.png"
            alt="해피너스 로고"
            className="h-5 w-auto object-contain"
          />
          {remainingLabel !== null && (
            <div className="flex items-center gap-1.5 shrink-0">
              <span className="font-mono text-body-xs font-bold text-content-secondary tabular-nums leading-none">
                {remainingLabel}
              </span>
              <Button
                type="button"
                variant="brandOutline"
                size="sm"
                className="h-6 px-2 text-body-micro font-bold"
                onClick={handleExtend}
                disabled={isExtending}
              >
                {isExtending ? "..." : "연장"}
              </Button>
            </div>
          )}
        </div>

        <div className="relative">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-content-muted z-10" />
          <Input
            type="text"
            placeholder="환자명 검색..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            className="pl-8 bg-white border-border-subtle shadow-sm h-8 text-body-sm focus-visible:ring-1 focus-visible:ring-brand-primary"
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
              <span className="text-[14px] font-black text-brand-primary">
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
                className="ml-2 flex h-7 w-7 items-center justify-center rounded-md text-content-muted hover:bg-brand-surface hover:text-brand-primary transition"
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
        <div className="flex items-center justify-between gap-3 p-2.5 bg-surface-base rounded-xl border border-border-subtle/50 transition-all hover:border-brand-primary/20">
          <div className="flex flex-col min-w-0 pl-1">
            <span className="text-[14px] font-black text-content-primary truncate leading-tight">
              {user?.name ?? ""}
              {user?.name && (
                <span className="ml-1 text-body-xs font-medium text-content-tertiary">
                  간호사
                </span>
              )}
            </span>
            {user?.wardName && (
              <span className="text-body-xs font-semibold text-content-secondary truncate mt-0.5">
                {user.wardName}
              </span>
            )}
          </div>

          <button
            onClick={() => {
              logout();
            }}
            className="p-2 text-status-danger hover:bg-status-danger-surface rounded-xl transition-all shadow-xs border border-status-danger/20"
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
  onClick: () => void;
  // 담당 환자 영역에서만 전달 — 있을 때만 카운트 뱃지가 popover 트리거로 동작.
  onJumpToUnconfirmed?: (
    patientId: number,
    recordId: number,
    occurredAt: string,
  ) => void;
}

function PatientItem({
  patient,
  isActive,
  isDuplicate,
  onClick,
  onJumpToUnconfirmed,
}: PatientItemProps) {
  const [isPopoverOpen, setIsPopoverOpen] = useState(false);
  const popoverEnabled = onJumpToUnconfirmed !== undefined;

  // 담당 환자 영역에 한해 /drafts 응답으로 뱃지 숫자를 직접 계산.
  // 백엔드의 WardPatient.unconfirmedNursingCount 가 실제 /drafts 응답과 어긋나는 케이스가 있어
  // popover 와 같은 데이터로 통일한다. 같은 queryKey 라 popover 열 때 재요청은 일어나지 않는다.
  // editable === true 인 항목 = 현재 로그인 간호사가 작성한 기록만 (백엔드 판단).
  const draftQuery = useDraftNursingNotes(
    popoverEnabled ? patient.encounterId : null,
  );
  const draftCount = popoverEnabled
    ? (draftQuery.data ?? []).filter(
        (note) => note.type === "STT_NOTE" && note.editable,
      ).length
    : patient.unconfirmedNursingCount;

  return (
    <div
      className={cn(
        "flex items-center justify-between w-full text-left transition-colors relative border-b border-border-subtle/20",
        isActive
          ? "bg-brand-surface/60 border-l-[4px] border-l-brand-primary"
          : "hover:bg-slate-50 bg-white border-l-[4px] border-l-transparent",
      )}
    >
      <button
        type="button"
        onClick={onClick}
        className="flex flex-col gap-1 min-w-0 flex-1 px-4 py-2.5 text-left"
      >
        <div className="flex items-center gap-2">
          <div className="relative inline-block shrink-0">
            <span
              className={cn(
                "text-base tracking-tight truncate",
                isActive
                  ? "font-bold text-sub-primary"
                  : "font-semibold text-content-secondary",
              )}
            >
              {patient.name}
            </span>
            {isDuplicate && (
              <div
                className="absolute top-0 -right-1 size-1 rounded-full bg-brand-primary shadow-[0_0_4px_var(--color-brand-primary)]/40"
                title="동명이인"
              />
            )}
          </div>
          <div
            className={cn(
              "flex items-center gap-1 text-[13px] leading-tight font-bold shrink-0 ml-auto",
              isActive ? "text-sub-primary/70" : "text-content-tertiary",
            )}
          >
            <span>{formatGenderShort(patient.gender)}</span>
            <span>/</span>
            <span>{formatBirthShort(patient.birthDate)}</span>
          </div>
        </div>
      </button>
      {popoverEnabled ? (
        // 담당 환자 영역: 본인 작성 draft 가 0건이어도 항상 노출 (0 도 표시).
        <Popover open={isPopoverOpen} onOpenChange={setIsPopoverOpen}>
          <PopoverTrigger asChild>
            <button
              type="button"
              title={`확정 전 기록 ${draftCount}건`}
              className={cn(
                "flex items-center justify-center min-w-[22px] h-5 px-1.5 mr-3 text-[11px] font-bold rounded-full shrink-0 transition-colors",
                draftCount > 0
                  ? "bg-status-warning-surface text-status-warning hover:bg-status-warning hover:text-white"
                  : "bg-surface-hover text-content-muted hover:bg-border-base",
              )}
            >
              {draftCount}
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
              onJump={(recordId, occurredAt) => {
                (onJumpToUnconfirmed as (
                  patientId: number,
                  recordId: number,
                  occurredAt: string,
                ) => void)(patient.patientId, recordId, occurredAt);
                setIsPopoverOpen(false);
              }}
            />
          </PopoverContent>
        </Popover>
      ) : draftCount > 0 ? (
        // 전체 환자 영역: 카운트 > 0 일 때만 정적 뱃지.
        <span
          title={`확정 전 기록 ${draftCount}건`}
          className="flex items-center justify-center min-w-[22px] h-5 px-1.5 mr-3 bg-brand-surface text-brand-primary text-[11px] font-bold rounded-full shrink-0"
        >
          {draftCount}
        </span>
      ) : null}
    </div>
  );
}

function UnconfirmedNotesContent({
  patient,
  onJump,
}: {
  patient: WardPatient;
  onJump: (recordId: number, occurredAt: string) => void;
}) {
  // /encounters/{id}/nursing-notes/drafts — 입원 단위 모든 일자의 draft 를 통합 반환.
  const { data, isPending, isError } = useDraftNursingNotes(patient.encounterId);
  // 현재 popover UI 는 STT_NOTE 만 렌더링 (MEDICATION draft 는 별도 디자인 필요).
  // editable === true → 현재 로그인 간호사가 작성한 기록만 (백엔드 판단).
  const draftNotes = useMemo(() => {
    return (data ?? [])
      .filter(
        (note): note is Extract<typeof note, { type: "STT_NOTE" }> =>
          note.type === "STT_NOTE" && note.editable,
      )
      .sort(
        (a, b) =>
          new Date(a.occurredAt).getTime() - new Date(b.occurredAt).getTime(),
      );
  }, [data]);

  return (
    <>
      <div className="px-3 py-2 border-b border-border-base flex items-center justify-between">
        <span className="text-body-sm font-bold text-content-primary truncate">
          {patient.name} · 확정 전 기록
        </span>
        {draftNotes.length > 0 && (
          <span className="text-body-micro font-mono text-content-tertiary shrink-0 ml-2">
            {draftNotes.length}건
          </span>
        )}
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
            확정 전 간호기록이 없습니다
          </div>
        ) : (
          draftNotes.map((note) => (
            <button
              key={note.nursingRecordId}
              type="button"
              onClick={() => onJump(note.nursingRecordId, note.occurredAt)}
              className="flex flex-col w-full px-3 py-2 hover:bg-surface-hover transition-colors text-left border-b border-border-subtle/50 last:border-b-0 gap-0.5"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-mono text-body-xs font-bold text-content-primary shrink-0">
                  {formatMonthDayHHmm(note.occurredAt)}
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
