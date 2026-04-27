"use client";

import { useCallback, useEffect, useState } from "react";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { PatientSidebar } from "@/features/patient/components/PatientSidebar";
import { EMRGrid } from "./EMRGrid";
import { RightPanel } from "./RightPanel";
import { MOCK_WARDS } from "@/mockup/wards";
import { loadWards, saveWards } from "@/lib/ward-assignments";
import type { Ward } from "@/features/patient/types/patient";

export function DashboardView() {
  const [isLeftOpen, setIsLeftOpen] = useState(true);
  const [isRightOpen, setIsRightOpen] = useState(true);
  // SSR 시에는 MOCK_WARDS, 클라이언트 마운트 이후 localStorage 병합본으로 교체한다.
  const [wards, setWards] = useState<Ward[]>(MOCK_WARDS);

  useEffect(() => {
    setWards(loadWards());
  }, []);

  const currentUser =
    typeof window !== "undefined"
      ? localStorage.getItem("currentUser") || "김영희"
      : "김영희";

  const assignPatientsToCurrentUser = useCallback(
    (patientIds: string[]) => {
      const targetSet = new Set(patientIds);
      setWards((prev) => {
        const next = prev.map((ward) => ({
          ...ward,
          rooms: ward.rooms.map((room) => ({
            ...room,
            patients: room.patients.map((patient) => {
              const shouldAssign = targetSet.has(patient.id);
              const currentlyMine = patient.assignedNurse === currentUser;
              if (shouldAssign && !currentlyMine) {
                return { ...patient, assignedNurse: currentUser };
              }
              if (!shouldAssign && currentlyMine) {
                return { ...patient, assignedNurse: "" };
              }
              return patient;
            }),
          })),
        }));
        saveWards(next);
        return next;
      });
    },
    [currentUser],
  );

  return (
    <DashboardLayout
      isLeftOpen={isLeftOpen}
      isRightOpen={isRightOpen}
      onOpenLeft={() => setIsLeftOpen(true)}
      onOpenRight={() => setIsRightOpen(true)}
      wards={wards}
      currentUser={currentUser}
      onAssignPatients={assignPatientsToCurrentUser}
      sidebar={
        <PatientSidebar wards={wards} onCollapse={() => setIsLeftOpen(false)} />
      }
      mainGrid={<EMRGrid />}
      actionPanel={<RightPanel onCollapse={() => setIsRightOpen(false)} />}
    />
  );
}
