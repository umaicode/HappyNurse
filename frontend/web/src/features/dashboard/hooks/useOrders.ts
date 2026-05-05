"use client";

import { useQuery } from "@tanstack/react-query";
import { getOrders } from "../api/order";

export const useOrders = (encounterId: number | null) =>
  useQuery({
    queryKey: ["encounter", encounterId, "orders"] as const,
    queryFn: () => getOrders(encounterId as number),
    enabled: encounterId !== null,
  });
