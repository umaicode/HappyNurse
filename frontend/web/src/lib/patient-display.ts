/**
 * 환자 정보 표시 변환 헬퍼.
 *
 * 서버 응답 (gender enum, ISO date) → UI 표시용 문자열로 변환.
 * 사이드바, 환자 헤더, 담당 모달 등에서 재사용.
 */
import type { Gender } from "@/features/patient/types/ward-patient";

export function formatGenderShort(gender: Gender): string {
  switch (gender) {
    case "female":
      return "F";
    case "male":
      return "M";
    case "other":
      return "X";
    default:
      return "?";
  }
}

// "1999-05-20" → "99.05.20"
export function formatBirthShort(birthDate: string): string {
  if (!birthDate) return "";
  const [year, month, day] = birthDate.split("-");
  if (!year || !month || !day) return birthDate;
  return `${year.slice(2)}.${month}.${day}`;
}

// "1999-05-20" → "1999.05.20"
export function formatBirthFull(birthDate: string): string {
  if (!birthDate) return "";
  const [year, month, day] = birthDate.split("-");
  if (!year || !month || !day) return birthDate;
  return `${year}.${month}.${day}`;
}

export function calculateAge(birthDate: string, today: Date = new Date()): number {
  const [year, month, day] = birthDate.split("-").map(Number);
  if (!year || !month || !day) return 0;
  let age = today.getFullYear() - year;
  const todayMonth = today.getMonth() + 1;
  const todayDate = today.getDate();
  const beforeBirthday =
    todayMonth < month || (todayMonth === month && todayDate < day);
  if (beforeBirthday) age -= 1;
  return age;
}

export function groupByRoom<T extends { roomName: string }>(
  items: T[],
): Array<{ roomName: string; items: T[] }> {
  const order: string[] = [];
  const groups = new Map<string, T[]>();
  items.forEach((item) => {
    if (!groups.has(item.roomName)) {
      order.push(item.roomName);
      groups.set(item.roomName, []);
    }
    groups.get(item.roomName)!.push(item);
  });
  return order.map((roomName) => ({
    roomName,
    items: groups.get(roomName)!,
  }));
}
