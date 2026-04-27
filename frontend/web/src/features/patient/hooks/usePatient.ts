/**
 * 환자 목록 · 병동 · 호실 필터 상태 관리.
 */
'use client'

import { useState, useEffect } from 'react'
import { getList } from '../api'
import type { Patient, PatientQuery } from '../types/patient'

export function usePatient(initialQuery: PatientQuery = {}) {
  const [patients, setPatients] = useState<Patient[]>([])
  const [query, setQuery] = useState<PatientQuery>(initialQuery)

  useEffect(() => {
    getList(query).then(setPatients)
  }, [query])

  return { patients, query, setQuery }
}
