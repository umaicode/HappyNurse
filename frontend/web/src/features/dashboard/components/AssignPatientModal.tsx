"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";
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
  const [searchQuery, setSearchQuery] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const assignMutation = useAssignMyPatients();

  const normalizedQuery = searchQuery.trim().toLowerCase();

  const filteredPatients = useMemo(() => {
    if (!normalizedQuery) return patients;
    return patients.filter((p) =>
      p.name.toLowerCase().includes(normalizedQuery),
    );
  }, [patients, normalizedQuery]);

  const grouped = useMemo(
    () => groupByRoom(filteredPatients),
    [filteredPatients],
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
    const selectedCount = roomPatients.filter((p) =>
      selectedIds.has(p.encounterId),
    ).length;
    if (selectedCount === 0) return false;
    if (selectedCount === roomPatients.length) return true;
    return "indeterminate";
  };

  const toggleRoom = (roomPatients: WardPatient[]) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      const allSelected = roomPatients.every((p) =>
        next.has(p.encounterId),
      );
      if (allSelected) {
        roomPatients.forEach((p) => next.delete(p.encounterId));
      } else {
        roomPatients.forEach((p) => next.add(p.encounterId));
      }
      return next;
    });
  };

  const handleSave = () => {
    if (assignMutation.isPending) return;
    setErrorMessage("");
    assignMutation.mutate(Array.from(selectedIds), {
      onSuccess: () => onClose(),
      onError: () =>
        setErrorMessage("담당 환자 저장에 실패했습니다. 잠시 후 다시 시도해주세요."),
    });
  };

  const hasResults = grouped.length > 0;

  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
    >
      <DialogContent className="sm:max-w-[520px] max-h-[85vh] overflow-hidden flex flex-col rounded-2xl p-0 border border-border-base bg-white shadow-2xl">
        <DialogHeader className="px-7 pt-7 pb-4 border-b border-border-subtle">
          <DialogTitle className="text-2xl font-bold text-sub-primary">
            담당 환자 설정
          </DialogTitle>
          <DialogDescription className="text-[14px] text-content-tertiary">
            담당할 환자를 선택하세요. 선택 해제한 환자는 담당에서 제외됩니다.
          </DialogDescription>
        </DialogHeader>

        <div className="px-7 py-3 border-b border-border-subtle">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-content-muted z-10" />
            <Input
              type="text"
              placeholder="환자명 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9 h-9 bg-white border-border-subtle focus-visible:ring-1 focus-visible:ring-brand-primary"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {!hasResults ? (
            <div className="py-12 text-center text-[14px] font-semibold text-content-muted">
              {patients.length === 0
                ? "병동에 입원 중인 환자가 없습니다"
                : "검색 결과가 없습니다"}
            </div>
          ) : (
            grouped.map(({ roomName, items: roomPatients }) => {
              const roomState = getRoomCheckedState(roomPatients);
              const roomCheckboxId = `assign-room-${roomName}`;
              return (
                <div key={roomName} className="flex flex-col">
                  <label
                    htmlFor={roomCheckboxId}
                    className="flex items-center gap-2 px-2 py-1.5 border-b border-border-subtle mb-1 cursor-pointer select-none hover:bg-surface-hover rounded-md"
                  >
                    <span className="text-[13px] font-bold text-content-muted tracking-wider uppercase">
                      {roomName}
                    </span>
                    <div className="ml-auto flex items-center gap-2">
                      <span className="text-[12px] font-semibold text-content-tertiary">
                        전체선택
                      </span>
                      <Checkbox
                        id={roomCheckboxId}
                        checked={roomState}
                        onCheckedChange={() => toggleRoom(roomPatients)}
                      />
                    </div>
                  </label>
                  <div className="flex flex-col">
                    {roomPatients.map((patient) => {
                      const checked = selectedIds.has(patient.encounterId);
                      const checkboxId = `assign-${patient.encounterId}`;
                      return (
                        <label
                          key={patient.encounterId}
                          htmlFor={checkboxId}
                          className="flex items-center gap-3 w-full px-3 py-2.5 rounded-md hover:bg-surface-hover transition-all cursor-pointer select-none"
                        >
                          <Checkbox
                            id={checkboxId}
                            checked={checked}
                            onCheckedChange={() =>
                              togglePatient(patient.encounterId)
                            }
                          />
                          <span className="text-base font-bold text-content-primary truncate">
                            {patient.name}
                          </span>
                          <span className="text-[13px] font-mono font-bold text-content-muted shrink-0">
                            {formatGenderShort(patient.gender)}/
                            {formatBirthShort(patient.birthDate)}
                          </span>
                        </label>
                      );
                    })}
                  </div>
                </div>
              );
            })
          )}
        </div>

        <div className="border-t border-border-subtle px-7 py-5 flex items-center justify-between gap-3 bg-white">
          <div className="flex items-center gap-3 text-[13px] font-semibold">
            <span className="text-content-tertiary">
              {selectedIds.size}명 선택됨
            </span>
            {errorMessage ? (
              <span className="text-red-500">{errorMessage}</span>
            ) : null}
          </div>
          <Button
            type="button"
            variant="brand"
            size="lg"
            onClick={handleSave}
            disabled={assignMutation.isPending}
          >
            {assignMutation.isPending ? "저장 중..." : "저장"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
