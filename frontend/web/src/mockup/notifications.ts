/**
 * 알림 mockup — PatientAlerts 시연용.
 *
 * 사용 위치: PatientAlerts (실 응답이 빈 배열일 때 fallback).
 * createdAt 은 모듈 로드 시점 기준 상대 시각으로 동적 생성 — 검증 시 항상 "방금/N분 전" 노출.
 * 검증 끝나면 import 와 fallback 제거.
 */
import type { NotificationListItem } from "@/features/dashboard/types/notification";

const now = Date.now();
const minutesAgo = (n: number) => new Date(now - n * 60_000).toISOString();
const hoursAgo = (n: number) => new Date(now - n * 3_600_000).toISOString();

export const MOCK_NOTIFICATIONS: NotificationListItem[] = [
  {
    notificationId: 6001,
    sourceType: "iv_alert",
    title: "🛠️ 수액 종료",
    body: "🛠️ N/S 1L 종료 — 즉시 교체 필요",
    patientId: 1001,
    patientName: "🛠️ 김가민",
    sourceEntityId: null,
    createdAt: minutesAgo(0),
    recipientPractitionerId: -1,
  },
  {
    notificationId: 6002,
    sourceType: "self_report",
    title: "🛠️ 환자 호출",
    body: "🛠️ 속이 메스껍습니다",
    patientId: 1002,
    patientName: "🛠️ 박영희",
    sourceEntityId: null,
    createdAt: minutesAgo(3),
    recipientPractitionerId: -1,
  },
  {
    notificationId: 6003,
    sourceType: "iv_alert",
    title: "🛠️ 수액 5분 임박",
    body: "🛠️ Hartmann 1L 잔여 5분",
    patientId: 1003,
    patientName: "🛠️ 최민호",
    sourceEntityId: null,
    createdAt: minutesAgo(8),
    recipientPractitionerId: -1,
  },
  {
    notificationId: 6004,
    sourceType: "order_change",
    title: "🛠️ 의사오더 변경",
    body: "🛠️ Tramadol 50mg — 빈도 3 → 4회 변경",
    patientId: 1001,
    patientName: "🛠️ 김가민",
    sourceEntityId: 9005,
    createdAt: minutesAgo(15),
    recipientPractitionerId: -1,
  },
  {
    notificationId: 6006,
    sourceType: "self_report",
    title: "🛠️ 환자 호출",
    body: "🛠️ 통증이 심해졌습니다",
    patientId: 1005,
    patientName: "🛠️ 이도현",
    sourceEntityId: null,
    createdAt: hoursAgo(2),
    recipientPractitionerId: -1,
  },
];
