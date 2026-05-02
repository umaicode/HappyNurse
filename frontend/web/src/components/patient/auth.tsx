"use client";

import { useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { CheckCircle } from "lucide-react";
import { verifyPatient } from "@/features/auth/api";
import { usePatientStore } from "@/features/patient/stores/patient";

export default function Auth() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const setPatient = usePatientStore((state) => state.setPatient);
  const devPatientId = Number(searchParams.get("patientId")) || null;
  const resolvedPatientId = devPatientId;
  const isPrefill = searchParams.get("prefill") === "1";

  const [name, setName] = useState(isPrefill ? "이승연" : "");
  const [birthDigits, setBirthDigits] = useState<string[]>(
    isPrefill ? ["9", "9", "0", "7", "2", "5"] : Array(6).fill(""),
  );
  const [focusIdx, setFocusIdx] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
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

  const filledCount = birthDigits.filter(Boolean).length;
  const isComplete = filledCount === 6 && name.trim().length > 0;

  const handleSubmit = async () => {
    if (!isComplete || !resolvedPatientId || submitting) return;
    setSubmitting(true);
    setErrorMessage("");
    try {
      const patient = await verifyPatient({
        patientId: resolvedPatientId,
        name: name.trim(),
        birthDate: birthDigits.join(""),
      });
      setPatient(patient);
      router.push("/patient/help");
    } catch {
      setErrorMessage("이름 또는 생년월일이 일치하지 않습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex flex-1 flex-col px-[22px] pt-5 pb-[50px]">
      <div className="self-start rounded-full bg-patient-accent-surface px-3 py-1.5">
        <span className="text-base font-extrabold tracking-wide text-patient-primary">
          환자용
        </span>
      </div>

      <div className="mt-7 flex items-center gap-2 rounded-xl bg-green-50 px-3.5 py-2.5">
        <CheckCircle className="size-5 shrink-0 text-green-500" strokeWidth={2.5} />
        <span className="text-base font-bold text-green-600">
          NFC 태깅이 완료되었습니다
        </span>
      </div>

      <div className="flex flex-1 flex-col justify-center gap-7 mb-20">
        <h1 className="text-[26px] leading-[1.35] font-extrabold tracking-tight text-patient-ink">
          이름과 생년월일을 입력해주세요
        </h1>

        <div className="flex flex-col gap-2">
          <label className="text-lg font-bold text-patient-sub">이름</label>
          <input
            type="text"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="이름을 입력하세요"
            className="rounded-xl border-[1.5px] border-patient-line bg-white px-3.5 py-3 text-[20px] font-bold tracking-tight text-patient-ink outline-none placeholder:text-patient-fade focus:border-patient-primary"
          />
        </div>

        <div className="flex flex-col gap-2">
          <label className="text-lg font-bold text-patient-sub">
            생년월일 6자리
          </label>
          <div className="flex w-full gap-1.5">
            {birthDigits.map((digit, index) => {
              const isFocus = focusIdx === index;
              return (
                <div
                  key={index}
                  className={`relative flex h-[52px] flex-1 items-center justify-center rounded-[10px] bg-white ${
                    isFocus
                      ? "border-2 border-patient-primary"
                      : "border-[1.5px] border-patient-line"
                  }`}
                >
                  {digit ? (
                    <span className="text-[19px] font-extrabold text-patient-ink">
                      {digit}
                    </span>
                  ) : (
                    <span className="size-[5px] rounded-full bg-patient-fade" />
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
                    onFocus={() => setFocusIdx(index)}
                    onChange={(event) =>
                      handleBirthChange(index, event.target.value)
                    }
                    onKeyDown={(event) => handleBirthKeyDown(index, event)}
                    onPaste={handleBirthPaste}
                    className="absolute inset-0 size-full rounded-[10px] bg-transparent text-center text-transparent caret-transparent outline-none"
                  />
                </div>
              );
            })}
          </div>
          {errorMessage ? (
            <p className="mt-1 text-sm font-bold text-red-500">{errorMessage}</p>
          ) : null}
        </div>
      </div>

      <button
        type="button"
        disabled={!isComplete || submitting}
        onClick={handleSubmit}
        className="h-14 w-full rounded-[14px] bg-patient-primary text-[20px] font-bold tracking-tight text-white transition-colors hover:bg-[#0F1F7A] disabled:cursor-default disabled:bg-[#C8CBD4]"
      >
        다음
      </button>
    </div>
  );
}
