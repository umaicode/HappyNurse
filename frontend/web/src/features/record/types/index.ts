/**
 * 간호기록 관련 타입 정의.
 * NursingRecord · STTRecord · RecordType · TransferStatus
 */

export type RecordType     = 'VS_CHECK' | 'MEDICATION' | 'DRESSING' | 'NOTE'
export type TransferStatus = 'PENDING'  | 'TRANSFERRED'

export interface NursingRecord {
  id:        string
  type:      RecordType
  content:   string
  nurseName: string
  createdAt: string
}

export interface STTRecord {
  id:           string
  originalText: string
  ragText:      string
  status:       TransferStatus
  createdAt:    string
  nurseName:    string
}
