import { AlertCircle } from "lucide-react";

export default function Invalid() {
  return (
    <div className="flex flex-1 flex-col gap-5 px-[22px] pt-5 pb-[50px]">
      <div className="self-start rounded-full bg-patient-accent-surface px-3 py-1.5">
        <span className="text-base font-extrabold tracking-wide text-patient-primary">
          환자용
        </span>
      </div>

      <div className="mt-6 flex flex-1 flex-col items-center justify-center gap-8 text-center">
        <div className="flex size-32 items-center justify-center rounded-full bg-red-50">
          <AlertCircle className="size-14 text-red-500" strokeWidth={2} />
        </div>

        <div className="flex flex-col gap-3">
          <h1 className="text-[22px] leading-[1.35] font-extrabold tracking-tight text-patient-ink">
            NFC 인증에 실패했습니다
          </h1>
          <p className="text-base font-medium text-patient-muted">
            팔찌를 다시 태깅해주세요
          </p>
        </div>
      </div>
    </div>
  );
}
