// HH:mm 포맷 (24시간). Date 또는 ISO 문자열을 받는다.
export const formatHHmm = (value: Date | string): string => {
  const date = typeof value === "string" ? new Date(value) : value;
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
};

// "MM.dd HH:mm" 포맷 (24시간).
export const formatMonthDayHHmm = (value: Date | string): string => {
  const date = typeof value === "string" ? new Date(value) : value;
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${month}.${day} ${formatHHmm(date)}`;
};

// "yyyy-MM-dd" 부분만 추출. ISO datetime 의 시간 부분을 잘라낸다.
export const toIsoDate = (value: Date | string): string => {
  const date = typeof value === "string" ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return "";
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

// 알림용 한국어 상대 시간 — "방금" / "N분 전" / "MM.dd HH:mm".
// 1시간 이후로는 절대 시간(MM.dd HH:mm) 으로 전환한다.
export const formatRelativeTime = (value: Date | string): string => {
  const date = typeof value === "string" ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return "";
  const diffMs = Date.now() - date.getTime();
  const diffMin = Math.floor(diffMs / 60_000);
  if (diffMin < 1) return "방금";
  if (diffMin < 60) return `${diffMin}분 전`;
  return formatMonthDayHHmm(date);
};
