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

