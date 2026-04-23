"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ChevronLeft, X } from "lucide-react";
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

  const openedFaq = useMemo(
    () => faqs.find((item) => item.id === openFaqId) ?? null,
    [faqs, openFaqId],
  );

  useEffect(() => {
    if (!openFaqId) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpenFaqId(null);
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [openFaqId]);

  const toggleSymptom = (id: string) => {
    setSelectedSymptoms((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    );
  };

  const handleSubmit = () => {
    const selectedLabels = symptomsMock
      .filter((symptom) => selectedSymptoms.includes(symptom.id))
      .map((symptom) => symptom.label);
    const trimmedInput = directInput.trim();
    const contents = trimmedInput
      ? [...selectedLabels, trimmedInput]
      : selectedLabels;
    const requestContent = contents.join("\n");
    if (!requestContent) return;

    const now = new Date();
    const hours = String(now.getHours()).padStart(2, "0");
    const minutes = String(now.getMinutes()).padStart(2, "0");
    const sentAt = `${hours}:${minutes}`;

    const params = new URLSearchParams({
      name: patientName,
      request: requestContent,
      sentAt,
    });
    router.push(`/patient/complete?${params.toString()}`);
  };

  return (
    <div className="flex flex-1 flex-col gap-5">
      <div className="relative flex h-10 items-center justify-center">
        <button
          type="button"
          onClick={() => router.push("/patient")}
          aria-label="뒤로가기"
          className="absolute left-0 flex size-10 items-center justify-center"
        >
          <ChevronLeft className="size-7 text-content-primary" />
        </button>
        <h1 className="text-3xl font-bold tracking-tight text-content-primary">
          도움 요청
        </h1>
      </div>

      <div className="flex items-center rounded-xl bg-patient-accent-surface px-3.5 py-3">
        <div className="flex flex-1 flex-col gap-0.5">
          <p className="text-xl font-bold text-content-primary">
            {patientName} · {patientMock.room}
          </p>
          <p className="text-base font-bold text-content-primary">
            {nurseMock.role} {nurseMock.name}
          </p>
          <p className="text-base font-medium text-content-secondary">
            {patientMock.ward} · {patientMock.surgeryType}
          </p>
        </div>
      </div>

      <div className="flex gap-2">
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
              className={`rounded-full px-4 py-2 text-base font-bold transition-colors ${
                isActive
                  ? "bg-[#1428a0] text-white"
                  : "bg-[#ededf5] text-content-quaternary"
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {activeTab === "form" ? (
        <>
          <div className="grid grid-cols-2 gap-2.5">
            {symptomsMock.map((symptom) => {
              const isSelected = selectedSymptoms.includes(symptom.id);
              return (
                <button
                  key={symptom.id}
                  type="button"
                  onClick={() => toggleSymptom(symptom.id)}
                  className={`flex h-[76px] items-center rounded-2xl border p-3 text-left text-xl font-medium tracking-tight transition-colors ${
                    isSelected
                      ? "border-2 border-[#cdd8e8] bg-[#ededf5] text-content-primary"
                      : "border-border-subtle bg-white text-content-primary"
                  }`}
                >
                  {symptom.label}
                </button>
              );
            })}
          </div>

          <div className="flex flex-col gap-3">
            <label className="text-base font-bold text-content-primary">
              직접 입력하기
            </label>
            <textarea
              value={directInput}
              onChange={(event) => setDirectInput(event.target.value)}
              placeholder="[증상을 입력해주세요]"
              className="h-28 w-full resize-none rounded-xl border border-border-subtle px-3.5 py-3 text-xl text-content-primary placeholder:text-content-muted focus:outline-none focus:ring-1 focus:ring-brand-primary/30"
            />
          </div>

          <button
            type="button"
            onClick={handleSubmit}
            className="mt-auto w-full max-w-[300px] self-center rounded-2xl bg-[#1428a0] py-[18px] text-2xl font-bold tracking-tight text-white transition-colors hover:bg-[#101E7A]"
          >
            간호사에게 전송
          </button>
        </>
      ) : (
        <div className="flex max-h-[460px] flex-col gap-3 overflow-y-auto pr-1">
          {faqs.length === 0 ? (
            <p className="py-10 text-center text-base text-content-muted">
              등록된 FAQ가 없습니다.
            </p>
          ) : (
            faqs.map((faq) => (
              <button
                key={faq.id}
                type="button"
                onClick={() => setOpenFaqId(faq.id)}
                className="rounded-2xl border border-border-subtle bg-white p-4 text-left text-lg font-medium tracking-tight text-content-primary transition-colors hover:bg-[#f8f9fc]"
              >
                {faq.question}
              </button>
            ))
          )}
        </div>
      )}

      {openedFaq ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-5"
          onClick={() => setOpenFaqId(null)}
        >
          <div
            className="flex w-full max-w-[340px] flex-col gap-4 rounded-2xl bg-white p-5"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3">
              <p className="text-xl font-bold tracking-tight text-content-primary">
                {openedFaq.question}
              </p>
              <button
                type="button"
                aria-label="닫기"
                onClick={() => setOpenFaqId(null)}
                className="flex size-8 shrink-0 items-center justify-center rounded-full hover:bg-[#ededf5]"
              >
                <X className="size-5 text-content-primary" />
              </button>
            </div>
            <p className="whitespace-pre-line text-base leading-relaxed text-content-secondary">
              {openedFaq.answer}
            </p>
            <button
              type="button"
              onClick={() => setOpenFaqId(null)}
              className="mt-1 w-full rounded-2xl bg-[#1428a0] py-3 text-lg font-bold tracking-tight text-white transition-colors hover:bg-[#101E7A]"
            >
              닫기
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
