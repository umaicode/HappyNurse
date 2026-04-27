"use client";

import { useRouter } from "next/navigation";
import { motion } from "motion/react";
import { ArrowLeft, Mail, User } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Heading } from "@/components/ui/heading";

export function FindPasswordForm() {
  const router = useRouter();

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-[#f8fafc] font-sans">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{
          duration: 0.5,
          ease: [0.16, 1, 0.3, 1],
        }}
        className="w-full max-w-[440px] p-8 md:p-10 bg-white rounded-[32px] shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-slate-100 mx-4"
      >
        <button
          onClick={() => router.push("/login")}
          className="flex items-center gap-2 text-content-tertiary hover:text-[var(--color-brand-primary)] transition-colors mb-6 group"
        >
          <ArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
          <span className="text-sm font-bold">로그인으로 돌아가기</span>
        </button>

        <div className="mb-10 text-center md:text-left">
          <Heading
            level="h2"
            className="text-2xl font-bold text-[var(--color-sub-primary)] mb-2"
          >
            비밀번호 찾기
          </Heading>
          <p className="text-content-tertiary text-body-base font-medium">
            본인 확인을 위해 아래 정보를 입력해 주세요.
          </p>
        </div>

        <div className="space-y-6">
          <div className="space-y-2 group">
            <Label className="text-[13px] font-bold text-content-secondary group-focus-within:text-[var(--color-brand-primary)] transition-colors tracking-wide ml-0.5">
              사원번호
            </Label>
            <div className="relative">
              <User className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted group-focus-within:text-[var(--color-brand-primary)] transition-colors z-10" />
              <Input
                type="text"
                placeholder="사원번호를 입력하세요"
                className="pl-11 h-14 bg-white border-border-base focus-visible:border-[var(--color-brand-primary)] focus-visible:ring-[var(--color-brand-primary)]/10 transition-all text-body-base font-medium rounded-xl shadow-sm"
              />
            </div>
          </div>

          <div className="space-y-2 group">
            <Label className="text-[13px] font-bold text-content-secondary group-focus-within:text-[var(--color-brand-primary)] transition-colors tracking-wide ml-0.5">
              이메일 주소
            </Label>
            <div className="relative">
              <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-content-muted group-focus-within:text-[var(--color-brand-primary)] transition-colors z-10" />
              <Input
                type="email"
                placeholder="example@hospital.com"
                className="pl-11 h-14 bg-white border-border-base focus-visible:border-[var(--color-brand-primary)] focus-visible:ring-[var(--color-brand-primary)]/10 transition-all text-body-base font-medium rounded-xl shadow-sm"
              />
            </div>
          </div>

          <Button
            size="lg"
            className="w-full h-14 mt-6 bg-[var(--color-brand-primary)] hover:bg-[var(--color-brand-hover)] !text-white font-bold text-body-base rounded-xl transition-all duration-300 flex items-center justify-center gap-3 active:scale-[0.98] shadow-lg shadow-[var(--color-brand-primary)]/20 hover:shadow-[var(--color-brand-primary)]/30"
          >
            <span className="!text-white text-body-base">
              임시 비밀번호 발송
            </span>
          </Button>
        </div>
      </motion.div>
    </div>
  );
}
