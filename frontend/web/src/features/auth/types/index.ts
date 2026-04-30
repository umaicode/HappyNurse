/**
 * 인증 관련 타입 정의.
 *
 * - [간호사용 웹] LoginRequest, AuthUser, DevLoginRequest, DevLoginResponse, SignupRequest, SignupResponse
 * - [환자용 웹앱] PatientVerifyRequest, PatientInfo
 *
 * 백엔드 응답 wrapper: { success, message, errorCode, data } — api 함수에서 data 만 추출하여 반환.
 */

// roleCode: 스웨거 enum — head_nurse | nurse | doctor | admin
export type RoleCode = "head_nurse" | "nurse" | "doctor" | "admin";

// [간호사용 웹] 로그인

export interface LoginRequest {
  organizationId: number;
  wardId: number;
  employeeNumber: string;
  password: string;
}

export interface AuthUser {
  practitionerId: number;
  name: string;
  employeeNumber: string;
  roleCode: RoleCode;
  wardId: number;
  // /auth/login · /auth/refresh 응답에만 포함, /practitioners/me 응답엔 없음
  organizationId?: number;
  // /practitioners/me 응답에만 포함
  wardName?: string;
}

// [간호사용 웹] DEV 로그인 (응답 body 로 토큰 반환)

export interface DevLoginRequest {
  employeeNumber: string;
}

export interface DevLoginResponse extends AuthUser {
  accessToken: string;
  refreshToken: string;
}

// [간호사용 웹] DEV 회원가입

export interface SignupRequest {
  employeeNumber: string;
  password: string;
  name: string;
  phone: string;
  organizationId: number;
  wardId: number;
  roleCode: RoleCode;
}

export interface SignupResponse {
  practitionerId: number;
  employeeNumber: string;
  name: string;
  roleCode: RoleCode;
  organizationId: number;
  wardId: number;
  periodStart: string;
}

// [환자용 웹앱] 본인 확인

export interface PatientVerifyRequest {
  patientId: number;
  name: string;
  birthDate: string;
}

export interface PatientInfo {
  patientId: number;
  patientName: string;
  roomName: string;
  gender: "male" | "female";
  departmentCode: string;
  diseaseName: string;
  chiefComplaint: string;
  surgeryName: string;
  assignedNurseName: string;
}
