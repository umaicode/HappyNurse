"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { CircleCheck } from "lucide-react";
import { patientMock } from "@/mockup/patient";

export default function Auth() {
  const router = useRouter();
  const [name, setName] = useState(patientMock.name);
  const [birthDigits, setBirthDigits] = useState<string[]>(Array(6).fill(""));
  const inputRefs = useRef<Array<HTMLInputElement | null>>([]);

  const handleBirthChange = (index: number, value: string) => {
    const digit = value.replace(/[^0-9]/g, "").slice(-1);
    const nextDigits = [...birthDigits];
    nextDigits[index] = digit;
    setBirthDigits(nextDigits);
    if (digit && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleBirthKeyDown = (
    index: number,
    event: React.KeyboardEvent<HTMLInputElement>,
  ) => {
    if (event.key === "Backspace" && !birthDigits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handleBirthPaste = (event: React.ClipboardEvent<HTMLInputElement>) => {
    event.preventDefault();
    const pasted = event.clipboardData
      .getData("text")
      .replace(/[^0-9]/g, "")
      .slice(0, 6);
    if (!pasted) return;
    const nextDigits = Array(6).fill("");
    for (let i = 0; i < pasted.length; i += 1) nextDigits[i] = pasted[i];
    setBirthDigits(nextDigits);
    inputRefs.current[Math.min(pasted.length, 5)]?.focus();
  };

  return (
    <div className="flex flex-1 flex-col">
      <div className="self-start rounded-full bg-[#eff6ff] px-4 py-1.5">
        <span className="text-xl font-bold text-[#1428a0]">환자용</span>
      </div>

      <div className="mt-4 flex items-center gap-3 rounded-xl bg-patient-accent-surface px-4 py-3.5">
        <CircleCheck
          className="size-7 text-patient-success"
          strokeWidth={2.5}
        />
        <div className="flex flex-col gap-0.5">
          <span className="text-sm font-bold text-patient-success">
            태깅 완료
          </span>
          <span className="text-base font-medium text-content-primary">
            {patientMock.name}님의 팔찌가 인식되었습니다
          </span>
        </div>
      </div>

      <h1 className="mt-20 text-[22px] leading-8 font-bold tracking-tight text-content-primary">
        이름과 생년월일을 입력해주세요
      </h1>

      <div className="mt-8 flex flex-col gap-2">
        <label className="text-base font-bold text-content-primary">이름</label>
        <input
          type="text"
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="이름을 입력하세요"
          className="rounded-xl border-[1.5px] border-border-subtle px-4 py-3.5 text-2xl text-content-primary outline-none placeholder:text-content-tertiary focus:border-[#2563eb]"
        />
      </div>

      <div className="mt-7 flex flex-col gap-3">
        <label className="text-base font-bold text-content-primary">
          생년월일 6자리
        </label>
        <div className="flex w-full gap-2">
          {birthDigits.map((digit, index) => (
            <div
              key={index}
              className={`relative flex h-14 flex-1 items-center justify-center rounded-xl bg-white ${
                index === 0
                  ? "border-2 border-[#2563eb]"
                  : "border-[1.5px] border-border-subtle"
              }`}
            >
              {digit ? (
                <span className="text-2xl font-bold text-content-primary">
                  {digit}
                </span>
              ) : (
                <span className="size-1.5 rounded-full bg-content-primary/60" />
              )}
              <input
                ref={(element) => {
                  inputRefs.current[index] = element;
                }}
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={1}
                value={digit}
                onChange={(event) =>
                  handleBirthChange(index, event.target.value)
                }
                onKeyDown={(event) => handleBirthKeyDown(index, event)}
                onPaste={handleBirthPaste}
                className="absolute inset-0 size-full rounded-xl bg-transparent text-center text-transparent caret-transparent outline-none"
              />
            </div>
          ))}
        </div>
        <p className="text-base text-content-tertiary">
          예: 010429 (2001년 4월 29일생)
        </p>
      </div>

      <button
        type="button"
        onClick={() =>
          router.push(
            `/patient/help?name=${encodeURIComponent(name)}`,
          )
        }
        className="mt-auto w-full max-w-[300px] self-center rounded-2xl bg-[#1428a0] py-[18px] text-2xl font-bold tracking-tight text-white transition-colors hover:bg-[#101E7A]"
      >
        완료
      </button>
    </div>
  );
}
