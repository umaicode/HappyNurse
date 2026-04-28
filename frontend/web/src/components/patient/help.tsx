"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ChevronDown, ChevronLeft, Mic } from "lucide-react";
import { faqMock } from "@/mockup/patient";
import { getButtons, submitSymptom } from "@/features/patient/api";
import type { SymptomButton } from "@/features/patient/types/patient";
import { formatHHmm } from "@/lib/time";

type TabKey = "form" | "faq";

export default function Help() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const patientId = Number(searchParams.get("patientId"));
  const patientName = searchParams.get("name") ?? "";
  const roomName = searchParams.get("roomName") ?? "";
  const assignedNurseName = searchParams.get("assignedNurseName") ?? "";

  const [buttons, setButtons] = useState<SymptomButton[]>([]);
  const [selectedButtonId, setSelectedButtonId] = useState<number | null>(null);
  const [directInput, setDirectInput] = useState("");
  const [activeTab, setActiveTab] = useState<TabKey>("form");
  const [openFaqId, setOpenFaqId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const faqs = faqMock;

  useEffect(() => {
    getButtons()
      .then((list) =>
        setButtons([...list].sort((a, b) => a.displayOrder - b.displayOrder)),
      )
      .catch(() => setErrorMessage("증상 버튼을 불러올 수 없습니다."));
  }, []);

  const toggleSymptom = (buttonId: number) => {
    setSelectedButtonId((prev) => (prev === buttonId ? null : buttonId));
  };

  const trimmedDirect = directInput.trim();
  const hasRequest = selectedButtonId !== null || trimmedDirect.length > 0;

  const handleSubmit = async () => {
    if (!hasRequest || !patientId || submitting) return;
    setSubmitting(true);
    setErrorMessage("");
    try {
      const submitted = await submitSymptom(
        patientId,
        selectedButtonId
          ? { buttonId: selectedButtonId }
          : { symptomText: trimmedDirect },
      );
      const selected = buttons.find(
        (button) => button.buttonId === selectedButtonId,
      );
      const query = new URLSearchParams({
        name: patientName,
        roomName,
        assignedNurseName,
        sentAt: formatHHmm(submitted.submittedAt),
      });
      if (selected) query.set("requestLabel", selected.label);
      if (trimmedDirect && !selectedButtonId)
        query.set("direct", trimmedDirect);
      router.push(`/patient/complete?${query.toString()}`);
    } catch {
      setErrorMessage("요청을 전송할 수 없습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

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

      <div className="flex flex-col gap-1 rounded-2xl bg-[#F9FAFB] px-4 py-2 shadow-[0_1px_6px_rgba(0,0,0,0.08)]">
        <div className="flex flex-wrap items-center gap-1">
          <span className="text-[22px] font-extrabold tracking-tight text-patient-ink">
            {patientName}
          </span>
          <span className="rounded-full mx-1 bg-patient-slate-surface px-2 py-[2px] text-[16px] font-bold text-patient-slate">
            {roomName}
          </span>
        </div>
        <div className="h-px bg-patient-hairline" />
        <div className="flex items-center gap-2">
          <span className="text-lg font-bold tracking-tight text-patient-muted">
            담당 간호사
          </span>
          <span className="text-lg font-bold tracking-tight text-patient-ink">
            {assignedNurseName || "-"}
          </span>
        </div>
      </div>

      <div className="flex border-b border-patient-hairline">
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
              className={`relative flex flex-1 items-center justify-center pb-3 text-lg font-bold tracking-tight transition-colors duration-200 ${
                isActive ? "text-patient-primary" : "text-patient-muted"
              }`}
            >
              {tab.label}
              {isActive && (
                <span className="absolute bottom-0 left-0 right-0 h-[2.5px] rounded-full bg-patient-primary" />
              )}
            </button>
          );
        })}
      </div>

      {activeTab === "form" ? (
        <div className="flex flex-1 flex-col gap-[15px] overflow-hidden">
          <div className="grid grid-cols-2 gap-5 mx-2 mt-2">
            {buttons.map((button) => {
              const isSelected = selectedButtonId === button.buttonId;
              return (
                <button
                  key={button.buttonId}
                  type="button"
                  onClick={() => toggleSymptom(button.buttonId)}
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
                    {button.label}
                  </span>
                  <span
                    className={`text-sm font-medium tracking-tight ${
                      isSelected
                        ? "text-patient-slate opacity-85"
                        : "text-patient-muted"
                    }`}
                  >
                    {button.description}
                  </span>
                </button>
              );
            })}
          </div>

          <div className="mt-4 flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <label className="text-lg font-bold text-patient-sub">
                그 외 증상
              </label>
              <button
                type="button"
                aria-label="음성으로 입력"
                disabled={selectedButtonId !== null}
                className={`flex size-9 items-center justify-center rounded-full transition-colors ${
                  selectedButtonId !== null
                    ? "cursor-not-allowed bg-[#e5e7eb] text-patient-fade"
                    : "bg-patient-primary text-white hover:bg-[#0F1F7A]"
                }`}
              >
                <Mic className="size-[18px]" strokeWidth={2.2} />
              </button>
            </div>
            <textarea
              value={selectedButtonId !== null ? "" : directInput}
              onChange={(event) => setDirectInput(event.target.value)}
              placeholder="증상이나 필요한 도움을 입력해주세요"
              disabled={selectedButtonId !== null}
              className={`min-h-[72px] w-full resize-none rounded-xl border px-3 py-2.5 text-lg font-medium leading-relaxed outline-none transition-colors ${
                selectedButtonId !== null
                  ? "border-[#e0e2e6] bg-[#f3f4f6] text-patient-muted placeholder:text-patient-fade cursor-not-allowed"
                  : "border-[#cdcfd7] bg-white text-patient-ink placeholder:text-patient-fade focus:border-patient-primary"
              }`}
            />
            {errorMessage ? (
              <p className="text-sm font-bold text-red-500">{errorMessage}</p>
            ) : null}
          </div>

          <button
            type="button"
            onClick={handleSubmit}
            disabled={!hasRequest || submitting}
            className="mt-auto h-14 w-full rounded-[14px] bg-patient-primary text-[20px] font-bold tracking-tight text-white transition-colors hover:bg-[#0F1F7A] disabled:cursor-default disabled:bg-[#C8CBD4]"
          >
            간호사에게 전송
          </button>
        </div>
      ) : (
        <div className="flex flex-1 mt-2 flex-col gap-[17px] overflow-auto">
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
