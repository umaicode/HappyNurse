'use client'

import { useCallback, useState } from "react";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { PatientSidebar } from "@/features/patient/components/PatientSidebar";
import { EMRGrid } from "./EMRGrid";
import { RightPanel } from "./RightPanel";
import { MOCK_WARDS, type Ward } from "@/mockup/wards";

export function DashboardView() {
  const [isLeftOpen, setIsLeftOpen] = useState(true);
  const [isRightOpen, setIsRightOpen] = useState(true);
  const [wards, setWards] = useState<Ward[]>(MOCK_WARDS);

  const currentUser =
    typeof window !== "undefined"
      ? localStorage.getItem("currentUser") || "김영희"
      : "김영희";

  const assignPatientToCurrentUser = useCallback(
    (patientId: string) => {
      setWards((prev) =>
        prev.map((ward) => ({
          ...ward,
          rooms: ward.rooms.map((room) => ({
            ...room,
            patients: room.patients.map((patient) =>
              patient.id === patientId
                ? { ...patient, assignedNurse: currentUser }
                : patient,
            ),
          })),
        })),
      );
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
      onAssignPatient={assignPatientToCurrentUser}
      sidebar={
        <PatientSidebar
          wards={wards}
          onCollapse={() => setIsLeftOpen(false)}
        />
      }
      mainGrid={<EMRGrid />}
      actionPanel={<RightPanel onCollapse={() => setIsRightOpen(false)} />}
    />
  );
}
