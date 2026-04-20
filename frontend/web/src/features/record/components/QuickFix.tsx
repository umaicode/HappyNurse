/**
 * RAG 대안 후보 표시.
 * STT 단어 클릭 시 표시 · 추천후보 + 직접 입력.
 * → useSTT.ts의 openRAG() 로 열림.
 */
'use client'

import { useState } from 'react'

interface Props {
  word:       string
  candidates: string[]
  onApply:    (word: string) => void
  onClose:    () => void
}

export function QuickFix({ word, candidates, onApply, onClose }: Props) {
  const [custom, setCustom] = useState('')

  return (
    <div className="quickfix border rounded p-3 bg-white shadow">
      <h4 className="font-medium mb-2">`{word}` 대체 후보</h4>
      {/* RAG 추천 후보 목록 */}
      <div className="flex flex-wrap gap-2 mb-3">
        {candidates.map((c) => (
          <button
            key={c}
            onClick={() => onApply(c)}
            className="px-2 py-1 border rounded text-sm hover:bg-gray-100"
          >
            {c}
          </button>
        ))}
      </div>
      {/* 직접 입력 */}
      <div className="flex gap-2">
        <input
          value={custom}
          onChange={(e) => setCustom(e.target.value)}
          placeholder="직접 입력"
          className="border rounded px-2 py-1 text-sm flex-1"
        />
        <button
          onClick={() => onApply(custom)}
          className="px-3 py-1 bg-blue-500 text-white rounded text-sm"
        >
          적용
        </button>
      </div>
      <button onClick={onClose} className="mt-2 text-xs text-gray-400">
        닫기
      </button>
    </div>
  )
}
