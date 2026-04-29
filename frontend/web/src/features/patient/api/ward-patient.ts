/**
 * 병동 입원 환자 / 담당 환자 관리 API.
 *
 * - [간호사용 웹] getWardPatients() — GET /wards/me/patients (토큰 wardId 기준)
 * - [간호사용 웹] assignMyPatients(encounterIds) — PUT /practitioners/me/patients
 */
import { client } from "@/lib/client";
import type {
  AssignMyPatientsResponse,
  WardPatient,
} from "../types/ward-patient";

export const getWardPatients = (): Promise<WardPatient[]> =>
  client
    .get("/wards/me/patients")
    .then((response) => response.data?.data ?? response.data);

export const assignMyPatients = (
  encounterIds: number[],
): Promise<AssignMyPatientsResponse> =>
  client
    .put("/practitioners/me/patients", { encounterIds })
    .then((response) => response.data?.data ?? response.data);
