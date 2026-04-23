"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Check } from "lucide-react";
import { nurseMock, patientMock } from "@/mockup/patient";

export default function Complete() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const patientName = searchParams.get("name") ?? "환자이름";
  const requestContent = searchParams.get("request") ?? "";
  const sentAt = searchParams.get("sentAt") ?? "";

  const rows = [
    { label: "환자", value: `${patientName} · ${patientMock.room}` },
    { label: "담당 간호사", value: nurseMock.name },
    { label: "요청 내용", value: requestContent },
    { label: "전송 시각", value: sentAt },
  ];

  return (
    <div className="flex flex-1 flex-col px-5 pt-10">
      <div className="flex flex-col items-center gap-10">
        <div className="flex size-[88px] items-center justify-center rounded-full bg-[#58ab8f]">
          <Check className="size-12 text-white" strokeWidth={3} />
        </div>

        <h1 className="text-center text-[22px] leading-8 font-bold tracking-tight text-content-primary">
          간호사에게
          <br />
          요청이 전달되었어요
        </h1>
      </div>

      <div className="mt-12 flex flex-col rounded-xl border border-border-subtle bg-white px-4">
        {rows.map((row, index) => (
          <div
            key={row.label}
            className={`flex items-start justify-between gap-4 py-4 ${
              index < rows.length - 1 ? "border-b border-[#f3f4f6]" : ""
            }`}
          >
            <span className="text-xl font-medium text-content-quaternary">
              {row.label}
            </span>
            <span className="whitespace-pre-line text-right text-xl font-bold text-content-primary">
              {row.value}
            </span>
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={() => router.push("/patient/help")}
        className="mt-auto w-full max-w-[300px] self-center rounded-2xl bg-[#1428a0] py-[18px] text-2xl font-bold text-white transition-colors hover:bg-[#101E7A]"
      >
        확인
      </button>
    </div>
  );
}
