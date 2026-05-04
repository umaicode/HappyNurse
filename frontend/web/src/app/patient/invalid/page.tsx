import { Suspense } from "react";
import Invalid from "@/components/patient/invalid";

export default function InvalidPage() {
  return (
    <Suspense>
      <Invalid />
    </Suspense>
  );
}
