"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { motion } from "motion/react";
import {
  ArrowRight,
  Lock,
  User,
  ChevronLeft,
  Building2,
  LayoutGrid,
} from "lucide-react";
import { Text } from "@/components/ui/text";
import { Heading } from "@/components/ui/heading";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function LoginForm() {
  const router = useRouter();
  const [step, setStep] = useState(1);
  const [hospital, setHospital] = useState("");
  const [ward, setWard] = useState("");
  const [username, setUsername] = useState("김영희");

  // Mock 로그인 — 입력 무시하고 항상 "김영희"로 진입
  const handleLogin = () => {
    localStorage.setItem("currentUser", "김영희");
    router.push("/dashboard");
  };

  return (
    <div className="fixed inset-0 flex w-full bg-white overflow-hidden font-sans">
      {/* Layer 1: Base */}
      <div
        className="absolute inset-0 z-0"
        style={{
          background: `linear-gradient(135deg,
            var(--color-brand-surface) 0%,
            var(--color-brand-surface) 12%,
            #FFFFFF 35%,
            #FFFFFF 65%,
            var(--color-brand-surface) 88%,
            var(--color-brand-surface) 100%
          )`,
        }}
      />

      {/* Layer 2: Center white highlight */}
      <div
        className="absolute inset-0 z-0"
        style={{
          background: `radial-gradient(ellipse 80% 65% at 50% 45%, #FFFFFF, transparent 72%)`,
          opacity: 0.85,
        }}
      />

      {/* Layer 3: Top-right brand-primary spotlight */}
      <div
        className="absolute inset-0 z-0"
        style={{
          background: `radial-gradient(ellipse 60% 50% at 92% 8%, var(--color-brand-primary), transparent 60%)`,
          opacity: 0.25,
        }}
      />

      {/* Layer 4: Bottom-left sub-primary spotlight */}
      <div
        className="absolute inset-0 z-0"
        style={{
          background: `radial-gradient(ellipse 70% 55% at 8% 95%, var(--color-sub-primary), transparent 60%)`,
          opacity: 0.2,
        }}
      />

      {/* Layer 5: Saturated blobs */}
      <div className="absolute top-[-20%] left-[-12%] w-[55%] h-[55%] bg-[var(--color-brand-primary)]/22 rounded-full blur-[130px] z-1" />
      <div className="absolute bottom-[-15%] right-[8%] w-[50%] h-[50%] bg-[var(--color-brand-primary)]/18 rounded-full blur-[150px] z-1" />
      <div className="absolute top-[30%] right-[-15%] w-[42%] h-[42%] bg-[var(--color-sub-primary)]/16 rounded-full blur-[120px] z-1" />
      <div className="absolute bottom-[25%] left-[10%] w-[28%] h-[28%] bg-[var(--color-brand-primary)]/12 rounded-full blur-[100px] z-1" />

      {/* Layer 6: Soft white veil */}
      <div className="absolute inset-0 z-2 bg-gradient-to-b from-white/15 via-transparent to-white/10" />

      {/* 개발용 환자 로그인 버튼 */}
      <button 
        onClick={() => router.push("/patient")}
        className="absolute top-4 left-4 z-50 rounded-2xl border border-action-blue-hover bg-white px-3 py-1.5 text-[26px] font-bold text-black hover:bg-[#a0afec] transition-colors"
      >
        🛠️ 환자 로그인
      </button>

      {/* Content Layer */}
      <div className="relative z-10 flex w-full h-full">
        {/* Left Panel: Brand Visuals (50%) */}
        <div className="hidden md:flex w-1/2 relative flex-col justify-center p-16 lg:p-24 overflow-hidden">
          <div className="relative z-10 flex flex-col items-start text-left">
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.2 }}
              className="flex flex-col items-start"
            >
              <img
                src="/images/logo_ic.png"
                alt=""
                className="h-20 w-20 object-contain mb-6"
              />
              <Heading
                level="h1"
                className="text-[var(--color-sub-primary)] leading-[1.2] mb-6 text-5xl"
              >
                간호 업무의
                <br />
                <span className="text-[var(--color-brand-primary)]">
                  새로운 기준
                </span>
              </Heading>

              <Text
                size="lg"
                className="text-content-secondary leading-relaxed font-medium opacity-80 max-w-md"
              >
                해피너스는 의료진의 더 편리하고 정확한 업무 환경을 위해 최신
                기술과 사용자 중심의 설계를 결합했습니다.
              </Text>
            </motion.div>
          </div>
        </div>

        {/* Right Panel: Login Form (50%) */}
        <div className="w-full md:w-1/2 flex flex-col items-center justify-center p-8 bg-white relative z-30 md:rounded-l-[60px] shadow-[-20px_0_50px_rgba(0,0,0,0.05)] border-l border-white/20">
          <div className="absolute top-10 right-12 hidden md:block">
            <img
              src="/images/logo_2.png"
              alt="해피너스"
              className="h-4 object-contain"
            />
          </div>

          <div className="w-full max-w-[420px] px-4">
            {/* 상단 영역: 뒤로가기 버튼 (step 1에서도 높이 유지) */}
            <div className="mb-6">
              <button
                onClick={() => setStep(1)}
                aria-hidden={step !== 2}
                tabIndex={step === 2 ? 0 : -1}
                className={`flex items-center gap-1.5 text-sm font-bold text-content-muted hover:text-[var(--color-brand-primary)] transition-opacity group ${
                  step === 2 ? "opacity-100" : "opacity-0 pointer-events-none"
                }`}
              >
                <ChevronLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
                병원/병동 다시 선택
              </button>
            </div>

            {/* 하단 영역: 폼 내용 */}
            {step === 1 ? (
              <div className="space-y-8">
                <div>
                  <Heading
                    level="h2"
                    className="text-3xl font-bold text-[var(--color-sub-primary)] mb-3"
                  >
                    접속 정보 선택
                  </Heading>
                  <p className="text-content-tertiary font-medium">
                    소속된 병원과 현재 근무 병동을 선택해 주세요.
                  </p>
                </div>

                <div className="space-y-6">
                  <div className="space-y-2">
                    <Label className="text-sm font-bold text-content-secondary ml-1">
                      소속 병원
                    </Label>
                    <div className="relative">
                      <Building2 className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-content-muted" />
                      <Input
                        placeholder="병원을 검색하세요"
                        value={hospital}
                        onChange={(e) => setHospital(e.target.value)}
                        className="pl-12 h-14 bg-slate-50/50 border-slate-200 focus-visible:border-[var(--color-brand-primary)] focus-visible:ring-[var(--color-brand-primary)]/5 rounded-2xl text-base font-semibold transition-all"
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label className="text-sm font-bold text-content-secondary ml-1">
                      근무 병동
                    </Label>
                    <div className="relative">
                      <LayoutGrid className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-content-muted z-10 pointer-events-none" />
                      <Select value={ward} onValueChange={setWard}>
                        <SelectTrigger className="pl-12 h-14 bg-slate-50/50 border-slate-200 focus:border-[var(--color-brand-primary)] focus:ring-4 focus:ring-[var(--color-brand-primary)]/5 rounded-2xl text-base font-bold text-content-primary transition-all">
                          <SelectValue placeholder="접속할 병동을 선택하세요" />
                        </SelectTrigger>
                        <SelectContent className="z-[100] rounded-xl border-[var(--color-border-base)] shadow-xl">
                          <SelectItem value="71">
                            🛠️ 71병동 (일반내과)
                          </SelectItem>
                          <SelectItem value="72">
                            🛠️ 72병동 (소화기내과)
                          </SelectItem>
                          <SelectItem value="icu">🛠️ ICU (중환자실)</SelectItem>
                          <SelectItem value="er">🛠️ ER (응급실)</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </div>

                <Button
                  onClick={() => setStep(2)}
                  className="w-full h-15 bg-[var(--color-brand-primary)] hover:bg-[var(--color-brand-hover)] !text-white font-bold text-lg rounded-2xl shadow-xl shadow-[var(--color-brand-primary)]/20 transition-all flex items-center justify-center gap-2 group"
                >
                  다음 단계
                  <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </Button>
              </div>
            ) : (
              <div className="space-y-8">
                <div>
                  <Heading
                    level="h2"
                    className="text-3xl font-bold text-[var(--color-sub-primary)] mb-3"
                  >
                    환영합니다!
                  </Heading>
                  <p className="text-content-tertiary font-medium">
                    아이디와 비밀번호를 입력해 주세요.
                  </p>
                </div>

                <div className="space-y-6">
                  <div className="space-y-2">
                    <Label className="text-sm font-bold text-content-secondary ml-1">
                      사원 아이디
                    </Label>
                    <div className="relative">
                      <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-content-muted" />
                      <Input
                        placeholder="🛠️ 아이디를 입력하세요"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="pl-12 h-14 bg-slate-50/50 border-slate-200 focus-visible:border-[var(--color-brand-primary)] focus-visible:ring-[var(--color-brand-primary)]/5 rounded-2xl text-base font-semibold transition-all"
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label className="text-sm font-bold text-content-secondary ml-1">
                      비밀번호
                    </Label>
                    <div className="relative">
                      <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-content-muted" />
                      <Input
                        type="password"
                        placeholder="🛠️ 비밀번호를 입력하세요"
                        className="pl-12 h-14 bg-slate-50/50 border-slate-200 focus-visible:border-[var(--color-brand-primary)] focus-visible:ring-[var(--color-brand-primary)]/5 rounded-2xl text-base font-semibold transition-all"
                      />
                    </div>
                  </div>
                </div>

                <Button
                  onClick={handleLogin}
                  className="w-full h-15 bg-[var(--color-brand-primary)] hover:bg-[var(--color-brand-hover)] !text-white font-bold text-lg rounded-2xl shadow-xl shadow-[var(--color-brand-primary)]/20 transition-all flex items-center justify-center gap-2"
                >
                  로그인
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
