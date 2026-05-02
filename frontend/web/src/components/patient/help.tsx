"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ChevronDown, ChevronLeft } from "lucide-react";
import { getButtons, getFaq, submitSymptom } from "@/features/patient/api";
import { usePatientStore } from "@/features/patient/stores/patient";
import type { FaqItem, SymptomButton } from "@/features/patient/types/patient";

type TabKey = "form" | "faq";

const genderLabel = (gender: string): string => {
  if (gender === "male") return "남";
  if (gender === "female") return "여";
  return "";
};

const genderChipClass = (gender: string): string => {
  if (gender === "male") return "bg-blue-100 text-blue-700";
  if (gender === "female") return "bg-pink-100 text-pink-700";
  return "bg-patient-slate-surface text-patient-slate";
};

export default function Help() {
  const router = useRouter();
  const patient = usePatientStore((state) => state.patient);

  useEffect(() => {
    if (!patient) router.replace("/patient/verify");
  }, [patient, router]);

  const patientId = patient?.patientId ?? 0;
  const patientName = patient?.patientName ?? "";
  const roomName = patient?.roomName ?? "";
  const gender = patient?.gender ?? "";
  const diseaseName = patient?.diseaseName ?? "";
  const surgeryName = patient?.surgeryName ?? "";
  const chiefComplaint = patient?.chiefComplaint ?? "";
  const assignedNurseName = patient?.assignedNurseName ?? "";
  const [buttons, setButtons] = useState<SymptomButton[]>([]);
  const [selectedButtonId, setSelectedButtonId] = useState<number | null>(null);
  const [directInput, setDirectInput] = useState("");
  const [activeTab, setActiveTab] = useState<TabKey>("form");
  const [faqs, setFaqs] = useState<FaqItem[]>([]);
  const [openFaqIndex, setOpenFaqIndex] = useState<number | null>(null);
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getButtons()
      .then((list) =>
        setButtons([...list].sort((a, b) => a.displayOrder - b.displayOrder)),
      )
      .catch(() => setErrorMessage("증상 버튼을 불러올 수 없습니다."));
  }, []);

  useEffect(() => {
    if (!patientId) return;
    getFaq(patientId)
      .then((res) => setFaqs(res.items))
      .catch(() => {});
  }, [patientId]);

  const toggleSymptom = (buttonId: number) => {
    setSelectedButtonId((prev) => (prev === buttonId ? null : buttonId));
  };

  const handleSubmit = async () => {
    const trimmedInput = directInput.trim();
    if (selectedButtonId === null && !trimmedInput) return;
    if (!patientId || submitting) return;

    setSubmitting(true);
    setErrorMessage("");
    try {
      // 백엔드에서 buttonId · symptomText 동시 전송을 허용하도록 수정 진행 중.
      await submitSymptom(patientId, {
        buttonId: selectedButtonId ?? undefined,
        symptomText: trimmedInput || undefined,
      });

      const now = new Date();
      const sentAt = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
      const selectedLabel =
        buttons.find((button) => button.buttonId === selectedButtonId)?.label ?? "";

      const params = new URLSearchParams({ sentAt, symptoms: selectedLabel });
      if (trimmedInput) params.set("direct", trimmedInput);
      router.push(`/patient/complete?${params.toString()}`);
    } catch {
      setErrorMessage("증상 전송에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  const hasRequest = selectedButtonId !== null || !!directInput.trim();

  return (
    <div className="flex flex-1 min-h-0 flex-col gap-5 px-[22px] pt-5 pb-[50px]">
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
        <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
          <div className="flex flex-wrap items-center gap-1.5">
            <span className="text-[22px] font-extrabold tracking-tight text-patient-ink">
              {patientName}
            </span>
            {gender ? (
              <span
                className={`rounded-full px-2 py-[2px] text-[16px] font-bold ${genderChipClass(gender)}`}
              >
                {genderLabel(gender)}
              </span>
            ) : null}
            {roomName ? (
              <span className="rounded-full bg-patient-slate-surface px-2 py-[2px] text-[16px] font-bold text-patient-slate">
                {roomName}
              </span>
            ) : null}
          </div>
          <div className="flex items-center gap-2">
            <span className="text-lg font-bold tracking-tight text-patient-muted">
              담당 간호사
            </span>
            <span className="text-lg font-bold tracking-tight text-patient-ink">
              {assignedNurseName}
            </span>
          </div>
        </div>
        <div className="h-px bg-patient-hairline" />
        <button
          type="button"
          onClick={() => setDetailsOpen((prev) => !prev)}
          className="flex w-full items-center gap-2 py-1 text-left"
        >
          <span className="text-lg font-bold tracking-tight text-patient-muted">
            병명
          </span>
          <span className="text-[18px] font-bold tracking-tight text-patient-ink">
            {diseaseName}
          </span>
          <ChevronDown
            className={`ml-auto size-4 shrink-0 text-patient-muted transition-transform duration-200 ${
              detailsOpen ? "rotate-180" : ""
            }`}
            strokeWidth={2.5}
          />
        </button>
        {detailsOpen ? (
          <>
            <div className="h-px bg-patient-hairline" />
            <div className="flex items-center gap-2 py-1">
              <span className="text-lg font-bold tracking-tight text-patient-muted">
                주 증상
              </span>
              <span className="text-[18px] font-bold tracking-tight text-patient-ink">
                {chiefComplaint}
              </span>
            </div>
            <div className="h-px bg-patient-hairline" />
            <div className="flex items-center gap-2 py-1">
              <span className="text-lg font-bold tracking-tight text-patient-muted">
                수술명
              </span>
              <span className="text-[18px] font-bold tracking-tight text-patient-ink">
                {surgeryName}
              </span>
            </div>
          </>
        ) : null}
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
                      ? "border border-transparent bg-[#d4e0f7] shadow-[0_4px_6px_rgba(0,0,0,0.13)]"
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
            <label className="text-lg font-bold text-patient-sub">
              그 외 증상
            </label>
            <textarea
              value={directInput}
              onChange={(event) => setDirectInput(event.target.value)}
              placeholder="증상이나 필요한 도움을 입력해주세요"
              className="min-h-[72px] w-full resize-none rounded-xl border border-[#cdcfd7] bg-white px-3 py-2.5 text-lg font-medium leading-relaxed text-patient-ink placeholder:text-patient-fade outline-none transition-colors focus:border-patient-primary"
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
        <div className="flex flex-1 min-h-0 mt-2 flex-col gap-[17px] overflow-y-auto [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
          {faqs.length === 0 ? (
            <p className="shrink-0 py-10 text-center text-lg font-bold text-patient-muted">
              등록된 FAQ가 없습니다.
            </p>
          ) : (
            faqs.map((faq, index) => {
              const isOpen = openFaqIndex === index;
              return (
                <div
                  key={index}
                  className="shrink-0 overflow-hidden rounded-xl bg-[#F8F8F8] shadow-[0_1px_4px_rgba(0,0,0,0.06)]"
                >
                  <button
                    type="button"
                    onClick={() =>
                      setOpenFaqIndex((prev) => (prev === index ? null : index))
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
