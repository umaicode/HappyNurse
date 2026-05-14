/**
 * 수액 조회 API.
 *
 * - GET /iv?wardId=&status= — 병동 IV 목록 (slim).
 *   NFC 의존 endpoint (start/by-tag/complete/rate) 는 데스크톱 웹 미지원.
 */
import { client } from "@/lib/client";
import type { IvInfusionListItem, IvStatus } from "../types/iv-infusion";

export const getWardIvInfusions = (
  wardId: number,
  status?: IvStatus,
): Promise<IvInfusionListItem[]> =>
  client
    .get(`/iv`, {
      params: status ? { wardId, status } : { wardId },
    })
    .then((response) => response.data);
