"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  formatBirthShort,
  formatGenderShort,
  groupByRoom,
} from "@/lib/patient-display";
import { useAssignMyPatients } from "@/features/patient/hooks/useWardPatients";
import type { WardPatient } from "@/features/patient/types/ward-patient";

interface AssignPatientModalProps {
  // 부모에서 `{isAssignOpen && <AssignPatientModal ... />}` 로 조건부 렌더 — 매번 mount 시 useState 초기값으로 자동 reset.
  patients: WardPatient[];
  onClose: () => void;
}

type RoomCheckedState = boolean | "indeterminate";

export function AssignPatientModal({
  patients,
  onClose,
}: AssignPatientModalProps) {
  // mount 시 1회 — patients 의 isMyPatient 들로 초기 선택값. 이후엔 사용자 선택만 유지.
  const [selectedIds, setSelectedIds] = useState<Set<number>>(() => {
    const set = new Set<number>();
    patients.forEach((patient) => {
      if (patient.isMyPatient) set.add(patient.encounterId);
    });
    return set;
  });
  // mount 시점 초기 선택값 보존 — 푸터 변경 요약(+추가 / -제외) 계산용.
  // useState lazy initializer 로 1회 고정. 사용자가 선택을 바꾸어도 setter 호출 안 해 그대로 유지.
  const [initialSelectedIds] = useState<Set<number>>(
    () =>
      new Set(
        patients
          .filter((patient) => patient.isMyPatient)
          .map((patient) => patient.encounterId),
      ),
  );
  const [searchQuery, setSearchQuery] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  // 담당 환자가 0명인 상태에서 자동 열린 모달 — 헤더 description 톤 강조용.
  const isOnboarding = initialSelectedIds.size === 0;

  const assignMutation = useAssignMyPatients();

  const normalizedQuery = searchQuery.trim().toLowerCase();

  // 검색 범위 확장 — 환자명 + 호실명 + chiefComplaint OR 매치.
  // 호실명에서 "호" 떼고 비교 (사용자가 "301" 만 입력해도 "301호" 매치).
  const filteredPatients = useMemo(() => {
    if (!normalizedQuery) return patients;
    return patients.filter((patient) => {
      const name = patient.name.toLowerCase();
      const roomBare = patient.roomName.replace(/호$/, "").toLowerCase();
      const room = patient.roomName.toLowerCase();
      const cc = (patient.chiefComplaint ?? "").toLowerCase();
      return (
        name.includes(normalizedQuery) ||
        room.includes(normalizedQuery) ||
        roomBare.includes(normalizedQuery) ||
        cc.includes(normalizedQuery)
      );
    });
  }, [patients, normalizedQuery]);

  const grouped = useMemo(
    () => groupByRoom(filteredPatients),
    [filteredPatients],
  );

  // 동명이인 — 사이드바와 동일 시각 표시. 검색으로 1명만 남은 경우에도 원본 patients 기준으로 본다.
  const duplicateNames = useMemo(
    () =>
      patients.reduce<Record<string, number>>((accumulator, patient) => {
        accumulator[patient.name] = (accumulator[patient.name] || 0) + 1;
        return accumulator;
      }, {}),
    [patients],
  );

  const togglePatient = (encounterId: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(encounterId)) next.delete(encounterId);
      else next.add(encounterId);
      return next;
    });
  };

  const getRoomCheckedState = (roomPatients: WardPatient[]): RoomCheckedState => {
    if (roomPatients.length === 0) return false;
    const selectedCount = roomPatients.filter((patient) =>
      selectedIds.has(patient.encounterId),
    ).length;
    if (selectedCount === 0) return false;
    if (selectedCount === roomPatients.length) return true;
    return "indeterminate";
  };

  const toggleRoom = (roomPatients: WardPatient[]) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      const allSelected = roomPatients.every((patient) =>
        next.has(patient.encounterId),
      );
      if (allSelected) {
        roomPatients.forEach((patient) => next.delete(patient.encounterId));
      } else {
        roomPatients.forEach((patient) => next.add(patient.encounterId));
      }
      return next;
    });
  };

  // 변경사항 diff — initial 대비 추가 / 제외 카운트.
  const { addedCount, removedCount, hasChanges } = useMemo(() => {
    let added = 0;
    let removed = 0;
    selectedIds.forEach((id) => {
      if (!initialSelectedIds.has(id)) added += 1;
    });
    initialSelectedIds.forEach((id) => {
      if (!selectedIds.has(id)) removed += 1;
    });
    return {
      addedCount: added,
      removedCount: removed,
      hasChanges: added > 0 || removed > 0,
    };
  }, [selectedIds, initialSelectedIds]);

  const handleSave = () => {
    if (assignMutation.isPending) return;
    setErrorMessage("");
    assignMutation.mutate(Array.from(selectedIds), {
      onSuccess: () => onClose(),
      onError: () =>
        setErrorMessage("담당 환자 저장에 실패했습니다. 잠시 후 다시 시도해주세요."),
    });
  };

  // 취소 — 변경사항 있으면 한 번 확인. 없으면 그냥 닫기.
  const handleCancelOrClose = () => {
    if (hasChanges) {
      const confirmed = window.confirm(
        "변경사항이 저장되지 않았습니다. 정말 닫을까요?",
      );
      if (!confirmed) return;
    }
    onClose();
  };

  const hasResults = grouped.length > 0;

  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) handleCancelOrClose();
      }}
    >
      <DialogContent className="sm:max-w-[560px] max-h-[85vh] overflow-hidden flex flex-col rounded-2xl p-0 border border-border-base bg-white shadow-2xl">
        <DialogHeader className="px-7 pt-7 pb-4 border-b border-border-subtle">
          <DialogTitle className="text-2xl font-bold text-sub-primary">
            담당 환자 설정
          </DialogTitle>
          <DialogDescription
            className={cn(
              "text-[14px]",
              isOnboarding
                ? "text-brand-primary font-semibold"
                : "text-content-tertiary",
            )}
          >
            {isOnboarding
              ? "담당 환자를 선택해주세요. 시프트 교대 후에도 선택은 유지됩니다."
              : "담당할 환자를 선택하세요. 선택 해제한 환자는 담당에서 제외됩니다."}
          </DialogDescription>
        </DialogHeader>

        <div className="px-7 py-3 border-b border-border-subtle">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-content-muted z-10" />
            <Input
              type="text"
              placeholder="환자명 · 호실 · 증상 검색"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              className="pl-9 h-9 bg-white border-border-subtle focus-visible:ring-1 focus-visible:ring-brand-primary"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto">
          {!hasResults ? (
            <div className="py-12 px-7 text-center text-[14px] font-semibold text-content-muted">
              {patients.length === 0
                ? "병동에 입원 중인 환자가 없습니다"
                : "검색 결과가 없습니다 · 환자명·호실·증상으로 다시 검색해보세요"}
            </div>
          ) : (
            grouped.map(({ roomName, items: roomPatients }) => {
              const roomState = getRoomCheckedState(roomPatients);
              const roomCheckboxId = `assign-room-${roomName}`;
              const selectedInRoom = roomPatients.filter((patient) =>
                selectedIds.has(patient.encounterId),
              ).length;
              return (
                <div key={roomName} className="flex flex-col">
                  {/* 호실 그룹 헤더 — sticky 로 스크롤 시 위치 감각 유지. */}
                  <label
                    htmlFor={roomCheckboxId}
                    className="sticky top-0 z-10 flex items-center gap-2 px-7 py-2 bg-slate-100/95 backdrop-blur border-b border-border-subtle cursor-pointer select-none hover:bg-slate-100"
                  >
                    <span className="text-[12px] font-bold text-content-secondary tracking-wider uppercase">
                      {roomName}
                    </span>
                    <span className="text-body-micro font-bold text-content-muted">
                      담당 {selectedInRoom}/{roomPatients.length}명
                    </span>
                    <div className="ml-auto flex items-center gap-2">
                      <span className="text-body-micro font-semibold text-content-tertiary">
                        전체선택
                      </span>
                      <Checkbox
                        id={roomCheckboxId}
                        checked={roomState}
                        onCheckedChange={() => toggleRoom(roomPatients)}
                      />
                    </div>
                  </label>
                  <div className="flex flex-col bg-white">
                    {roomPatients.map((patient) => {
                      const checked = selectedIds.has(patient.encounterId);
                      const checkboxId = `assign-${patient.encounterId}`;
                      const isDuplicate = duplicateNames[patient.name] > 1;
                      // 호실-침대 칩 — 사이드바 IV 카드와 동일 패턴 ("{roomName-호}-{bedName}").
                      const roomBed = [
                        patient.roomName.replace(/호$/, ""),
                        patient.bedName,
                      ]
                        .filter(Boolean)
                        .join("-");
                      return (
                        <label
                          key={patient.encounterId}
                          htmlFor={checkboxId}
                          className={cn(
                            "flex items-center gap-3 w-full pl-6 pr-7 py-2.5 border-b border-border-subtle/40 last:border-b-0 cursor-pointer select-none transition-colors relative",
                            checked
                              ? "bg-brand-surface/40 border-l-[4px] border-l-brand-primary"
                              : "hover:bg-surface-hover border-l-[4px] border-l-transparent",
                          )}
                        >
                          <Checkbox
                            id={checkboxId}
                            checked={checked}
                            onCheckedChange={() =>
                              togglePatient(patient.encounterId)
                            }
                            className="shrink-0"
                          />
                          <div className="flex items-center gap-2 min-w-0 flex-1">
                            {/* 이름 + 동명이인 점 */}
                            <div className="relative inline-block shrink-0">
                              <span className="text-base font-bold text-content-primary truncate">
                                {patient.name}
                              </span>
                              {isDuplicate && (
                                <div
                                  className="absolute top-0 -right-1 size-1 rounded-full bg-brand-primary shadow-[0_0_4px_var(--color-brand-primary)]/40"
                                  title="동명이인"
                                />
                              )}
                            </div>
                            {/* 호실-침대 칩 — 사이드바와 동일 톤 */}
                            {roomBed && (
                              <span className="px-1.5 py-0.5 rounded bg-[#F7F8FA] text-content-secondary text-[11px] font-bold leading-none shrink-0">
                                {roomBed}
                              </span>
                            )}
                            {/* 성별/생년월일 — 우측 정렬. 폰트는 전역 Pretendard. */}
                            <span className="ml-auto text-[13px] font-bold text-content-tertiary shrink-0">
                              {formatGenderShort(patient.gender)}/
                              {formatBirthShort(patient.birthDate)}
                            </span>
                          </div>
                        </label>
                      );
                    })}
                  </div>
                </div>
              );
            })
          )}
        </div>

        <div className="border-t border-border-subtle px-7 py-4 flex items-center justify-between gap-3 bg-white">
          <div className="flex flex-col gap-0.5 min-w-0">
            {/* 담당 인원 변화 — 기존 → 변경 후. 색은 content-primary 로 강조해 한눈에 보이게. */}
            <div className="flex items-baseline gap-2 text-body-sm font-bold">
              <span className="text-content-primary">
                담당 {initialSelectedIds.size}명
              </span>
              <span className="text-content-muted">→</span>
              <span className="text-brand-primary">{selectedIds.size}명</span>
              {hasChanges && (
                <span className="text-body-xs font-bold">
                  {addedCount > 0 && (
                    <span className="text-status-success">+{addedCount}</span>
                  )}
                  {addedCount > 0 && removedCount > 0 && (
                    <span className="text-content-muted"> · </span>
                  )}
                  {removedCount > 0 && (
                    <span className="text-status-danger">-{removedCount}</span>
                  )}
                </span>
              )}
            </div>
            {errorMessage && (
              <span className="text-body-micro font-semibold text-status-danger">
                {errorMessage}
              </span>
            )}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <Button
              type="button"
              variant="neutral"
              size="default"
              onClick={handleCancelOrClose}
              disabled={assignMutation.isPending}
            >
              취소
            </Button>
            <Button
              type="button"
              variant="brand"
              size="default"
              onClick={handleSave}
              disabled={assignMutation.isPending || !hasChanges}
            >
              {assignMutation.isPending ? "저장 중..." : "저장"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
