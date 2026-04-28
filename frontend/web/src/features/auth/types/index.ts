/**
 * 인증 관련 타입 정의.
 * UserRole · LoginRequest · AuthResponse
 */

export type UserRole = 'ADMIN' | 'HEAD_NURSE' | 'NURSE'

export interface LoginRequest {
  hospitalCode: string
  userId: string
  password: string
  role: UserRole
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: {
    id: string
    name: string
    role: UserRole
  }
}

// [환자용 웹앱] 본인 확인
export interface PatientVerifyRequest {
  patientId: number
  name: string
  birthDate: string
}

export interface PatientInfo {
  patientId: number
  patientName: string
  roomName: string
}
