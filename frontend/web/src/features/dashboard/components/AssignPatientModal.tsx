"use client";

import { useEffect, useMemo, useState } from "react";
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
import type { Patient, Ward } from "@/features/patient/types/patient";

interface AssignPatientModalProps {
  wards: Ward[];
  currentUser: string;
  isOpen: boolean;
  onClose: () => void;
  onAssignPatients: (patientIds: string[]) => void;
}

type RoomCheckedState = boolean | "indeterminate";

export function AssignPatientModal({
  wards,
  currentUser,
  isOpen,
  onClose,
  onAssignPatients,
}: AssignPatientModalProps) {
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [searchQuery, setSearchQuery] = useState("");

  const initiallyAssignedIds = useMemo(() => {
    const set = new Set<string>();
    wards.forEach((ward) =>
      ward.rooms.forEach((room) =>
        room.patients.forEach((patient) => {
          if (patient.assignedNurse === currentUser) {
            set.add(patient.id);
          }
        }),
      ),
    );
    return set;
  }, [wards, currentUser]);

  // 모달이 열릴 때마다 현재 담당 상태와 검색어를 초기화한다.
  useEffect(() => {
    if (isOpen) {
      setSelectedIds(new Set(initiallyAssignedIds));
      setSearchQuery("");
    }
  }, [isOpen, initiallyAssignedIds]);

  const normalizedQuery = searchQuery.trim().toLowerCase();

  // 빈 병동/호실은 항상 제거하고, 검색어가 있을 때만 환자명으로 추가 필터링한다.
  const filteredWards = useMemo(() => {
    return wards
      .map((ward) => ({
        ...ward,
        rooms: ward.rooms
          .map((room) => ({
            ...room,
            patients: normalizedQuery
              ? room.patients.filter((p) =>
                  p.name.toLowerCase().includes(normalizedQuery),
                )
              : room.patients,
          }))
          .filter((room) => room.patients.length > 0),
      }))
      .filter((ward) => ward.rooms.length > 0);
  }, [wards, normalizedQuery]);

  const showWardHeader = filteredWards.length > 1;

  const togglePatient = (patientId: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(patientId)) next.delete(patientId);
      else next.add(patientId);
      return next;
    });
  };

  const getRoomCheckedState = (patients: Patient[]): RoomCheckedState => {
    if (patients.length === 0) return false;
    const selectedCount = patients.filter((p) => selectedIds.has(p.id)).length;
    if (selectedCount === 0) return false;
    if (selectedCount === patients.length) return true;
    return "indeterminate";
  };

  const toggleRoom = (patients: Patient[]) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      const allSelected = patients.every((p) => next.has(p.id));
      if (allSelected) {
        patients.forEach((p) => next.delete(p.id));
      } else {
        patients.forEach((p) => next.add(p.id));
      }
      return next;
    });
  };

  const handleSave = () => {
    onAssignPatients(Array.from(selectedIds));
    onClose();
  };

  const hasResults = filteredWards.some((ward) =>
    ward.rooms.some((room) => room.patients.length > 0),
  );

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
    >
      <DialogContent className="sm:max-w-[520px] max-h-[85vh] overflow-hidden flex flex-col rounded-2xl p-0 border border-border-base bg-white shadow-2xl">
        <DialogHeader className="px-7 pt-7 pb-4 border-b border-border-subtle">
          <DialogTitle className="text-2xl font-bold text-[var(--color-sub-primary)]">
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
              className="pl-9 h-9 bg-white border-border-subtle focus-visible:ring-1 focus-visible:ring-[var(--color-brand-primary)]"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {!hasResults ? (
            <div className="py-12 text-center text-[14px] font-semibold text-content-muted">
              검색 결과가 없습니다
            </div>
          ) : (
            filteredWards.map((ward) => {
              if (ward.rooms.length === 0) return null;
              return (
                <div
                  key={ward.id}
                  className="flex flex-col gap-2 pb-4 last:pb-0"
                >
                  {showWardHeader && (
                    <div className="px-2 text-[14px] font-black text-content-secondary tracking-wider uppercase">
                      {ward.name}
                    </div>
                  )}
                  {ward.rooms.map((room) => {
                    const roomState = getRoomCheckedState(room.patients);
                    const roomCheckboxId = `assign-room-${room.id}`;
                    return (
                      <div key={room.id} className="flex flex-col">
                        <label
                          htmlFor={roomCheckboxId}
                          className="flex items-center gap-2 px-2 py-1.5 border-b border-border-subtle mb-1 cursor-pointer select-none hover:bg-[var(--color-surface-hover)] rounded-md"
                        >
                          <span className="text-[13px] font-bold text-content-muted tracking-wider uppercase">
                            {room.name}
                          </span>
                          <div className="ml-auto flex items-center gap-2">
                            <span className="text-[12px] font-semibold text-content-tertiary">
                              전체선택
                            </span>
                            <Checkbox
                              id={roomCheckboxId}
                              checked={roomState}
                              onCheckedChange={() => toggleRoom(room.patients)}
                            />
                          </div>
                        </label>
                        <div className="flex flex-col">
                          {room.patients.map((patient) => {
                            const checked = selectedIds.has(patient.id);
                            const checkboxId = `assign-${patient.id}`;
                            return (
                              <label
                                key={patient.id}
                                htmlFor={checkboxId}
                                className="flex items-center gap-3 w-full px-3 py-2.5 rounded-md hover:bg-[var(--color-surface-hover)] transition-all cursor-pointer select-none"
                              >
                                <Checkbox
                                  id={checkboxId}
                                  checked={checked}
                                  onCheckedChange={() =>
                                    togglePatient(patient.id)
                                  }
                                />
                                <span className="text-base font-bold text-content-primary truncate">
                                  {patient.name}
                                </span>
                                <span className="text-[13px] font-mono font-bold text-content-muted shrink-0">
                                  {patient.gender}/
                                  {patient.birthday
                                    ? patient.birthday.substring(2)
                                    : "00.01.01"}
                                </span>
                              </label>
                            );
                          })}
                        </div>
                      </div>
                    );
                  })}
                </div>
              );
            })
          )}
        </div>

        <div className="border-t border-border-subtle px-7 py-5 flex items-center justify-between gap-3 bg-white">
          <span className="text-[13px] font-semibold text-content-tertiary">
            {selectedIds.size}명 선택됨
          </span>
          <Button
            type="button"
            variant="brand"
            size="lg"
            onClick={handleSave}
          >
            저장
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
