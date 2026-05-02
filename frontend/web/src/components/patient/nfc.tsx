"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AlertCircle, Smartphone } from "lucide-react";
import { getNfcEntry } from "@/features/patient/api";

type Status = "verifying" | "error";

export default function Nfc() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const patientId = Number(searchParams.get("patientId"));

  const [status, setStatus] = useState<Status>("verifying");
  const [errorMessage, setErrorMessage] = useState("");

  const verify = useCallback(async () => {
    if (!patientId || !Number.isFinite(patientId) || patientId <= 0) {
      setStatus("error");
      setErrorMessage("팔찌 정보를 인식하지 못했습니다. 단말기에 다시 태깅해주세요.");
      return;
    }

    setStatus("verifying");
    setErrorMessage("");

    try {
      await getNfcEntry(patientId);
      router.replace(`/patient/verify?patientId=${patientId}`);
    } catch {
      setStatus("error");
      setErrorMessage("환자 정보를 확인할 수 없습니다. 다시 시도해주세요.");
    }
  }, [patientId, router]);

  useEffect(() => {
    verify();
  }, [verify]);

  const isError = status === "error";

  return (
    <div className="flex flex-1 flex-col gap-5 px-[22px] pt-5 pb-[50px]">
      <div className="self-start rounded-full bg-patient-accent-surface px-3 py-1.5">
        <span className="text-sm font-extrabold tracking-wide text-patient-primary">
          환자용
        </span>
      </div>

      <div className="mt-6 flex flex-1 flex-col items-center justify-center gap-8 text-center">
        <div
          className={`flex size-32 items-center justify-center rounded-full ${
            isError ? "bg-red-50" : "bg-patient-accent-surface"
          }`}
        >
          {isError ? (
            <AlertCircle className="size-14 text-red-500" strokeWidth={2} />
          ) : (
            <Smartphone
              className="size-14 animate-pulse text-patient-primary"
              strokeWidth={2}
            />
          )}
        </div>

        <div className="flex flex-col gap-3">
          <h1 className="text-[22px] leading-[1.35] font-extrabold tracking-tight text-patient-ink">
            {isError ? "NFC 인증에 실패했습니다" : "환자 정보를 확인 중입니다"}
          </h1>
          <p className="text-base font-medium text-patient-muted">
            {isError ? errorMessage : "잠시만 기다려 주세요"}
          </p>
        </div>
      </div>

      {isError ? (
        <button
          type="button"
          onClick={verify}
          className="h-14 w-full rounded-[14px] bg-patient-primary text-[20px] font-bold tracking-tight text-white transition-colors hover:bg-[#0F1F7A]"
        >
          다시 시도
        </button>
      ) : null}
    </div>
  );
}
