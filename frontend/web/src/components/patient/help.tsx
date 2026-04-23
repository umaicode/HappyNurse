'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { nurseMock, patientMock, symptomsMock } from '@/mockup/patient'

export default function Help() {
  const router = useRouter()
  const [selectedSymptom, setSelectedSymptom] = useState<string | null>(null)
  const [directInput, setDirectInput] = useState('')

  return (
    <div className="flex flex-1 flex-col gap-5 px-5 pt-4 pb-6">
      <h1 className="text-3xl font-bold tracking-tight text-content-primary">증상 전송</h1>

      <div className="flex items-center rounded-xl bg-patient-accent-surface px-3.5 py-3">
        <div className="flex flex-1 flex-col gap-0.5">
          <p className="text-xl font-bold text-content-primary">
            {patientMock.name}  ·  {patientMock.room}
          </p>
          <p className="text-base font-bold text-content-primary">
            {nurseMock.role}  {nurseMock.name}
          </p>
        </div>
      </div>

      <h2 className="text-[22px] leading-[30px] font-bold tracking-tight text-content-primary">
        어떻게 도와드릴까요?
      </h2>

      <div className="grid grid-cols-2 gap-2.5">
        {symptomsMock.map((symptom) => {
          const isSelected = selectedSymptom === symptom.id
          return (
            <button
              key={symptom.id}
              type="button"
              onClick={() => setSelectedSymptom(symptom.id)}
              className={`flex h-[76px] items-center rounded-2xl border p-3 text-left text-xl font-bold tracking-tight transition-colors ${
                isSelected
                  ? 'border-brand-primary bg-brand-surface text-brand-primary'
                  : 'border-border-subtle bg-white text-content-primary'
              }`}
            >
              {symptom.label}
            </button>
          )
        })}
      </div>

      <div className="flex flex-col gap-3">
        <label className="text-base font-bold text-content-primary">직접 입력하기</label>
        <textarea
          value={directInput}
          onChange={(event) => setDirectInput(event.target.value)}
          placeholder="[증상을 입력해주세요]"
          className="h-28 w-full resize-none rounded-xl border border-border-subtle px-3.5 py-3 text-xl text-content-primary placeholder:text-content-muted focus:outline-none focus:ring-2 focus:ring-brand-primary/30"
        />
      </div>

      <button
        type="button"
        onClick={() => router.push('/patient/complete')}
        className="mt-auto w-full max-w-[300px] self-center rounded-2xl bg-[#1428a0] py-[18px] text-2xl font-bold tracking-tight text-white transition-colors hover:bg-[#101E7A]"
      >
        간호사에게 전송
      </button>
    </div>
  )
}
