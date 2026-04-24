import { Suspense } from "react";
import Help from "@/components/patient/help";

export default function HelpPage() {
  return (
    <Suspense fallback={null}>
      <Help />
    </Suspense>
  );
}
