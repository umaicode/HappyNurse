/**
 * 간호기록 API 함수.
 * getTimeline(patientId) · getSTTList(patientId)
 * transfer(sttId) · applyRAG(recordId, word)
 */
import { client } from '@/lib/client'
import type { NursingRecord, STTRecord } from '../types'

export const getTimeline = (patientId: string) =>
  client.get<NursingRecord[]>(`/patients/${patientId}/records`).then((r) => r.data)

export const getSTTList = (patientId: string) =>
  client.get<STTRecord[]>(`/patients/${patientId}/stt-records`).then((r) => r.data)

export const transfer = (sttId: string) =>
  client.post(`/stt-records/${sttId}/transfer`)

export const applyRAG = (recordId: string, word: string) =>
  client.post(`/stt-records/${recordId}/rag`, { word })
