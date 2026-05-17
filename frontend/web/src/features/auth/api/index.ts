/**
 * 인증 API 함수.
 *
 * client 응답 인터셉터가 { success, data, ... } wrapper 의 data 만 평탄화하여 내려주므로
 * 각 함수는 response.data 를 그대로 반환한다.
 *
 * - [간호사용 웹] login() · logout() · getMe() · devLogin() · devSignup() · extendSession()
 * - [환자용 웹앱] verifyPatient(request) — ACCESS_TOKEN 쿠키 발급
 *
 * /auth/refresh — 401 인터셉터가 자동 호출 (REFRESH_TOKEN 으로 ACCESS+REFRESH 둘 다 재발급)
 * /auth/extend  — 사용자 명시 연장 (ACCESS_TOKEN 만 재발급, REFRESH_TOKEN 그대로 유지)
 */
import { client } from "@/lib/client";
import type {
  AuthUser,
  DevLoginRequest,
  DevLoginResponse,
  LoginRequest,
  PatientInfo,
  PatientVerifyRequest,
  SignupRequest,
  SignupResponse,
} from "../types";

// [간호사용 웹] 로그인 / 로그아웃 / 내 정보

export const login = (request: LoginRequest): Promise<AuthUser> =>
  client.post('/auth/login', request).then((response) => response.data)

export const logout = (): Promise<void> =>
  client.post("/auth/logout").then(() => undefined);

export const getMe = (): Promise<AuthUser> =>
  client.get('/practitioners/me').then((response) => response.data)

// 사용자 명시 세션 연장 — 사이드바의 "연장" 버튼이 호출. BE 가 ACCESS_TOKEN 쿠키만 재발급.
// REFRESH_TOKEN 은 그대로 유지되므로 보안 영향 적음. 응답 body 는 ApiResponseVoid.
export const extendSession = (): Promise<void> =>
  client.post("/auth/extend").then(() => undefined);

// [간호사용 웹] DEV 로그인 / DEV 회원가입 — NEXT_PUBLIC_APP_ENV === 'dev' 환경에서만 호출

export const devLogin = (request: DevLoginRequest): Promise<DevLoginResponse> =>
  client.post('/auth/dev-login', request).then((response) => response.data)

export const devSignup = (request: SignupRequest): Promise<SignupResponse> =>
  client.post('/auth/signup', request).then((response) => response.data)

// [환자용 웹앱] 본인 확인 — ACCESS_TOKEN 쿠키가 응답에 Set-Cookie 로 발급됨.

export const verifyPatient = (
  request: PatientVerifyRequest,
): Promise<PatientInfo> =>
  client.post('/patients/verify', request).then((response) => response.data)
