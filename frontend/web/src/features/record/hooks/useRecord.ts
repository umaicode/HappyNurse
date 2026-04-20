/**
 * 간호기록 이관 처리 · 동시편집 잠금 상태.
 * WebSocket 실시간 잠금 동기화.
 */
'use client'

import { useState } from 'react'
import { transfer } from '../api'

export function useRecord(patientId: string) {
  const [locked, setLocked] = useState(false)
  const [lockedBy, setLockedBy] = useState<string | null>(null)

  const handleTransfer = async (sttId: string) => {
    await transfer(sttId)
  }

  // TODO: WebSocket 실시간 잠금 동기화
  // ws.on('lock',   ({ nurseName }) => { setLocked(true);  setLockedBy(nurseName) })
  // ws.on('unlock', ()              => { setLocked(false); setLockedBy(null)      })

  return { locked, lockedBy, handleTransfer }
}
