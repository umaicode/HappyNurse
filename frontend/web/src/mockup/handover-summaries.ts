import type { HandoverSummary } from "@/features/handover/types/handover";

export const HANDOVER_SUMMARIES: HandoverSummary[] = [
  {
    patientId: "p1",
    headline:
      "🛠️ 급성 충수염(Appendicitis) 수술 대기 중. 우하복부 통증 NRS 7에서 2로 조절, NPO 유지 상태.",
    keyIssues: [
      "11:45 RLQ 심한 통증(NRS 7) 호소하여 Tridol 1amp IV 투여 후 NRS 2로 호전",
      "14:20 복부 CT 결과 급성 충수염 확인, 수술 동의서 징구 완료",
      "21:45 수술 전 처치(항생제 반응검사·제모) 및 체크리스트 완료",
    ],
    watchPoints: [
      "NPO 유지 상태 재확인 (수술 전 금식)",
      "수액 N/S 1L 80cc/hr 잔량 200cc — 1시간 내 교체 필요",
      "낙상 고위험군: 야간 보행 시 간호사 동행 권고",
    ],
    vitalsNote: "BT 37.0~37.2℃, HR 78~88, BP 128/82 — 전반 안정적",
    sourceRecordIds: [4, 7, 13, 103],
    generatedAt: "2026-04-24 23:30",
    model: "AI 요약 엔진",
  },
  {
    patientId: "p2",
    headline:
      "🛠️ 위절제술 후 회복기. JP bag 배액 안정, 조기 이상 2회 독려 완료.",
    keyIssues: [
      "08:30 JP bag serosanguineous 양상 50cc 배액, 주입 부위 특이사항 없음",
      "12:30 점심 Soft diet 1/2 섭취, 오심·구토 없음",
      "14:00 조기 이상(Ambulation) 2회 독려, 어지러움 호소 없음",
    ],
    watchPoints: [
      "JP bag 배액 양·양상 교대 시 재확인",
      "식사 섭취량 및 복부 팽만감 모니터링",
      "수술 부위 드레싱 상태 야간 라운딩 시 점검",
    ],
    vitalsNote: "BP 118/72, HR 78, BT 36.6℃ — 안정적",
    sourceRecordIds: [200, 202, 203, 204],
    generatedAt: "2026-04-24 23:30",
    model: "AI 요약 엔진",
  },
  {
    patientId: "p3",
    headline:
      "🛠️ 폐렴 치료 중. SpO2 하강 이력 있으며 Nasal Prong 적용 후 회복, 지속 모니터링 필요.",
    keyIssues: [
      "09:00 SpO2 91%(RA) 하강 및 화농성 가래 심화 — 담당의 노티",
      "09:30 Nebulizer 1차 시행 후 가래 배출 원활",
      "11:00 Nasal Prong 2L 적용 시작 후 SpO2 96%까지 회복",
    ],
    watchPoints: [
      "Nasal Prong 유지 상태 및 SpO2 지속 모니터링",
      "2시간 간격 체위 변경 — 천골 부위 발적 재확인",
      "Nebulizer 치료 일정(3차) 교대 시 연결 확인",
    ],
    vitalsNote: "SpO2 Nasal Prong 적용 후 96% 유지. 가래 배출 원활",
    sourceRecordIds: [300, 302, 304],
    generatedAt: "2026-04-24 23:30",
    model: "AI 요약 엔진",
  },
];
