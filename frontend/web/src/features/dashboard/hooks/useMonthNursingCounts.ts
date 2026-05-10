"use client";

import { useMemo } from "react";
import { useQueries } from "@tanstack/react-query";
import { getNursingNotes } from "../api/nursing-note";
import { toIsoDate } from "@/lib/time";

/**
 * 한 달치 일자별 간호기록 개수 조회.
 *
 * 백엔드에 월 단위 카운트 endpoint 가 없어서 (BE TODO ③) 모바일과 동일하게 일자별
 * `GET /encounters/{id}/nursing-notes?date=` 를 28~31번 병렬 호출 후 size 로 카운트를 만든다.
 * queryKey 가 NursingTab 의 useNursingNotes 와 동일해 같은 일자 캐시를 공유한다 — 즉
 * EMR 진입 시 미리 캐시된 일자는 추가 fetch 가 일어나지 않고, 새로 fetch 한 일자도 NursingTab
 * 진입 시 즉시 사용 가능.
 *
 * 반환은 Map<"yyyy-MM-dd", number> — count > 0 인 날짜만 포함.
 */
export const useMonthNursingCounts = (
  encounterId: number | null,
  visibleMonth: Date,
) => {
  const dayIsoList = useMemo(() => {
    const year = visibleMonth.getFullYear();
    const month = visibleMonth.getMonth();
    const lastDay = new Date(year, month + 1, 0).getDate();
    return Array.from({ length: lastDay }, (_, index) =>
      toIsoDate(new Date(year, month, index + 1)),
    );
  }, [visibleMonth]);

  const queries = useQueries({
    queries: dayIsoList.map((date) => ({
      queryKey: ["encounter", encounterId, "nursing-notes", date] as const,
      queryFn: () => getNursingNotes(encounterId as number, date),
      enabled: encounterId !== null,
      staleTime: 60_000,
    })),
  });

  return useMemo(() => {
    const map = new Map<string, number>();
    queries.forEach((query, index) => {
      const data = query.data;
      if (data && data.length > 0) {
        map.set(dayIsoList[index], data.length);
      }
    });
    return map;
  }, [queries, dayIsoList]);
};
