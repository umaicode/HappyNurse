import { Suspense } from "react";
import Auth from "@/components/patient/auth";

export default function PatientPage() {
  return (
    <Suspense fallback={null}>
      <Auth />
    </Suspense>
  );
}
