"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ChevronDown,
  ChevronLeft,
  HelpCircle,
  type LucideIcon,
} from "lucide-react";
import type { ComponentType, SVGProps } from "react";
import {
  FaHeadSideMask,
  FaFaceTired,
  FaSyringe,
  FaRestroom,
  FaBedPulse,
} from "react-icons/fa6";
import { BsBandaidFill } from "react-icons/bs";
import { getButtons, getFaq, submitSymptom } from "@/features/patient/api";
import { usePatientStore } from "@/features/patient/stores/patient";
import type { FaqItem, SymptomButton } from "@/features/patient/types/patient";

type TabKey = "form" | "faq"

const INTENT_LABEL_COLORS: Record<string, string> = {
  "정의":      "bg-[#F2F7FA] text-[#3D5466]",
  "증상":      "bg-[#F9EEED] text-[#6A3F3C]",
  "원인":      "bg-[#FAF1E6] text-[#6A4A30]",
  "진단":      "bg-[#FAF5E4] text-[#65572A]",
  "치료":      "bg-[#EEF5EB] text-[#3F5A3B]",
  "약물":      "bg-[#EBF4F2] text-[#35544F]",
  "예방":      "bg-[#EEF6F7] text-[#39555B]",
  "식이, 생활": "bg-[#F4F7E7] text-[#545930]",
  "운동":      "bg-[#F1EEF6] text-[#4C4666]",
  "재활":      "bg-[#F4ECF5] text-[#5B4361]",
  "검진":      "bg-[#F8EEF2] text-[#6B4554]",
} as const;

type SymptomIcon = LucideIcon | ComponentType<SVGProps<SVGSVGElement>>;

const SYMPTOM_ICONS: Array<{ keyword: string; Icon: SymptomIcon }> = [
  { keyword: "통증",     Icon: FaFaceTired },
  { keyword: "화장실",   Icon: FaRestroom },
  { keyword: "드레싱",   Icon: BsBandaidFill },
  { keyword: "수액",     Icon: FaSyringe },
  { keyword: "체위",     Icon: FaBedPulse },
  { keyword: "호흡",     Icon: FaHeadSideMask },
];

const resolveSymptomIcon = (label: string): SymptomIcon =>
  SYMPTOM_ICONS.find(({ keyword }) => label.includes(keyword))?.Icon ?? HelpCircle;

const GENDER = {
  male:   { label: "남", chip: "bg-blue-100 text-blue-700" },
  female: { label: "여", chip: "bg-pink-100 text-pink-700" },
} as const;

export default function Help() {
  const router = useRouter();
  const patient = usePatientStore((state) => state.patient);

  useEffect(() => {
    if (!patient) router.replace("/patient/verify");
  }, [patient, router]);

  const {
    patientId = 0,
    patientName = "",
    roomName = "",
    gender = "",
    diseaseName = "",
    surgeryName = "",
    chiefComplaint = "",
    assignedNurseName = "",
  } = patient ?? {};
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

      const sentAt = new Date().toTimeString().slice(0, 5);
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
            <span className="text-[22px] font-bold tracking-tight text-patient-ink">
              {patientName}
            </span>
            {gender ? (
              <span
                className={`rounded-full px-2 py-[2px] text-[16px] font-semibold ${GENDER[gender as keyof typeof GENDER]?.chip}`}
              >
                {GENDER[gender as keyof typeof GENDER]?.label}
              </span>
            ) : null}
            {roomName ? (
              <span className="rounded-full bg-patient-slate-surface px-2 py-[2px] text-[16px] font-semibold text-patient-slate">
                {roomName}
              </span>
            ) : null}
          </div>
          <div className="flex items-center gap-2">
            <span className="text-lg font-semibold tracking-tight text-patient-muted">
              담당 간호사
            </span>
            <span className="text-lg font-semibold tracking-tight text-patient-ink">
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
          <span className="text-lg font-semibold tracking-tight text-patient-muted">
            병명
          </span>
          <span className="text-[18px] font-semibold tracking-tight text-patient-ink">
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
              <span className="text-lg font-semibold tracking-tight text-patient-muted">
                주 증상
              </span>
              <span className="text-[18px] font-semibold tracking-tight text-patient-ink">
                {chiefComplaint}
              </span>
            </div>
            <div className="h-px bg-patient-hairline" />
            <div className="flex items-center gap-2 py-1">
              <span className="text-lg font-semibold tracking-tight text-patient-muted">
                수술명
              </span>
              <span className="text-[18px] font-semibold tracking-tight text-patient-ink">
                {surgeryName}
              </span>
            </div>
          </>
        ) : null}
      </div>

      <div className="flex mt-3 border-b border-patient-hairline">
        {(
          [
            { key: "form", label: "요청하기" },
            { key: "faq", label: "맞춤형 FAQ" },
          ] as const
        ).map((tab) => {
          const isActive = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              className={`relative flex flex-1 items-center justify-center pb-3 text-xl font-bold tracking-tight transition-colors duration-200 ${
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
        <div className="flex flex-1 min-h-0 flex-col gap-[15px]">
          <div className="flex flex-1 min-h-0 flex-col gap-[15px] overflow-y-auto [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
          <div className="grid grid-cols-2 gap-5 mx-2 mt-2">
            {buttons.map((button) => {
              const isSelected = selectedButtonId === button.buttonId;
              const Icon = resolveSymptomIcon(button.label);
              return (
                <button
                  key={button.buttonId}
                  type="button"
                  onClick={() => toggleSymptom(button.buttonId)}
                  className={`flex h-[70px] items-center gap-3 rounded-xl px-3.5 py-4 text-left transition-all ${
                    isSelected
                      ? "border border-transparent bg-[#d4e0f7] shadow-[0_4px_6px_rgba(0,0,0,0.13)]"
                      : "border border-transparent bg-white shadow-[0_2px_8px_rgba(0,0,0,0.13)]"
                  }`}
                >
                  <Icon
                    className={`size-7 shrink-0 ${
                      isSelected ? "text-patient-slate" : "text-patient-primary"
                    }`}
                    strokeWidth={2}
                  />
                  <div className="flex min-w-0 flex-1 flex-col justify-center gap-1">
                    <span
                      className={`truncate text-lg font-bold tracking-tight ${
                        isSelected ? "text-patient-slate" : "text-patient-ink"
                      }`}
                    >
                      {button.label}
                    </span>
                    <span
                      className={`truncate text-[16px] font-SemiBold tracking-tight ${
                        isSelected
                          ? "text-patient-slate opacity-85"
                          : "text-patient-muted"
                      }`}
                    >
                      {button.description}
                    </span>
                  </div>
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
          </div>

          <button
            type="button"
            onClick={handleSubmit}
            disabled={!hasRequest || submitting}
            className="shrink-0 h-14 w-full rounded-[14px] bg-patient-primary text-[20px] font-bold tracking-tight text-white transition-colors hover:bg-[#0F1F7A] disabled:cursor-default disabled:bg-[#C8CBD4]"
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
                    <span className="flex flex-wrap items-center gap-2 flex-1">
                      {faq.intentLabel ? (
                        <span
                          className={`shrink-0 rounded-full px-2.5 py-0.5 text-[14px] font-bold ${INTENT_LABEL_COLORS[faq.intentLabel] ?? "bg-gray-100 text-gray-600"}`}
                        >
                          {faq.intentLabel}
                        </span>
                      ) : null}
                      <span className="text-[16px] font-bold tracking-tight text-patient-ink">
                        {faq.question}
                      </span>
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
