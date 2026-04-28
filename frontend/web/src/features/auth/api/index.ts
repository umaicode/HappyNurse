/**
 * 인증 API 함수.
 *
 * 모든 함수는 백엔드 응답 wrapper { success, data, ... } 에서 data 만 unwrap 하여 반환한다.
 *
 * - [간호사용 웹] login() · logout() · getMe() · devLogin() · devSignup()
 * - [환자용 웹앱] verifyPatient(request) — ACCESS_TOKEN 쿠키 발급
 *
 * 토큰 갱신(/auth/refresh) 은 client.ts 의 401 인터셉터가 직접 호출하므로 별도 export 하지 않는다.
 */
import { client } from '@/lib/client'
import type {
  AuthUser,
  DevLoginRequest,
  DevLoginResponse,
  LoginRequest,
  PatientInfo,
  PatientVerifyRequest,
  SignupRequest,
  SignupResponse,
} from '../types'

// [간호사용 웹] 로그인 / 로그아웃 / 내 정보

export const login = (request: LoginRequest): Promise<AuthUser> =>
  client
    .post('/auth/login', request)
    .then((response) => response.data?.data ?? response.data)

export const logout = (): Promise<void> =>
  client.post('/auth/logout').then(() => undefined)

export const getMe = (): Promise<AuthUser> =>
  client
    .get('/practitioners/me')
    .then((response) => response.data?.data ?? response.data)

// [간호사용 웹] DEV 로그인 / DEV 회원가입 — NEXT_PUBLIC_APP_ENV === 'dev' 환경에서만 호출

export const devLogin = (request: DevLoginRequest): Promise<DevLoginResponse> =>
  client
    .post('/auth/dev-login', request)
    .then((response) => response.data?.data ?? response.data)

export const devSignup = (request: SignupRequest): Promise<SignupResponse> =>
  client
    .post('/auth/signup', request)
    .then((response) => response.data?.data ?? response.data)

// [환자용 웹앱] 본인 확인 — 환자 웹앱 코드가 import 중. 시그니처 변경 금지.

export const verifyPatient = (
  request: PatientVerifyRequest,
): Promise<PatientInfo> =>
  client
    .post('/patients/verify', request, { withCredentials: true })
    .then((response) => response.data?.data ?? response.data)
