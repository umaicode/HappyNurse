"use client";

import { useMemo, useState } from "react";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { PatientSidebar } from "@/features/patient/components/PatientSidebar";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import { EMRGrid } from "./EMRGrid";
import { RightPanel } from "./RightPanel";
import { AssignPatientModal } from "./AssignPatientModal";
import { usePatientDetail } from "../hooks/usePatientDetail";

export function DashboardView() {
  const [isLeftOpen, setIsLeftOpen] = useState(true);
  const [isRightOpen, setIsRightOpen] = useState(true);
  const [isAssignOpen, setIsAssignOpen] = useState(false);
  // 사용자가 명시적으로 선택한 환자 ID (null = 자동 선택 모드).
  const [overridePatientId, setOverridePatientId] = useState<number | null>(
    null,
  );
  // 첫 데이터 로드 시 자동으로 담당 환자 모달을 한 번 띄우기 위한 플래그.
  const [hasCheckedAssignment, setHasCheckedAssignment] = useState(false);

  const wardPatientsQuery = useWardPatients();
  const patients = wardPatientsQuery.data ?? [];

  // 담당 환자 우선, 없으면 첫 환자, 환자 자체가 없으면 null.
  const fallbackPatientId = useMemo<number | null>(() => {
    const firstMine = patients.find((p) => p.isMyPatient);
    if (firstMine) return firstMine.patientId;
    return patients[0]?.patientId ?? null;
  }, [patients]);

  // 사용자 선택값이 환자 목록에 여전히 존재하면 그걸, 아니면 fallback.
  const selectedPatientId = useMemo<number | null>(() => {
    if (
      overridePatientId !== null &&
      patients.some((p) => p.patientId === overridePatientId)
    ) {
      return overridePatientId;
    }
    return fallbackPatientId;
  }, [overridePatientId, fallbackPatientId, patients]);

  // 같은 patientId 로 EMRGrid 도 usePatientDetail 을 호출한다 — TanStack Query 캐시 공유로 fetch 는 한 번만 일어난다.
  const patientDetailQuery = usePatientDetail(selectedPatientId);
  const selectedEncounterId = patientDetailQuery.data?.encounterId ?? null;

  // 데이터 로드 직후 한 번만: 담당 환자가 한 명도 없으면 모달 자동 오픈.
  if (
    !hasCheckedAssignment &&
    !wardPatientsQuery.isPending &&
    wardPatientsQuery.isSuccess
  ) {
    setHasCheckedAssignment(true);
    if (patients.length > 0 && !patients.some((p) => p.isMyPatient)) {
      setIsAssignOpen(true);
    }
  }

  return (
    <>
      <DashboardLayout
        isLeftOpen={isLeftOpen}
        isRightOpen={isRightOpen}
        onToggleLeft={() => setIsLeftOpen((prev) => !prev)}
        onOpenRight={() => setIsRightOpen(true)}
        onToggleRight={() => setIsRightOpen((prev) => !prev)}
        onOpenAssignModal={() => setIsAssignOpen(true)}
        sidebar={
          <PatientSidebar
            patients={patients}
            isLoading={wardPatientsQuery.isPending}
            selectedPatientId={selectedPatientId}
            onSelectPatient={setOverridePatientId}
            onOpenAssignModal={() => setIsAssignOpen(true)}
          />
        }
        mainGrid={<EMRGrid patientId={selectedPatientId} />}
        actionPanel={<RightPanel encounterId={selectedEncounterId} />}
      />
      <AssignPatientModal
        patients={patients}
        isOpen={isAssignOpen}
        onClose={() => setIsAssignOpen(false)}
      />
    </>
  );
}
