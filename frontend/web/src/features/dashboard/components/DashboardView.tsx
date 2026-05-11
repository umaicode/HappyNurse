"use client";

import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { PatientSidebar } from "@/features/patient/components/PatientSidebar";
import { useWardPatients } from "@/features/patient/hooks/useWardPatients";
import type { WardPatient } from "@/features/patient/types/ward-patient";
import { EMRGrid, type EMRTab } from "./EMRGrid";
import { RightPanel } from "./RightPanel";
import { AssignPatientModal } from "./AssignPatientModal";
import { usePatientDetail } from "../hooks/usePatientDetail";

// data 가 undefined 일 때 매 렌더 새 빈 배열을 만들면 useMemo deps 가 흔들려
// selectedPatientId 등 파생값이 매번 재계산된다. 모듈 스코프 배열로 reference 고정.
const EMPTY_WARD_PATIENTS: WardPatient[] = [];

export function DashboardView() {
  // 인수인계 화면의 Citation 클릭으로 진입했을 때 search param 으로 환자/일자/포커스 받음.
  // useSearchParams 를 useState 의 lazy initializer 에 넣을 수 없어 컴포넌트 본문에서 한 번 읽고 사용.
  const searchParams = useSearchParams();
  const initialPatientIdParam = searchParams.get("patientId");
  const initialFocusRecordIdParam = searchParams.get("focusRecordId");
  const initialDateParam = searchParams.get("date");

  const [isLeftOpen, setIsLeftOpen] = useState(true);
  const [isRightOpen, setIsRightOpen] = useState(true);
  const [isAssignOpen, setIsAssignOpen] = useState(false);
  // 사용자가 명시적으로 선택한 환자 ID (null = 자동 선택 모드).
  const [overridePatientId, setOverridePatientId] = useState<number | null>(
    () =>
      initialPatientIdParam !== null && initialPatientIdParam.length > 0
        ? Number(initialPatientIdParam)
        : null,
  );
  const [activeTab, setActiveTab] = useState<EMRTab>(() =>
    initialFocusRecordIdParam !== null ? "nursing" : "nursing",
  );
  const [selectedDate, setSelectedDate] = useState<Date>(() => {
    if (initialDateParam) {
      const parsed = new Date(`${initialDateParam}T00:00:00`);
      if (!Number.isNaN(parsed.getTime())) return parsed;
    }
    return new Date();
  });
  const [focusRecordId, setFocusRecordId] = useState<number | null>(() =>
    initialFocusRecordIdParam !== null && initialFocusRecordIdParam.length > 0
      ? Number(initialFocusRecordIdParam)
      : null,
  );
  // 첫 데이터 로드 시 자동으로 담당 환자 모달을 한 번 띄우기 위한 플래그.
  // search param 으로 특정 환자 진입 시엔 모달 안 띄우게 처음부터 true.
  const [hasCheckedAssignment, setHasCheckedAssignment] = useState(
    initialPatientIdParam !== null,
  );

  const wardPatientsQuery = useWardPatients();
  const patients = wardPatientsQuery.data ?? EMPTY_WARD_PATIENTS;

  // 담당 환자 우선, 없으면 첫 환자, 환자 자체가 없으면 null.
  const fallbackPatientId = useMemo<number | null>(() => {
    const firstMine = patients.find((patient) => patient.isMyPatient);
    if (firstMine) return firstMine.patientId;
    return patients[0]?.patientId ?? null;
  }, [patients]);

  // 사용자 선택값이 환자 목록에 여전히 존재하면 그걸, 아니면 fallback.
  const selectedPatientId = useMemo<number | null>(() => {
    if (
      overridePatientId !== null &&
      patients.some((patient) => patient.patientId === overridePatientId)
    ) {
      return overridePatientId;
    }
    return fallbackPatientId;
  }, [overridePatientId, fallbackPatientId, patients]);

  // 같은 patientId 로 EMRGrid 도 usePatientDetail 을 호출한다 — TanStack Query 캐시 공유로 fetch 는 한 번만 일어난다.
  const patientDetailQuery = usePatientDetail(selectedPatientId);
  const selectedEncounterId = patientDetailQuery.data?.encounterId ?? null;

  // 데이터 로드 직후 한 번만: 담당 환자가 한 명도 없으면 모달 자동 오픈.
  useEffect(() => {
    if (hasCheckedAssignment || !wardPatientsQuery.isSuccess) return;
    /* eslint-disable-next-line react-hooks/set-state-in-effect */
    setHasCheckedAssignment(true);
    if (patients.length > 0 && !patients.some((patient) => patient.isMyPatient)) {
      setIsAssignOpen(true);
    }
  }, [hasCheckedAssignment, wardPatientsQuery.isSuccess, patients]);

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
            onJumpToUnconfirmed={(patientId, recordId, occurredAt) => {
              setOverridePatientId(patientId);
              setActiveTab("nursing");
              setFocusRecordId(recordId);
              // popover 는 일자 무관 draft 를 보여주므로, 다른 일자 항목을 클릭해도
              // NursingTab 이 그 날짜로 이동하도록 selectedDate 를 함께 맞춘다.
              setSelectedDate(new Date(occurredAt));
            }}
          />
        }
        mainGrid={
          <EMRGrid
            patientId={selectedPatientId}
            activeTab={activeTab}
            onTabChange={setActiveTab}
            selectedDate={selectedDate}
            onChangeSelectedDate={setSelectedDate}
            focusRecordId={focusRecordId}
            onFocusHandled={() => setFocusRecordId(null)}
          />
        }
        actionPanel={<RightPanel encounterId={selectedEncounterId} />}
      />
      {isAssignOpen && (
        <AssignPatientModal
          patients={patients}
          onClose={() => setIsAssignOpen(false)}
        />
      )}
    </>
  );
}
