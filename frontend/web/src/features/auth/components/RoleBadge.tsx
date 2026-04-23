/**
 * 역할 배지. 프로젝트 전체에서 반복 사용.
 * ADMIN · HEAD_NURSE · NURSE 색상 분기.
 * → components/layout/Header.tsx 등에서 사용.
 */
type Props = { role: 'ADMIN' | 'HEAD_NURSE' | 'NURSE' }

const map = {
  ADMIN:      { label: '관리자',    color: 'red'   },
  HEAD_NURSE: { label: '수간호사',  color: 'blue'  },
  NURSE:      { label: '일반간호사', color: 'green' },
}

export function RoleBadge({ role }: Props) {
  const { label, color } = map[role]
  return (
    <span className={`badge badge-${color}`}>
      {label}
    </span>
  )
}
