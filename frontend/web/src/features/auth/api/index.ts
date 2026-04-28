/**
 * 인증 API 함수.
 *
 * - [간호사용 웹] login() · logout() · refreshToken()
 * - [환자용 웹앱] verifyPatient(request)  ─ 본인 확인 시 ACCESS_TOKEN 쿠키 발급
 */
import { client } from "@/lib/client";
import type { AuthResponse, LoginRequest, PatientInfo, PatientVerifyRequest } from "../types";

// [간호사용 웹] 로그인 / 로그아웃 / 토큰 갱신

export const login = (body: LoginRequest) =>
  client.post<AuthResponse>("/auth/login", body);

export const logout = () => client.post("/auth/logout");

export const refreshToken = () =>
  client.post<{ accessToken: string }>("/auth/refresh");

// [환자용 웹앱] 본인 확인

export const verifyPatient = (
  request: PatientVerifyRequest,
): Promise<PatientInfo> =>
  client
    .post("/patients/verify", request, { withCredentials: true })
    .then((response) => response.data?.data ?? response.data);
