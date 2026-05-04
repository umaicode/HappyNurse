import { Suspense } from "react";
import Complete from "@/components/patient/complete";

export default function CompletePage() {
  return (
    <Suspense>
      <Complete />
    </Suspense>
  );
}
