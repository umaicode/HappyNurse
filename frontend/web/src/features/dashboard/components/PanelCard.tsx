/**
 * RightPanel 안의 카드 베이스 — STTPanel(의사 오더), PatientAlerts(알림),
 * IVTimerPanel(수액 타이머) 가 공유하는 카드 외관.
 *
 * - 흰 배경 + 옅은 보더 + rounded-xl + shadow-sm
 * - 호버 시 보더가 brand-primary 톤으로
 * - p-3 + flex-col gap-2
 *
 * 좌측 강조 보더(`accentBorder`) 는 sourceType/severity 별 강조 색을 받는 옵션.
 */
import { type HTMLAttributes, type ReactNode } from "react";
import { cn } from "@/lib/utils";

interface PanelCardProps extends HTMLAttributes<HTMLDivElement> {
  // 좌측 4px 강조 보더 (예: status warning 임박, isChanged 등). undefined 면 일반 카드.
  accentBorderClass?: string;
  // 카드 전체 톤 변형 (예: dim/highlight). undefined 면 기본.
  variantClass?: string;
  children: ReactNode;
}

export function PanelCard({
  accentBorderClass,
  variantClass,
  className,
  children,
  ...rest
}: PanelCardProps) {
  return (
    <div
      {...rest}
      className={cn(
        "relative bg-white rounded-xl border border-border-base shadow-sm p-3 flex flex-col gap-2 transition-all hover:border-brand-primary/30",
        accentBorderClass,
        variantClass,
        className,
      )}
    >
      {children}
    </div>
  );
}
