// EMR 관련 목업 데이터

export const NURSES = [
  "김영희",
  "이수진",
  "박민지",
  "최지원",
  "김의사",
];

export const HOURS = [
  "전체 시간",
  ...Array.from({ length: 24 }).map(
    (_, i) => `${i.toString().padStart(2, "0")}시`,
  ),
];

export interface EmrRecord {
  id: number;
  time: string;
  category: string;
  content: string;
  status: string;
  writer: string;
  isConfirmed: boolean;
  isHandover?: boolean;
  isAISuggested?: boolean;
  source?: string;
  drug?: {
    code: string;
    name: string;
    dose: string;
    unit: string;
    frequency: string;
    method: string;
  };
  patientId?: string;
}

export const INITIAL_RECORDS: EmrRecord[] = [
  {
    id: 1,
    time: "09:30",
    category: "활력징후",
    content:
      "BP 130/80, HR 82, BT 37.1, RR 20. 환자 특이 호소사항 없음.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
  },
  {
    id: 2,
    time: "10:15",
    category: "투약",
    content:
      "아세트아미노펜 500mg (PO) 투여함. 투여 목적 및 부작용(위장장애 등)에 대해 설명함.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
  },
  {
    id: 3,
    time: "11:00",
    category: "처치",
    content:
      "Foley catheter insertion 시행함. 16Fr 사용하였으며 맑은 소변 300cc 배액됨. 삽입 부위 발적이나 부종 관찰되지 않음. 환자에게 도뇨관 유지 목적과 주의사항(당기지 않기, 소변백 높이 유지 등) 교육함.",
    status: "completed",
    writer: "박민지",
    isConfirmed: true,
  },
  {
    id: 4,
    time: "11:45",
    category: "간호기록",
    content:
      "환자 분이 지속적인 우하복부 통증을 호소함.\nNRS 7점으로 측정되며, 식은땀을 흘리고 얼굴이 창백한 상태임.\nV/S 측정 결과 BP 140/90, HR 102, BT 37.5도로 확인됨.\n주치의 김의사에게 유선으로 노티하였으며, 지시에 따라 진통제(Tridol 1amp IV) 투여 및 복부 CT 촬영을 위해 휠체어로 영상의학과 이동 예정임.\n통증 양상은 찌르는 듯한 통증이며 방사통은 없다고 함. 보호자에게 현재 상황 및 향후 계획 설명하고 안심시킴. 진통제 투여 후 30분 뒤 통증 재평가 계획임.",
    status: "urgent",
    writer: "김영희",
    isConfirmed: true,
    isHandover: true,
  },
  {
    id: 5,
    time: "12:30",
    category: "식사",
    content: "일반식 1/2 섭취. 오심이나 구토 증상 없음.",
    status: "completed",
    writer: "최지원",
    isConfirmed: true,
  },
  {
    id: 6,
    time: "13:00",
    category: "활력징후",
    content:
      "BP 125/75, HR 78, BT 36.8. 통증 재평가 결과 NRS 2점으로 감소함. 환자 편안하게 수면 취하는 모습 관찰됨.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
  },
  {
    id: 7,
    time: "14:20",
    category: "의사기록",
    content:
      "복부 CT 결과상 급성 충수염(Acute Appendicitis) 소견 관찰됨.\n보호자 및 환자에게 수술 필요성 설명하고 수술 동의서 징구함.\nNPO 유지 지시함. 수액 요법(N/S 1L 80cc/hr) 시작함.",
    status: "pending",
    writer: "김의사",
    isConfirmed: false,
    isHandover: true,
  },
  {
    id: 8,
    time: "15:30",
    category: "활력징후",
    content:
      "BP 132/85, HR 88, BT 37.2. 수액 N/S 1L 80cc/hr 유지 중이며 잔량 600cc 확인됨. 주입 부위 발적이나 부종 없음.",
    status: "completed",
    writer: "이수진",
    isConfirmed: true,
  },
  {
    id: 9,
    time: "16:45",
    category: "처치",
    content:
      "복부 통증 부위 얼음찜질(Ice bag) 적용함. 환자에게 냉요법의 목적과 주의사항 설명함. 통증 양상 변화 관찰 중.",
    status: "completed",
    writer: "박민지",
    isConfirmed: true,
  },
  {
    id: 10,
    time: "18:00",
    category: "투약",
    content:
      "저녁 식전 약(위장운동조절제) PO 투여함. 환자 식사 전 NPO 유지 여부 재확인함.",
    status: "completed",
    writer: "이수진",
    isConfirmed: true,
  },
  {
    id: 11,
    time: "19:30",
    category: "간호기록",
    content: "저녁 식사 제공 시 환자 섭취 거부함. 담당의 보고.",
    status: "completed",
    writer: "문현지",
    isConfirmed: true,
    isAISuggested: true,
  },
  {
    id: 12,
    time: "20:30",
    category: "활력징후",
    content:
      "BP 128/82, HR 84, BT 37.0. 환자 안정을 취하고 있으며 통증은 NRS 3점 정도로 조절되고 있음.",
    status: "completed",
    writer: "최지원",
    isConfirmed: true,
  },
  {
    id: 13,
    time: "21:45",
    category: "처치",
    content:
      "수술 전 처치(항생제 반응 검사, 수술 부위 제모 등) 완료함. 수술 전 준비 리스트 체크 완료.",
    status: "completed",
    writer: "이수진",
    isConfirmed: true,
    isHandover: true,
  },
  {
    id: 14,
    time: "23:00",
    category: "간호기록",
    content:
      "야간 라운딩 결과 환자 수면 중임. 수액 주입 원활하며 특이 소견 없음.",
    status: "completed",
    writer: "박민지",
    isConfirmed: true,
  },
  {
    id: 99,
    time: "15:45",
    category: "간호기록",
    content: "환자 자가 배뇨 후 잔뇨감 없다고 함. 하복부 팽만감 감소됨.",
    status: "completed",
    writer: "이수진", // Another nurse
    isConfirmed: false, // Unconfirmed
  },
  // --- 본인(김영희) 미확정 기록 : 퀵 수정(AI 제안) 시연용 ---
  {
    id: 100,
    time: "22:15",
    category: "간호기록",
    content:
      "환자 우하복부 압통 지속됨. 충우염 의심되어 담당의 노티함. McBurney point 양성 소견 관찰됨.",
    status: "pending",
    writer: "김영희",
    isConfirmed: false, // → '충우염' 단어에 퀵 수정 팝오버 노출 (→ 충수염)
  },
  {
    id: 101,
    time: "22:40",
    category: "투약",
    content:
      "두통 호소하여 타이래놀 1정 PO 투여함. 투여 후 30분 뒤 통증 재평가 예정.",
    status: "pending",
    writer: "김영희",
    isConfirmed: false, // → '타이래놀' 단어에 퀵 수정 팝오버 노출 (→ 타이레놀)
  },
  {
    id: 102,
    time: "23:30",
    category: "처치",
    content:
      "바이탈 재측정 결과 안정적임. 도뇨관 유지 중이며 배액 양상 정상임. 수액 잔량 200cc 확인됨.",
    status: "pending",
    writer: "김영희",
    isConfirmed: false, // → '바이탈/도뇨관/수액' 모두 퀵 수정 제안 (표준 약어 변환 예시)
  },
  // --- NFC 태깅 기록 샘플 (약물) ---
  {
    id: 103,
    time: "23:55",
    category: "투약",
    content: "",
    status: "pending",
    writer: "김영희",
    isConfirmed: false,
    source: "nfc",
    drug: {
      code: "MD1000",
      name: "Acetaminophen 1000mg Tab.",
      dose: "1000",
      unit: "mg",
      frequency: "2",
      method: "PO",
    },
  },
  // --- p2 이철수 (위절제술 후) 기록 ---
  {
    id: 200,
    patientId: "p2",
    time: "08:30",
    category: "처치",
    content: "JP bag 배액 양상 serosanguineous하며 50cc 배액됨. 주입 부위 발적/부종 없음.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
    isHandover: true,
  },
  {
    id: 201,
    patientId: "p2",
    time: "10:00",
    category: "투약",
    content: "아침 식후 30분에 위장운동조절제 PO 투여함.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
  },
  {
    id: 202,
    patientId: "p2",
    time: "12:30",
    category: "식사",
    content: "점심 Soft diet 1/2 섭취함. 오심/구토 없음.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
  },
  {
    id: 203,
    patientId: "p2",
    time: "14:00",
    category: "간호기록",
    content: "조기 이상(Ambulation) 2회 독려 완료. 보행 시 어지러움 호소 없음.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
    isAISuggested: true,
  },
  {
    id: 204,
    patientId: "p2",
    time: "16:00",
    category: "활력징후",
    content: "BP 118/72, HR 78, BT 36.6℃. 전반적으로 안정적임.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
  },
  // --- p3 박영희 (폐렴) 기록 ---
  {
    id: 300,
    patientId: "p3",
    time: "09:00",
    category: "활력징후",
    content: "SpO2 91% (RA). 기침 및 화농성 가래 심해 담당의 노티함.",
    status: "urgent",
    writer: "김영희",
    isConfirmed: true,
    isHandover: true,
  },
  {
    id: 301,
    patientId: "p3",
    time: "09:30",
    category: "처치",
    content: "Nebulizer 치료 1차 시행함. 시행 후 가래 배출 원활함.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
  },
  {
    id: 302,
    patientId: "p3",
    time: "11:00",
    category: "처치",
    content: "Nasal Prong 2L 적용 시작함. 적용 후 SpO2 96%까지 회복됨.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
    isHandover: true,
  },
  {
    id: 303,
    patientId: "p3",
    time: "13:00",
    category: "처치",
    content: "Nebulizer 2차 치료 시행함.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
  },
  {
    id: 304,
    patientId: "p3",
    time: "15:00",
    category: "간호기록",
    content: "2시간 간격 체위 변경 완료. 천골 부위 경미한 발적 관찰됨.",
    status: "completed",
    writer: "김영희",
    isConfirmed: true,
    isAISuggested: true,
  },
];

// 처방 코드 체계 (원내 코드: 2-letter prefix + 4자리 숫자)
// - MD: 경구/투약(Medication)
// - IV: 주사/수액(Injection)
// - LB: 검체검사(Laboratory)
// - RD: 영상검사(Radiology)
// - OR: 일반지시(Order)
export const INITIAL_ORDERS = [
  {
    id: 1,
    category: "수액",
    code: "IV0901",
    name: "0.9% Sodium Chloride Inj. 1000ml",
    dose: "1000",
    frequency: "1",
    unit: "bag",
    method: "IV",
    status: "진행",
    remarks: "80cc/hr 유지",
  },
  {
    id: 2,
    category: "지시",
    code: "OR0001",
    name: "금식 (수술 전)",
    dose: "-",
    frequency: "-",
    unit: "-",
    method: "-",
    status: "접수",
    remarks: "자정부터 금식 유지",
  },
  {
    id: 3,
    category: "투약",
    code: "MD0500",
    name: "Acetaminophen 500mg Tab.",
    dose: "1",
    frequency: "3",
    unit: "tab",
    method: "PO",
    status: "완료",
    remarks: "식후 30분",
  },
  {
    id: 4,
    category: "LIS",
    code: "LB2501",
    name: "Complete Blood Count (CBC)",
    dose: "-",
    frequency: "1",
    unit: "-",
    method: "-",
    status: "검사중",
    remarks: "오전 6시 채혈",
  },
  {
    id: 5,
    category: "영상",
    code: "RD0449",
    name: "Abdomen CT (with contrast)",
    dose: "-",
    frequency: "1",
    unit: "-",
    method: "-",
    status: "검사중",
    remarks: "동의서 확인 요망",
  },
];

export interface PatientAlert {
  id: number;
  patientId: string;
  patientName: string;
  room: string;
  time: string;
  severity: "critical" | "warning" | "info";
  category: string;
  message: string;
  status: "unread" | "acknowledged" | "resolved";
}

export const INITIAL_PATIENT_ALERTS: PatientAlert[] = [
  {
    id: 1,
    patientId: "PT0001",
    patientName: "김가민",
    room: "7101호",
    time: "22:10",
    severity: "critical",
    category: "바이탈",
    message: "SpO2 92% 로 하강. 산소 포화도 경고 임계치 도달.",
    status: "unread",
  },
  {
    id: 2,
    patientId: "PT0001",
    patientName: "김가민",
    room: "7101호",
    time: "21:45",
    severity: "warning",
    category: "투약",
    message: "Tridol 1amp IV 투여 후 30분 경과. 통증 재평가 필요.",
    status: "unread",
  },
  {
    id: 3,
    patientId: "PT0001",
    patientName: "김가민",
    room: "7101호",
    time: "20:30",
    severity: "info",
    category: "간호",
    message: "수액 잔량 200cc. 1시간 내 교체 예정.",
    status: "acknowledged",
  },
  {
    id: 4,
    patientId: "PT0001",
    patientName: "김가민",
    room: "7101호",
    time: "19:15",
    severity: "warning",
    category: "낙상",
    message: "낙상 고위험군. 야간 보행 시 간호사 동행 권고.",
    status: "acknowledged",
  },
  {
    id: 5,
    patientId: "PT0001",
    patientName: "김가민",
    room: "7101호",
    time: "18:00",
    severity: "info",
    category: "식이",
    message: "NPO 유지 중. 수술 전 금식 지시 재확인.",
    status: "resolved",
  },
  {
    id: 6,
    patientId: "PT0002",
    patientName: "박영희",
    room: "7101호",
    time: "14:20",
    severity: "warning",
    category: "배액",
    message: "JP bag 배액량 증가(50→120cc). 담당의 노티 필요.",
    status: "unread",
  },
  {
    id: 7,
    patientId: "PT0003",
    patientName: "최민호",
    room: "7101호",
    time: "11:05",
    severity: "critical",
    category: "호흡",
    message: "SpO2 91% (RA). Nasal Prong 적용 검토.",
    status: "resolved",
  },
  {
    id: 8,
    patientId: "PT0003",
    patientName: "최민호",
    room: "7101호",
    time: "10:30",
    severity: "warning",
    category: "투약",
    message: "Levofloxacin IV 투여 예정 시각 지연(30분).",
    status: "unread",
  },
  {
    id: 9,
    patientId: "PT0004",
    patientName: "한지민",
    room: "7102호",
    time: "09:45",
    severity: "info",
    category: "검사",
    message: "아침 채혈(CBC) 완료. 결과 대기 중.",
    status: "acknowledged",
  },
  {
    id: 10,
    patientId: "PT0004",
    patientName: "한지민",
    room: "7102호",
    time: "08:20",
    severity: "critical",
    category: "바이탈",
    message: "HR 115 로 상승. 빈맥 경보 발생.",
    status: "unread",
  },
];

export const DEFAULT_PATIENT_INFO = {
  name: "김가민",
  genderAge: "F/25",
  id: "PT0001",
  room: "7101호",
  department: "소화기내과",
  doctor: "김의사",
  date: "2026.04.10 (D+4)",
  insurance: "건강보험 (국민)",
  cc: "우하복부 통증 (RLQ pain), 오심",
  memo: "낙상 고위험군 (보행 시 주의 요망), 보호자 상주 중",
  birthday: "1999.05.20",
  address:
    "서울특별시 강남구 테헤란로 123 해피아파트 101동 202호",
};