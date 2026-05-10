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
  self_report: "환자 요청",
  iv_alert: "수액 타이머",
  timer: "타이머",
  order_change: "오더 변경",
  vital_alert: "바이탈 경고",
};

export const SOURCE_TYPE_TONE: Record<SourceType, string> = {
  self_report: "text-status-danger",
  iv_alert: "text-status-active",
  timer: "text-status-warning",
  order_change: "text-status-success",
  vital_alert: "text-status-danger-strong",
};

// 카드 좌측 4px 강조 보더 + 카드 배경 surface 톤 — severity 표현.
// PanelCard 의 accentBorderClass 로 전달. STTPanel 의 변경 카드(`bg-brand-surface/20`) 와 통일된 패턴.
export const SOURCE_TYPE_BORDER: Record<SourceType, string> = {
  self_report:
    "border-l-4 border-l-status-warning border-status-warning/20 bg-status-warning-surface/30",
  iv_alert:
    "border-l-4 border-l-status-danger border-status-danger/20 bg-status-danger-surface/30",
  timer:
    "border-l-4 border-l-status-neutral border-status-neutral/20",
  order_change:
    "border-l-4 border-l-brand-primary border-brand-primary/20 bg-brand-surface/20",
  vital_alert:
    "border-l-4 border-l-status-danger-strong border-status-danger-strong/30 bg-status-danger-surface/30",
};

// 아이콘 박스 배경 — 카테고리 surface 톤.
export const SOURCE_TYPE_ICON_BG: Record<SourceType, string> = {
  self_report: "bg-status-warning-surface text-status-warning",
  iv_alert: "bg-status-danger-surface text-status-danger",
  timer: "bg-surface-hover text-status-neutral",
  order_change: "bg-brand-surface text-brand-primary",
  vital_alert: "bg-status-danger-surface text-status-danger-strong",
};
