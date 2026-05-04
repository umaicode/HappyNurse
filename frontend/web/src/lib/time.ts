// HH:mm 포맷 (24시간). Date 또는 ISO 문자열을 받는다.
export const formatHHmm = (value: Date | string): string => {
  const date = typeof value === "string" ? new Date(value) : value;
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
};
