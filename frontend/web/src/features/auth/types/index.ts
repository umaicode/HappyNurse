/**
 * 인증 관련 타입 정의.
 *
 * - [간호사용 웹] LoginRequest, AuthUser, DevLoginRequest, DevLoginResponse, SignupRequest, SignupResponse
 * - [환자용 웹앱] PatientNfc, PatientVerifyRequest, PatientInfo
 *
 */

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
  organizationId?: number;
  wardName?: string;
}

// [간호사용 웹] DEV 로그인

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

// [환자용 웹앱] NFC 진입 응답
export interface PatientNfc {
  patientId: number;
  patientName: string;
  roomName: string;
}

export interface PatientVerifyRequest {
  patientId: number;
  name: string;
  birthDate: string;
}

export interface PatientInfo {
  patientId: number;
  patientName: string;
  roomName: string;
  gender: string;
  departmentCode: string;
  diseaseName: string;
  chiefComplaint: string;
  // 수술 없는 환자는 null
  surgeryName: string | null;
  assignedNurseName: string;
}
