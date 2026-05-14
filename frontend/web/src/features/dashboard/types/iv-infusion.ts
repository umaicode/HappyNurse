/**
 * 수액 (IV Infusion) 타입.
 *
 * - [간호사용 웹] IVTimerPanel (RightPanel "수액 타이머" 탭) 데이터 소스
 *
 * 백엔드 응답 wrapper { success, message, errorCode, data } 는 axios 인터셉터에서 평탄화됨.
 */

export type IvStatus =
  | "IN_PROGRESS"
  | "PAUSED"
  | "COMPLETED"
  | "CANCELLED"
  | "EXPIRED";

// 수액 세트 (gtt/mL). 5/11 BE 가 기존 PatientType(ADULT/PEDIATRIC) 를 이 enum 으로 교체.
export type DropSet = "SET_10" | "SET_15" | "SET_20" | "SET_60";

// 카드 보조 표기용 라벨 (예: "20 set"). globals.css 토큰과 무관한 도메인 라벨.
export const DROP_SET_LABEL: Record<DropSet, string> = {
  SET_10: "10 set",
  SET_15: "15 set",
  SET_20: "20 set",
  SET_60: "60 set",
};

export interface IvInfusionListItem {
  ivInfusionId: number;
  patientId: number;
  patientName: string;
  // 혼합 약물명 목록 (1개 이상). UI 에선 join 표시.
  medicationNames: string[];
  currentRateMlPerHr: number;
  // gtt/min — BE 가 dropSet 기반으로 역환산해 내려줌. 마이그레이션 누락 row 는 null 가능.
  rateGttPerMin: number | null;
  // 수액 세트. 마이그레이션 누락 row 는 null 가능.
  dropSet: DropSet | null;
  status: IvStatus;
  // ISO datetime — 투여 시작 시각 (진행 막대바 기준)
  startedAt: string;
  // ISO datetime — 종료 예정 시각
  expectedEndAt: string;
  // 잔여 시간 (초) — 응답 시점 기준. 클라에선 expectedEndAt - now 로 매분 다시 계산하므로 초기 sanity 용도.
  remainingSeconds: number;
}
