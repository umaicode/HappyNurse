import type { HandoverShiftOverview } from "@/features/handover/types/handover";

// 담당 환자 통합 레포트. 실 API 연동 후에는 담당 환자 목록 기반으로 서버에서 생성된다.
export const HANDOVER_SHIFT_OVERVIEW: HandoverShiftOverview = {
  summary:
    "🛠️ 이번 교대에서 가장 주의가 필요한 환자는 김가민 환자로, 급성 충수염 진단 후 수술 대기 중이며 NPO 유지 상태입니다. 오후 동안 진통제 투여로 통증이 NRS 7에서 2로 조절되었으나 수액 N/S 1L 잔량이 200cc로 1시간 내 교체가 필요합니다.\n\n박영희 환자는 폐렴 치료 중이며 Nasal Prong 2L 적용 후 SpO2가 96%까지 회복되었습니다. 2시간 간격 체위 변경 시 천골 부위 경미한 발적이 관찰되어 재확인이 필요합니다.\n\n이철수 환자는 위절제술 후 회복기로 전반적으로 안정적이나 JP bag 배액 양상(serosanguineous, 50cc)을 교대 시 재확인해 주세요. 최민호 환자와 한지민 환자는 전반 바이탈 안정적이며 루틴 관찰 유지로 충분합니다.\n\n전체적으로 낙상 고위험군이 1명(김가민) 포함되어 있어 야간 보행 시 동행 권고가 유지되어야 합니다.",
  generatedAt: "2026-04-24 23:30",
  model: "AI 요약 엔진",
};
