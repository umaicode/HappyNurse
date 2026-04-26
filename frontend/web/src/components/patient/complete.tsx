"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Check, Clock } from "lucide-react";
import { nurseMock, patientMock, symptomsMock } from "@/mockup/patient";

export default function Complete() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const patientName = searchParams.get("name") ?? patientMock.name;
  const sentAt = searchParams.get("sentAt") ?? "";
  const symptomsParam = searchParams.get("symptoms") ?? "";
  const directInput = searchParams.get("direct") ?? "";

  const selectedIds = symptomsParam
    ? symptomsParam.split(",").filter(Boolean)
    : [];
  const requestLabels = symptomsMock
    .filter((symptom) => selectedIds.includes(symptom.id))
    .map((symptom) => symptom.label);
  const requestChips = directInput
    ? [...requestLabels, directInput]
    : requestLabels;

  return (
    <div className="flex flex-1 flex-col gap-5 px-[22px] pt-5 pb-[50px]">
      <div className="flex flex-col items-center pt-15 pb-10">
        <div className="flex size-[60px] items-center justify-center rounded-full bg-patient-success text-white shadow-[0_0_0_6px_var(--color-patient-success-surface)]">
          <Check className="size-8" strokeWidth={3} />
        </div>
        <h1 className="mt-5 text-center text-[21px] leading-[1.35] font-extrabold tracking-tight text-patient-ink">
          담당 간호사에게
          <br />
          요청이 전달되었어요
        </h1>
      </div>

      <div className="flex flex-1 flex-col gap-3">
        <div className="rounded-2xl border border-patient-hairline bg-white px-4 py-1">
          <Row label="환자" value={`${patientName} · ${patientMock.room}`} />
          <Row label="담당 간호사" value={nurseMock.name} />
          <Row label="전송 시각" value={sentAt} />
          <Row
            label="요청 내용"
            last
            value={
              requestChips.length > 0 ? (
                <div className="flex flex-col items-end gap-1.5">
                  {requestChips.map((chip, index) => (
                    <span
                      key={`${chip}-${index}`}
                      className="rounded-full bg-patient-slate-surface px-3 py-1 text-lg font-bold text-patient-slate"
                    >
                      {chip}
                    </span>
                  ))}
                </div>
              ) : (
                <span className="text-sm font-bold tracking-tight text-patient-ink">
                  -
                </span>
              )
            }
          />
        </div>

        <div className="rounded-2xl border border-patient-hairline bg-white px-4 pt-3.5 pb-4">
          <div className="mb-5 text-base font-extrabold tracking-wide text-[#383838]">
            진행 상태
          </div>
          <div className="flex items-start justify-center gap-0">
            {/* 확인중 */}
            <div className="flex flex-col items-center gap-1.5 w-[64px]">
              <div
                className="flex size-[28px] items-center justify-center rounded-full bg-patient-slate text-white"
                style={{ animation: "pt-glow 1.8s ease-in-out infinite" }}
              >
                <Clock className="size-3.5" strokeWidth={2.5} />
              </div>
              <span className="text-lg font-bold whitespace-nowrap text-patient-slate">
                확인중
              </span>
            </div>

            {/* 커넥터 */}
            <div className="mt-3.5 h-0.5 w-16 rounded-full bg-patient-hairline" />

            {/* 확인완료 */}
            <div className="flex flex-col items-center gap-1.5">
              <div className="flex size-[28px] items-center justify-center rounded-full border-[1.5px] border-patient-line bg-white text-patient-fade">
                <Check className="size-3.5" strokeWidth={2.5} />
              </div>
              <span className="text-lg font-bold whitespace-nowrap text-patient-muted">
                확인완료
              </span>
            </div>
          </div>
        </div>

        <div className="flex-1" />

        <button
          type="button"
          onClick={() => router.push("/patient/help")}
          className="h-14 w-full rounded-[14px] bg-patient-primary text-[20px] font-bold tracking-tight text-white transition-colors hover:bg-[#0F1F7A]"
        >
          확인
        </button>
      </div>
    </div>
  );
}

function Row({
  label,
  value,
  last,
}: {
  label: string;
  value: React.ReactNode;
  last?: boolean;
}) {
  return (
    <div
      className={`flex items-center justify-between gap-4 py-3 ${
        last ? "" : "border-b border-patient-hairline"
      }`}
    >
      <span className="text-[18px] font-semibold text-patient-muted">
        {label}
      </span>
      {typeof value === "string" ? (
        <span className="text-right text-[20px] font-bold tracking-tight text-patient-ink">
          {value}
        </span>
      ) : (
        value
      )}
    </div>
  );
}
