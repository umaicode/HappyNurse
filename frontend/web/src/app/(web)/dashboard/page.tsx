/**
 * 대시보드.
 * - 환자 미선택(id 없음): 안내 화면 표시
 * - 환자 선택(id 있음): 간호기록 뷰 표시
 * 병동·호실·환자 선택은 Sidebar(layout.tsx)에서 처리.
 */
import { Timeline } from '@/features/record/components/Timeline'
import { STTPanel } from '@/features/record/components/STTPanel'
import { getTimeline, getSTTList } from '@/features/record/api'

export default async function DashboardPage(props: {
  searchParams: Promise<{ id?: string }>
}) {
  const { id } = await props.searchParams

  if (!id) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-3 bg-[#F0F2F5]">
        <span className="text-5xl">📋</span>
        <p className="text-[16px] font-semibold text-gray-500">
          환자를 선택하면 간호기록이 표시됩니다
        </p>
        <p className="text-[13px] text-gray-400">
          왼쪽 사이드바에서 병동 → 호실 → 환자 순서로 선택
        </p>
      </div>
    )
  }

  const [records, stts] = await Promise.all([
    getTimeline(id),
    getSTTList(id),
  ])

  return (
    <div className="two-panel flex h-full">
      {/* 좌패널: 간호기록 통합본 타임라인 */}
      <Timeline data={records} />
      {/* 우패널: STT기록 + 포코기록 + 통인수 */}
      <STTPanel data={stts} />
    </div>
  )
}
