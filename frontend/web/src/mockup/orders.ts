/**
 * 의사 오더 mockup — 변경 뱃지/구분 필터 시각 검증용.
 *
 * 사용 위치: STTPanel (실 응답이 빈 배열일 때 fallback 으로 노출).
 * 검증 끝나면 import 제거 후 이 파일도 삭제 가능.
 */
import type {
  MedicationOrderItem,
  MedicationOrderListResponse,
} from "@/features/dashboard/types/order";

const ORDERS: MedicationOrderItem[] = [
  {
    medicationOrderId: 9001,
    orderType: "MEDICATION",
    orderCode: "🛠️ MED-001",
    orderName: "🛠️ Acetaminophen 500mg",
    dose: 1,
    frequency: 3,
    doseUnit: "T",
    route: "PO",
    remarks: "🛠️ 발열 시 복용",
    status: "active",
    dateWritten: "2026-05-06T08:00:00",
    prescriberId: 1,
    prescriberName: "🛠️ 김의사",
    createdAt: "2026-05-06T08:00:00",
    updatedAt: "2026-05-06T08:00:00",
  },
  {
    // 변경 뱃지 검증 — updatedAt 이 createdAt 보다 늦음.
    medicationOrderId: 9002,
    orderType: "FLUID",
    orderCode: "🛠️ FLU-002",
    orderName: "🛠️ N/S 1L",
    dose: 1000,
    frequency: 1,
    doseUnit: "mL",
    route: "IV",
    remarks: "🛠️ 용량 변경됨 (500 → 1000)",
    status: "active",
    dateWritten: "2026-05-06T07:30:00",
    prescriberId: 2,
    prescriberName: "🛠️ 이의사",
    createdAt: "2026-05-06T07:30:00",
    updatedAt: "2026-05-06T09:15:00",
  },
  {
    medicationOrderId: 9003,
    orderType: "INSTRUCTION",
    orderCode: "🛠️ INS-003",
    orderName: "🛠️ 절대 안정",
    dose: 0,
    frequency: 1,
    doseUnit: "-",
    route: "-",
    remarks: null,
    status: "active",
    dateWritten: "2026-05-06T06:00:00",
    prescriberId: 1,
    prescriberName: "🛠️ 김의사",
    createdAt: "2026-05-06T06:00:00",
    updatedAt: "2026-05-06T06:00:00",
  },
  {
    medicationOrderId: 9004,
    orderType: "LIS",
    orderCode: "🛠️ LIS-004",
    orderName: "🛠️ CBC + CRP",
    dose: 1,
    frequency: 1,
    doseUnit: "회",
    route: "Lab",
    remarks: null,
    status: "completed",
    dateWritten: "2026-05-05T22:00:00",
    prescriberId: 1,
    prescriberName: "🛠️ 김의사",
    createdAt: "2026-05-05T22:00:00",
    updatedAt: "2026-05-05T22:00:00",
  },
  {
    // 변경 뱃지 검증 #2.
    medicationOrderId: 9005,
    orderType: "MEDICATION",
    orderCode: "🛠️ MED-005",
    orderName: "🛠️ Tramadol 50mg",
    dose: 1,
    frequency: 4,
    doseUnit: "T",
    route: "PO",
    remarks: "🛠️ 빈도 변경됨 (3 → 4)",
    status: "active",
    dateWritten: "2026-05-05T20:00:00",
    prescriberId: 2,
    prescriberName: "🛠️ 이의사",
    createdAt: "2026-05-05T20:00:00",
    updatedAt: "2026-05-06T10:00:00",
  },
];

export const MOCK_MEDICATION_ORDER_LIST: MedicationOrderListResponse = {
  encounterId: -1,
  patientId: -1,
  patientName: "🛠️ 문현지",
  totalCount: ORDERS.length,
  orders: ORDERS,
};
