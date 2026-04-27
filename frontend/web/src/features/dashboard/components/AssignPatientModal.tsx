"use client";

import { useEffect, useMemo, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import type { Ward } from "@/features/patient/types/patient";

interface AssignPatientModalProps {
  wards: Ward[];
  currentUser: string;
  isOpen: boolean;
  onClose: () => void;
  onAssignPatients: (patientIds: string[]) => void;
}

export function AssignPatientModal({
  wards,
  currentUser,
  isOpen,
  onClose,
  onAssignPatients,
}: AssignPatientModalProps) {
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

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

  // 모달이 열릴 때마다 현재 담당 상태로 초기화한다.
  useEffect(() => {
    if (isOpen) {
      setSelectedIds(new Set(initiallyAssignedIds));
    }
  }, [isOpen, initiallyAssignedIds]);

  const togglePatient = (patientId: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(patientId)) next.delete(patientId);
      else next.add(patientId);
      return next;
    });
  };

  const handleConfirm = () => {
    onAssignPatients(Array.from(selectedIds));
    onClose();
  };

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(open) => {
        if (!open) onClose();
      }}
    >
      <DialogContent className="sm:max-w-[640px] max-h-[85vh] overflow-hidden flex flex-col rounded-2xl p-0 border border-border-base bg-white shadow-2xl">
        <DialogHeader className="px-7 pt-7 pb-4 border-b border-border-subtle">
          <DialogTitle className="text-2xl font-bold text-[var(--color-sub-primary)]">
            담당 환자 설정
          </DialogTitle>
          <DialogDescription className="text-[14px] text-content-tertiary">
            담당할 환자를 선택하세요. 선택 해제한 환자는 담당에서 제외됩니다.
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto px-5 py-4">
          {wards.map((ward) => {
            if (ward.rooms.length === 0) return null;
            return (
              <div key={ward.id} className="flex flex-col gap-2 pb-4 last:pb-0">
                <div className="px-2 text-[14px] font-black text-content-secondary tracking-wider uppercase">
                  {ward.name}
                </div>
                {ward.rooms.map((room) => (
                  <div key={room.id} className="flex flex-col">
                    <div className="px-2 py-1.5 text-[13px] font-bold text-content-muted border-b border-border-subtle mb-1 tracking-wider uppercase">
                      {room.name}
                    </div>
                    <div className="flex flex-col">
                      {room.patients.map((patient) => {
                        const checked = selectedIds.has(patient.id);
                        const checkboxId = `assign-${patient.id}`;
                        return (
                          <label
                            key={patient.id}
                            htmlFor={checkboxId}
                            className="flex items-center justify-between w-full px-3 py-2.5 rounded-md hover:bg-[var(--color-surface-hover)] transition-all cursor-pointer select-none"
                          >
                            <div className="flex items-center gap-3 min-w-0">
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
                            </div>
                            <span className="text-[13px] font-semibold text-content-tertiary shrink-0">
                              {patient.assignedNurse
                                ? `담당 ${patient.assignedNurse}`
                                : "미배정"}
                            </span>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            );
          })}
        </div>

        <div className="border-t border-border-subtle px-7 py-5 flex items-center justify-between gap-3 bg-white">
          <span className="text-[13px] font-semibold text-content-tertiary">
            {selectedIds.size}명 선택됨
          </span>
          <div className="flex items-center gap-2">
            <Button type="button" variant="outline" size="lg" onClick={onClose}>
              취소
            </Button>
            <Button
              type="button"
              variant="brand"
              size="lg"
              onClick={handleConfirm}
            >
              확인
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
