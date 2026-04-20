/**
 * [좌패널] 간호기록 통합본 타임라인.
 * 이전 기록 시간순 · 기록자 및 태그 · 작성자 · 확인 시각.
 * → app/(web)/patients/[id]/page.tsx 좌측 영역.
 */
import type { NursingRecord } from '../types'

const typeLabel: Record<NursingRecord['type'], string> = {
  VS_CHECK:   '활력징후',
  MEDICATION: '투약',
  DRESSING:   '드레싱',
  NOTE:       '노트',
}

export function Timeline({ data }: { data: NursingRecord[] }) {
  return (
    <aside className="timeline w-1/2 overflow-y-auto border-r p-4">
      <h2 className="font-bold mb-4">간호기록 통합본</h2>
      <ul className="space-y-3">
        {data.map((record) => (
          <li key={record.id} className="border-b pb-3">
            {/* 기록 유형 태그 */}
            <span className="tag text-xs">{typeLabel[record.type]}</span>
            {/* 기록자 · 확인 시각 */}
            <span className="text-sm text-gray-500 ml-2">{record.nurseName}</span>
            <span className="text-xs text-gray-400 ml-2">{record.createdAt}</span>
            <p className="mt-1">{record.content}</p>
          </li>
        ))}
      </ul>
    </aside>
  )
}
