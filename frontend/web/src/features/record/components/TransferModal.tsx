/**
 * 환자 이관 확인 모달.
 * 이관 전 작성자 · 확인 시각 입력 기록.
 * → components/common/Modal.tsx 재사용.
 */
'use client'

import { Modal } from '@/components/common/Modal'
import { transfer } from '../api'

interface Props {
  sttId:     string
  isOpen:    boolean
  onClose:   () => void
  onSuccess: () => void
}

export function TransferModal({ sttId, isOpen, onClose, onSuccess }: Props) {
  const handleConfirm = async () => {
    // 이관 전 작성자 · 확인 시각 기록
    await transfer(sttId)
    onSuccess()
    onClose()
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <h3 className="font-bold mb-3">이관 확인</h3>
      <p className="text-sm mb-4">해당 STT 기록을 간호기록으로 이관하시겠습니까?</p>
      <div className="flex gap-2 justify-end">
        <button
          onClick={handleConfirm}
          className="px-4 py-2 bg-blue-500 text-white rounded"
        >
          이관
        </button>
        <button onClick={onClose} className="px-4 py-2 border rounded">
          취소
        </button>
      </div>
    </Modal>
  )
}
