import { AlertCircle } from "lucide-react";

export default function InvalidPage() {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-6 px-[22px] pt-5 pb-[50px]">
      <div className="flex size-[60px] items-center justify-center rounded-full bg-patient-accent-surface text-patient-primary">
        <AlertCircle className="size-8" strokeWidth={2.5} />
      </div>
      <div className="flex flex-col items-center gap-2 text-center">
        <h1 className="text-[22px] leading-[1.35] font-extrabold tracking-tight text-patient-ink">
          NFC 태깅이 인식되지 않았어요
        </h1>
        <p className="text-lg font-medium text-patient-muted">
          팔찌를 다시 태깅해주세요
        </p>
      </div>
    </div>
  );
}
