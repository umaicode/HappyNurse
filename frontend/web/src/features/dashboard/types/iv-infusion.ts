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

export interface IvInfusionListItem {
  ivInfusionId: number;
  patientId: number;
  patientName: string;
  // 혼합 약물명 목록 (1개 이상). UI 에선 join 표시.
  medicationNames: string[];
  currentRateMlPerHr: number;
  status: IvStatus;
  // ISO datetime — 투여 시작 시각 (진행 막대바 기준)
  startedAt: string;
  // ISO datetime — 종료 예정 시각
  expectedEndAt: string;
  // 잔여 시간 (초) — 응답 시점 기준. 클라에선 expectedEndAt - now 로 매분 다시 계산하므로 초기 sanity 용도.
  remainingSeconds: number;
}
