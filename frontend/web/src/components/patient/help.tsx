"use client";

import { useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ChevronDown, ChevronLeft } from "lucide-react";
import {
  faqMock,
  nurseMock,
  patientMock,
  symptomsMock,
} from "@/mockup/patient";

type TabKey = "form" | "faq";

export default function Help() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const patientName = searchParams.get("name") ?? patientMock.name;
  const [selectedSymptoms, setSelectedSymptoms] = useState<string[]>([]);
  const [directInput, setDirectInput] = useState("");
  const [activeTab, setActiveTab] = useState<TabKey>("form");
  const [openFaqId, setOpenFaqId] = useState<string | null>(null);

  const faqs = useMemo(
    () =>
      faqMock.filter(
        (item) =>
          item.surgeryType === patientMock.surgeryType &&
          item.ward === patientMock.ward,
      ),
    [],
  );

  const toggleSymptom = (id: string) => {
    setSelectedSymptoms((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    );
  };

  const handleSubmit = () => {
    const trimmedInput = directInput.trim();
    if (selectedSymptoms.length === 0 && !trimmedInput) return;

    const now = new Date();
    const hours = String(now.getHours()).padStart(2, "0");
    const minutes = String(now.getMinutes()).padStart(2, "0");
    const sentAt = `${hours}:${minutes}`;

    const params = new URLSearchParams({
      name: patientName,
      symptoms: selectedSymptoms.join(","),
      sentAt,
    });
    if (trimmedInput) params.set("direct", trimmedInput);
    router.push(`/patient/complete?${params.toString()}`);
  };

  const requestCount = selectedSymptoms.length + (directInput.trim() ? 1 : 0);

  return (
    <div className="flex flex-1 flex-col gap-5 px-[22px] pt-5 pb-[50px]">
      <div className="relative flex h-12 items-center justify-center">
        <button
          type="button"
          onClick={() => router.push("/patient")}
          aria-label="뒤로가기"
          className="absolute left-0 flex size-12 items-center justify-center text-patient-ink"
        >
          <ChevronLeft className="size-6" strokeWidth={2} />
        </button>
        <h1 className="text-2xl font-extrabold tracking-tight text-patient-ink">
          도움 요청
        </h1>
      </div>

      <div className="flex flex-col gap-2 rounded-2xl bg-[#F9FAFB] px-4 py-2 shadow-[0_1px_6px_rgba(0,0,0,0.08)]">
        <div className="flex flex-wrap items-baseline gap-2">
          <span className="text-[20px] font-extrabold tracking-tight text-patient-ink">
            {patientName}
          </span>
          <span className="rounded-full bg-patient-slate-surface px-2.5 py-[3px] text-[16px] font-bold text-patient-slate">
            {patientMock.room}
          </span>
          <span className="text-lg font-bold text-patient-sub">
            {patientMock.ward} · {patientMock.surgeryType}
          </span>
        </div>
        <div className="h-px bg-patient-hairline" />
        <div className="flex items-center gap-2">
          <span className="text-lg font-bold tracking-tight text-patient-muted">
            담당 간호사
          </span>
          <span className="text-lg font-bold tracking-tight text-patient-ink">
            {nurseMock.name}
          </span>
        </div>
      </div>

      <div className="flex gap-4">
        {(
          [
            { key: "form", label: "요청하기" },
            { key: "faq", label: "FAQ" },
          ] as const
        ).map((tab) => {
          const isActive = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              className={`flex h-10 flex-1 items-center justify-center rounded-[10px] text-lg font-bold tracking-tight transition-colors ${
                isActive
                  ? "bg-patient-primary text-white"
                  : "bg-[#EFEFF0] text-[#2C3870]"
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {activeTab === "form" ? (
        <div className="flex flex-1 flex-col gap-[15px] overflow-hidden">
          <div className="flex items-baseline justify-between">
            <span className="text-lg font-extrabold tracking-tight text-patient-ink">
              요청할 항목을 선택하세요
            </span>
            <span className="text-sm font-semibold text-[#616774]">
              중복 선택 가능
            </span>
          </div>

          <div className="grid grid-cols-2 gap-5 mx-2">
            {symptomsMock.map((symptom) => {
              const isSelected = selectedSymptoms.includes(symptom.id);
              return (
                <button
                  key={symptom.id}
                  type="button"
                  onClick={() => toggleSymptom(symptom.id)}
                  className={`flex h-[62px] flex-col justify-center gap-0.5 rounded-xl px-3.5 py-3.5 text-left transition-all ${
                    isSelected
                      ? "border border-transparent bg-[#ebf0f9] shadow-[0_4px_6px_rgba(0,0,0,0.13)]"
                      : "border border-transparent bg-white shadow-[0_2px_8px_rgba(0,0,0,0.13)]"
                  }`}
                >
                  <span
                    className={`text-lg font-extrabold tracking-tight ${
                      isSelected ? "text-patient-slate" : "text-patient-ink"
                    }`}
                  >
                    {symptom.label}
                  </span>
                  <span
                    className={`text-sm font-medium tracking-tight ${
                      isSelected
                        ? "text-patient-slate opacity-85"
                        : "text-patient-muted"
                    }`}
                  >
                    {symptom.sub}
                  </span>
                </button>
              );
            })}
          </div>

          <div className="mt-4 flex flex-col gap-3">
            <label className="text-lg font-bold text-patient-sub">
              직접 입력
            </label>
            <textarea
              value={directInput}
              onChange={(event) => setDirectInput(event.target.value)}
              placeholder="증상이나 필요한 도움을 입력해주세요"
              className="min-h-[72px] w-full resize-none rounded-xl border border-[#cdcfd7] bg-white px-3 py-2.5 text-lg font-medium leading-relaxed text-patient-ink outline-none placeholder:text-patient-fade focus:border-patient-primary"
            />
          </div>

          <button
            type="button"
            onClick={handleSubmit}
            disabled={requestCount === 0}
            className="mt-auto h-14 w-full rounded-[14px] bg-patient-primary text-[20px] font-bold tracking-tight text-white transition-colors hover:bg-[#0F1F7A] disabled:cursor-default disabled:bg-[#C8CBD4]"
          >
            간호사에게 전송
            {requestCount > 0 ? ` · ${requestCount}건` : ""}
          </button>
        </div>
      ) : (
        <div className="flex flex-1 flex-col gap-[17px] overflow-auto">
          {faqs.length === 0 ? (
            <p className="py-10 text-center text-lg font-bold text-patient-muted">
              등록된 FAQ가 없습니다.
            </p>
          ) : (
            faqs.map((faq) => {
              const isOpen = openFaqId === faq.id;
              return (
                <div
                  key={faq.id}
                  className="overflow-hidden rounded-xl bg-[#F8F8F8] shadow-[0_1px_4px_rgba(0,0,0,0.06)]"
                >
                  <button
                    type="button"
                    onClick={() =>
                      setOpenFaqId((prev) => (prev === faq.id ? null : faq.id))
                    }
                    className="flex w-full items-center gap-2.5 px-3.5 py-3 text-left"
                  >
                    <span className="flex-1 text-[16px] font-bold tracking-tight text-patient-ink">
                      {faq.question}
                    </span>
                    <ChevronDown
                      className={`size-4 shrink-0 text-patient-muted transition-transform duration-200 ${
                        isOpen ? "rotate-180" : ""
                      }`}
                      strokeWidth={2.5}
                    />
                  </button>
                  {isOpen ? (
                    <div className="border-t border-patient-hairline bg-white px-3.5 pt-2.5 pb-3 text-base font-medium leading-relaxed text-patient-sub">
                      {faq.answer}
                    </div>
                  ) : null}
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
