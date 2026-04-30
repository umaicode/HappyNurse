import { Suspense } from "react";
import Nfc from "@/components/patient/nfc";

export default function PatientPage() {
  return (
    <Suspense>
      <Nfc />
    </Suspense>
  );
}
