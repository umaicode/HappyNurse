/**
 * STT 기록 목록 · RAG 패널 열기/닫기.
 * 선택 단어 · 대안 후보 상태 관리.
 */
'use client'

import { useState } from 'react'
import { getSTTList, applyRAG } from '../api'
import type { STTRecord } from '../types'

export function useSTT(patientId: string) {
  const [stts, setStts] = useState<STTRecord[]>([])
  const [selectedWord, setSelectedWord] = useState<string | null>(null)
  const [ragOpen, setRagOpen] = useState(false)

  const loadSTTs = async () => {
    const data = await getSTTList(patientId)
    setStts(data)
  }

  // STT 단어 클릭 시 RAG 패널 열기
  const openRAG = (word: string) => {
    setSelectedWord(word)
    setRagOpen(true)
  }

  const closeRAG = () => {
    setSelectedWord(null)
    setRagOpen(false)
  }

  const handleApplyRAG = async (recordId: string, word: string) => {
    await applyRAG(recordId, word)
    await loadSTTs()
    closeRAG()
  }

  return { stts, selectedWord, ragOpen, openRAG, closeRAG, handleApplyRAG, loadSTTs }
}
