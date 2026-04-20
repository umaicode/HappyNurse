/**
 * 모달 래퍼. TransferModal 등에서 재사용.
 * backdrop 클릭 시 onClose 호출.
 */
'use client'

interface Props {
  isOpen:   boolean
  onClose:  () => void
  children: React.ReactNode
}

export function Modal({ isOpen, onClose, children }: Props) {
  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-lg shadow-lg p-6 min-w-80"
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  )
}
