/**
 * 알림 타입.
 *
 * - [간호사용 웹] AlertsTab (EMR "환자 호출" 탭) · PatientAlerts (RightPanel "알림" 탭)
 *
 * 백엔드 SourceType enum 5종 (자세한 운영 의미는 백엔드 스펙 확인). 현 시점 실제 발행은 self_report 만.
 */

export type SourceType =
  | "self_report"
  | "iv_alert"
  | "timer"
  | "order_change"
  | "vital_alert";

export interface NotificationListItem {
  notificationId: number;
  // backend 가 enum.name() 으로 직렬화 — string 으로 받되 SourceType 으로 좁혀 사용.
  sourceType: SourceType | string;
  title: string;
  body: string;
  patientId: number | null;
  patientName: string | null;
  sourceEntityId: number | null;
  // ISO datetime
  createdAt: string;
  recipientPractitionerId: number;
}

export interface NotificationListResponse {
  items: NotificationListItem[];
  // 더 받을 게 있으면 마지막 row 의 notificationId, 끝이면 null
  nextBefore: number | null;
}

// UI 라벨 매핑 — sourceType 별 한글 라벨/색상.
export const SOURCE_TYPE_LABEL: Record<SourceType, string> = {
  self_report: "환자 호출",
  iv_alert: "수액 경고",
  timer: "타이머",
  order_change: "오더 변경",
  vital_alert: "바이탈 경고",
};

export const SOURCE_TYPE_TONE: Record<SourceType, string> = {
  self_report: "text-status-active",
  iv_alert: "text-status-danger",
  timer: "text-status-warning",
  order_change: "text-status-success",
  vital_alert: "text-status-danger-strong",
};
