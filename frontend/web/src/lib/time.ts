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

// 두 줄 표시용 — { month: "MM", day: "DD", time: "HH:mm" }
export const splitDateLabel = (
  value: Date | string,
): { month: string; day: string; time: string } => {
  const date = typeof value === "string" ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) {
    return { month: "--", day: "--", time: "--:--" };
  }
  return {
    month: String(date.getMonth() + 1).padStart(2, "0"),
    day: String(date.getDate()).padStart(2, "0"),
    time: formatHHmm(date),
  };
};
