/**
 * 의사 오더 (MedicationOrder) 타입.
 *
 * - [간호사용 웹] EMRGrid 의 "의사 오더" 탭 (OrderTab) · RightPanel 의 "의사 오더" 검색 패널 (STTPanel)
 *
 * 백엔드 응답 wrapper: { success, message, errorCode, data } — api 함수에서 data 만 추출.
 */

// 백엔드 OrderType / OrderStatus enum 과 1:1 일치 (대소문자 포함).
export type OrderType =
  | "MEDICATION"
  | "INSTRUCTION"
  | "FLUID"
  | "TREATMENT"
  | "LIS"
  | "IMAGE";

export type OrderStatus =
  | "active"
  | "on_hold"
  | "completed"
  | "stopped"
  | "draft";

export interface MedicationOrderItem {
  medicationOrderId: number;
  orderType: OrderType;
  orderCode: string;
  orderName: string;
  // BigDecimal 직렬화 — 정밀도 보존 위해 string 으로 받는 게 안전하지만
  // 현재 백엔드는 number 로 직렬화 중이라 number 로 받는다.
  dose: number;
  frequency: number;
  doseUnit: string;
  route: string;
  remarks: string | null;
  status: OrderStatus;
  // ISO datetime
  dateWritten: string;
  prescriberId: number;
  prescriberName: string;
  createdAt: string;
  updatedAt: string;
}

export interface MedicationOrderListResponse {
  encounterId: number;
  patientId: number;
  patientName: string;
  totalCount: number;
  orders: MedicationOrderItem[];
}

// UI 표시용 한글 라벨 (백엔드 enum → 한글). 룰: 라벨 매핑 상수는 도메인 types 에.
export const ORDER_TYPE_LABEL: Record<OrderType, string> = {
  MEDICATION: "투약",
  INSTRUCTION: "지시",
  FLUID: "수액",
  TREATMENT: "처치",
  LIS: "검사",
  IMAGE: "영상",
};

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  active: "진행",
  on_hold: "보류",
  completed: "완료",
  stopped: "중단",
  draft: "임시",
};

// 상태 색상 톤 (status 디자인 토큰 — globals.css 에 정의).
export const ORDER_STATUS_TONE: Record<OrderStatus, string> = {
  active: "text-status-active",
  on_hold: "text-status-warning",
  completed: "text-status-neutral",
  stopped: "text-status-danger",
  draft: "text-status-muted",
};
