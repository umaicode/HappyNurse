'use client'

import { useState } from "react";
import { DashboardLayout } from "@/components/layout/DashboardLayout";
import { PatientSidebar } from "@/features/patient/components/PatientSidebar";
import { EMRGrid } from "./EMRGrid";
import { RightPanel } from "./RightPanel";

export function DashboardView() {
  const [isLeftOpen, setIsLeftOpen] = useState(true);
  const [isRightOpen, setIsRightOpen] = useState(true);

  return (
    <DashboardLayout
      isLeftOpen={isLeftOpen}
      isRightOpen={isRightOpen}
      onOpenLeft={() => setIsLeftOpen(true)}
      onOpenRight={() => setIsRightOpen(true)}
      sidebar={<PatientSidebar onCollapse={() => setIsLeftOpen(false)} />}
      mainGrid={<EMRGrid />}
      actionPanel={<RightPanel onCollapse={() => setIsRightOpen(false)} />}
    />
  );
}
