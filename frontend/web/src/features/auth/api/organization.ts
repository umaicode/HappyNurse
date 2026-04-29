/**
 * 기관(병원)/병동 조회 API.
 *
 * - [간호사용 웹] getOrganizations() — GET /organizations
 * - [간호사용 웹] getWards(organizationId) — GET /organizations/{organizationId}/wards
 */
import { client } from "@/lib/client";
import type { Organization, WardSummary } from "../types/organization";

export const getOrganizations = (): Promise<Organization[]> =>
  client
    .get("/organizations")
    .then((response) => response.data?.data ?? response.data);

export const getWards = (organizationId: number): Promise<WardSummary[]> =>
  client
    .get(`/organizations/${organizationId}/wards`)
    .then((response) => response.data?.data ?? response.data);
