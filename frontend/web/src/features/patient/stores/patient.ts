import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import type { PatientInfo } from "@/features/auth/types";

interface PatientStore {
  patient: PatientInfo | null;
  setPatient: (patient: PatientInfo) => void;
  reset: () => void;
}

export const usePatientStore = create<PatientStore>()(
  persist(
    (set) => ({
      patient: null,
      setPatient: (patient) => set({ patient }),
      reset: () => set({ patient: null }),
    }),
    {
      name: "patient-session",
      storage: createJSONStorage(() => sessionStorage),
    },
  ),
);
