/**
 * 의사 오더 API.
 *
 * - [간호사용 웹] getOrders(encounterId) — GET /encounters/{encounterId}/orders
 *   응답은 dateWritten 내림차순 정렬되어 내려온다.
 */
import { client } from "@/lib/client";
import type { MedicationOrderListResponse } from "../types/order";

export const getOrders = (
  encounterId: number,
): Promise<MedicationOrderListResponse> =>
  client
    .get(`/encounters/${encounterId}/orders`)
    .then((response) => response.data);
