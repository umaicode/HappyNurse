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

// BE 가 dispatchSessionEvent 로 발행하는 로그인/로그아웃 알림 등 — 사용자 알림 피드에 노출 안 함.
// BE SourceType enum 이 7종(self_report·iv_alert·timer·order_change·vital_alert·web_login·web_logout)이지만
// FE 는 위 5종만 의미 있는 알림으로 취급. 향후 시스템 이벤트가 더 추가되면 여기에 한 줄.
export const HIDDEN_SOURCE_TYPES = new Set<string>(["web_login", "web_logout"]);

// BE SymptomPriority (자가보고 분류 LLM 결과). self_report 알림에만 채워지고
// iv_alert/order_change 등은 항상 null. 5월 초 BE 자가보고 중요도 분류 통합 시 추가.
export type SymptomPriority = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";

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
  // SymptomPriority — self_report 만 채워짐. 다른 sourceType 은 null.
  priority: SymptomPriority | null;
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

// self_report 알림 priority 한글 라벨.
export const PRIORITY_LABEL: Record<SymptomPriority, string> = {
  CRITICAL: "긴급",
  HIGH: "높음",
  MEDIUM: "보통",
  LOW: "낮음",
};

// priority 별 칩 톤 (라벨용 텍스트 + 배경). self_report 카드에 보조 칩으로 노출.
export const PRIORITY_CHIP: Record<SymptomPriority, string> = {
  CRITICAL: "bg-status-danger-surface text-status-danger-strong",
  HIGH: "bg-status-danger-surface text-status-danger",
  MEDIUM: "bg-status-warning-surface text-status-warning",
  LOW: "bg-surface-hover text-status-neutral",
};

// priority 가 채워진 self_report 카드는 SOURCE_TYPE_BORDER 대신 이걸로 override.
// CRITICAL/HIGH 는 danger 강조, MEDIUM 은 warning, LOW 는 neutral.
export const PRIORITY_BORDER: Record<SymptomPriority, string> = {
  CRITICAL:
    "border-l-4 border-l-status-danger-strong border-status-danger-strong/30 bg-status-danger-surface/40",
  HIGH:
    "border-l-4 border-l-status-danger border-status-danger/20 bg-status-danger-surface/30",
  MEDIUM:
    "border-l-4 border-l-status-warning border-status-warning/20 bg-status-warning-surface/30",
  LOW:
    "border-l-4 border-l-status-neutral border-status-neutral/20",
};
