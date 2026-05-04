import { Suspense } from "react";
import Auth from "@/components/patient/auth";

export default function PatientVerifyPage() {
  return (
    <Suspense>
      <Auth />
    </Suspense>
  );
}
