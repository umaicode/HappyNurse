"use client";

import { useCallback, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Smartphone } from "lucide-react";
import { getNfcEntry } from "@/features/patient/api";

export default function Nfc() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const verify = useCallback(async () => {
    if (!token) {
      router.replace("/patient/invalid");
      return;
    }

    try {
      const entry = await getNfcEntry(token);
      router.replace(`/patient/verify?patientId=${entry.patientId}`);
    } catch {
      router.replace("/patient/invalid");
    }
  }, [token, router]);

  useEffect(() => {
    verify();
  }, [verify]);

  return (
    <div className="flex flex-1 flex-col gap-5 px-[22px] pt-5 pb-[50px]">
      <div className="self-start rounded-full bg-patient-accent-surface px-3 py-1.5">
        <span className="text-sm font-extrabold tracking-wide text-patient-primary">
          환자용
        </span>
      </div>

      <div className="mt-6 flex flex-1 flex-col items-center justify-center gap-8 text-center">
        <div className="flex size-32 items-center justify-center rounded-full bg-patient-accent-surface">
          <Smartphone
            className="size-14 animate-pulse text-patient-primary"
            strokeWidth={2}
          />
        </div>

        <div className="flex flex-col gap-3">
          <h1 className="text-[22px] leading-[1.35] font-extrabold tracking-tight text-patient-ink">
            환자 정보를 확인 중입니다
          </h1>
          <p className="text-base font-medium text-patient-muted">
            잠시만 기다려 주세요
          </p>
        </div>
      </div>
    </div>
  );
}
