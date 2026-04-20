/**
 * 환자 관련 타입 정의.
 * Patient · PatientDetail · PatientQuery · Ward · Room
 */

export interface Patient {
  id:              string
  name:            string
  age:             number
  bedNo:           string
  roomNo:          string
  diagnosis:       string
  pendingSTTCount: number
}

export interface PatientDetail extends Patient {
  admittedAt: string
  nurseId:    string
}

export interface PatientQuery {
  ward?: string
  room?: string
}

export type Ward = string
export type Room = string
