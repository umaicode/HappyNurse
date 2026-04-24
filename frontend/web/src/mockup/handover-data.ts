import type { HandoverPatient } from "@/features/handover/types/handover";

export const HANDOVER_PATIENTS: HandoverPatient[] = [
  {
    id: "p1",
    name: "김가민",
    patientNo: "2026-00125",
    birthDate: "1999.05.20 (25세/F)",
    room: "7101-01",
    mainSymptom: "Acute Appendicitis (급성 충수염)",
    assignedNurse: "김영희",
    recentVitals: { status: "abnormal", detail: "BT 38.2℃ (14:30)" },
  },
  {
    id: "p2",
    name: "이철수",
    patientNo: "2026-00342",
    birthDate: "1982.11.03 (42세/M)",
    room: "7101-02",
    mainSymptom: "Post-op Gastrectomy (위절제술 후)",
    assignedNurse: "김영희",
    recentVitals: { status: "normal", detail: "안정적" },
  },
  {
    id: "p3",
    name: "박영희",
    patientNo: "2026-00088",
    birthDate: "1956.07.15 (68세/F)",
    room: "7101-03",
    mainSymptom: "Pneumonia (폐렴)",
    assignedNurse: "김영희",
    recentVitals: { status: "abnormal", detail: "SpO2 91% (RA, 13:00)" },
  },
];
