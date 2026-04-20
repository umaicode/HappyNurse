/**
 * 동시편집 잠금 상태 표시.
 * 다른 간호사 편집 중일 때 잠금 안내.
 * → useRecord.ts의 locked 상태와 연동.
 */
interface Props {
  locked:    boolean
  lockedBy?: string
}

export function LockBadge({ locked, lockedBy }: Props) {
  if (!locked) return null

  return (
    <div className="lock-badge flex items-center gap-1 text-sm text-orange-600 bg-orange-50 border border-orange-200 rounded px-3 py-1">
      <span>잠금</span>
      <span>{lockedBy}님이 편집 중입니다</span>
    </div>
  )
}
