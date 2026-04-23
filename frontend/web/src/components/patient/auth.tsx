'use client'

import { useRouter } from 'next/navigation'
import { CircleCheck } from 'lucide-react'
import { patientMock } from '@/mockup/patient'

export default function Auth() {
  const router = useRouter()

  return (
    <div className="flex flex-1 flex-col px-5 pt-10 pb-6">
      <div className="self-start rounded-full bg-[#eff6ff] px-4 py-1.5">
        <span className="text-xl font-bold text-[#1428a0]">환자용</span>
      </div>

      <div className="mt-4 flex items-center gap-3 rounded-xl bg-patient-accent-surface px-4 py-3.5">
        <CircleCheck className="size-7 text-patient-success" strokeWidth={2.5} />
        <div className="flex flex-col gap-0.5">
          <span className="text-sm font-bold text-patient-success">태깅 완료</span>
          <span className="text-base font-medium text-content-primary">
            {patientMock.name}님의 팔찌가 인식되었습니다
          </span>
        </div>
      </div>

      <h1 className="mt-6 text-[22px] leading-8 font-bold tracking-tight text-content-primary">
        이름과 생년월일을 입력해주세요
      </h1>

      <div className="mt-8 flex flex-col gap-2">
        <label className="text-base font-bold text-content-primary">이름</label>
        <div className="rounded-xl border-[1.5px] border-border-subtle px-4 py-3.5">
          <span className="text-2xl text-content-primary">{patientMock.name}</span>
        </div>
      </div>

      <div className="mt-7 flex flex-col gap-3">
        <label className="text-base font-bold text-content-primary">생년월일 6자리</label>
        <div className="flex w-full gap-2">
          {Array.from({ length: 6 }).map((_, index) => (
            <div
              key={index}
              className={`flex h-14 flex-1 items-center justify-center rounded-xl bg-white ${
                index === 0
                  ? 'border-2 border-[#2563eb]'
                  : 'border-[1.5px] border-border-subtle'
              }`}
            >
              <span className="size-1.5 rounded-full bg-content-primary/60" />
            </div>
          ))}
        </div>
        <p className="text-base text-content-tertiary">
          예: 010429 (2001년 4월 29일생)
        </p>
      </div>

      <button
        type="button"
        onClick={() => router.push('/patient/help')}
        className="mt-auto w-full max-w-[300px] self-center rounded-2xl bg-[#1428a0] py-[18px] text-2xl font-bold tracking-tight text-white transition-colors hover:bg-[#101E7A]"
      >
        완료
      </button>
    </div>
  )
}
