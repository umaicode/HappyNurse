import { MOCK_WARDS } from "@/mockup/wards";
import type { Ward } from "@/features/patient/types/ward";

const STORAGE_KEY = "ward-assignments";

type AssignmentMap = Record<string, string>; // patientId → assignedNurse

// MOCK_WARDS 를 기반으로 patientId → assignedNurse 맵을 추출한다.
function extractAssignmentMap(wards: Ward[]): AssignmentMap {
  const map: AssignmentMap = {};
  wards.forEach((ward) =>
    ward.rooms.forEach((room) =>
      room.patients.forEach((patient) => {
        map[patient.id] = patient.assignedNurse;
      }),
    ),
  );
  return map;
}

// 저장된 배정 맵을 MOCK_WARDS 위에 덧씌워서 반환한다.
function applyAssignmentMap(wards: Ward[], map: AssignmentMap): Ward[] {
  return wards.map((ward) => ({
    ...ward,
    rooms: ward.rooms.map((room) => ({
      ...room,
      patients: room.patients.map((patient) =>
        map[patient.id] !== undefined
          ? { ...patient, assignedNurse: map[patient.id] }
          : patient,
      ),
    })),
  }));
}

export function loadWards(): Ward[] {
  if (typeof window === "undefined") return MOCK_WARDS;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return MOCK_WARDS;
    const parsed = JSON.parse(raw) as AssignmentMap;
    return applyAssignmentMap(MOCK_WARDS, parsed);
  } catch {
    return MOCK_WARDS;
  }
}

export function saveWards(wards: Ward[]): void {
  if (typeof window === "undefined") return;
  const map = extractAssignmentMap(wards);
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(map));
}

export function clearWardAssignments(): void {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(STORAGE_KEY);
}
