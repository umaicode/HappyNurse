'use client'

import { useEffect, useState } from 'react'

import { useRouter } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { motion } from 'motion/react'
import {
  ArrowRight,
  Lock,
  User,
  ChevronLeft,
  Building2,
  LayoutGrid,
} from 'lucide-react'
import { Text } from '@/components/ui/text'
import { Heading } from '@/components/ui/heading'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { devLogin, login } from '../api'
import { useOrganizations, useWards } from '../hooks/useOrganizations'
import { useAuthStore } from '../stores/auth'
import { devTokenStorage } from '@/lib/client'
import { DevSignupModal } from './DevSignupModal'

const isDevelopment = process.env.NEXT_PUBLIC_APP_ENV === 'dev'

// 마지막으로 로그인 성공한 세션의 병원·병동을 기억해 다음 로그인 때 step 1 을 스킵한다.
// "병원/병동 다시 선택" → 새 값으로 로그인 성공 시점에만 덮어쓰므로, 단순히 선택만 바꾼 경우엔 보존된다.
const LOGIN_CONTEXT_KEY = 'lastLoginContext'

interface LoginContext {
  organizationId: string
  wardId: string
}

const loginContextStorage = {
  load(): LoginContext | null {
    if (typeof window === 'undefined') return null
    try {
      const raw = window.localStorage.getItem(LOGIN_CONTEXT_KEY)
      if (!raw) return null
      const parsed = JSON.parse(raw) as Partial<LoginContext>
      if (
        typeof parsed.organizationId === 'string' &&
        parsed.organizationId.length > 0 &&
        typeof parsed.wardId === 'string' &&
        parsed.wardId.length > 0
      ) {
        return {
          organizationId: parsed.organizationId,
          wardId: parsed.wardId,
        }
      }
      return null
    } catch {
      return null
    }
  },
  save(context: LoginContext) {
    if (typeof window === 'undefined') return
    if (!context.organizationId || !context.wardId) return
    window.localStorage.setItem(LOGIN_CONTEXT_KEY, JSON.stringify(context))
  },
}

export function LoginForm() {
  const router = useRouter()
  const setUser = useAuthStore((state) => state.setUser)

  const [step, setStep] = useState(1)
  const [organizationId, setOrganizationId] = useState('')
  const [ward, setWard] = useState('')
  const [employeeNumber, setEmployeeNumber] = useState('')
  const [password, setPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSignupOpen, setIsSignupOpen] = useState(false)

  const organizationsQuery = useOrganizations()
  const wardsQuery = useWards(organizationId ? Number(organizationId) : null)

  // 마운트 시 마지막 로그인 컨텍스트가 있으면 step 1 을 건너뛰고 사번/비밀번호 입력으로 바로 진입.
  // SSR 초기 렌더와 클라이언트 첫 렌더의 HTML 을 일치시키기 위해 lazy init 이 아닌 useEffect 사용.
  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    const saved = loginContextStorage.load()
    if (saved) {
      setOrganizationId(saved.organizationId)
      setWard(saved.wardId)
      setStep(2)
    }
  }, [])
  /* eslint-enable react-hooks/set-state-in-effect */

  const handleOrganizationChange = (value: string) => {
    setOrganizationId(value)
    setWard('')
  }

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      loginContextStorage.save({ organizationId, wardId: ward })
      setUser(data)
      router.push('/dashboard')
    },
    onError: () => {
      setErrorMessage('로그인에 실패했습니다. 사번과 비밀번호를 확인해주세요.')
    },
  })

  const devLoginMutation = useMutation({
    mutationFn: devLogin,
    onSuccess: (data) => {
      devTokenStorage.setTokens(data.accessToken, data.refreshToken)
      // step 1 을 거치며 병원·병동을 채운 상태에서 dev 로그인했을 때만 컨텍스트 저장.
      // 빈 상태로 dev 로그인하면 무의미한 컨텍스트라 저장 안 함 (load 단계에서 어차피 무시됨).
      loginContextStorage.save({ organizationId, wardId: ward })
      setUser({
        practitionerId: data.practitionerId,
        name: data.name,
        employeeNumber: data.employeeNumber,
        roleCode: data.roleCode,
        organizationId: data.organizationId,
        wardId: data.wardId,
      })
      router.push('/dashboard')
    },
    onError: () => {
      setErrorMessage('DEV 로그인에 실패했습니다.')
    },
  })

  const handleLogin = () => {
    if (!employeeNumber.trim() || !password.trim() || loginMutation.isPending) return
    if (!organizationId || !ward) {
      setErrorMessage('병원과 병동을 다시 선택해주세요.')
      setStep(1)
      return
    }
    setErrorMessage('')
    loginMutation.mutate({
      organizationId: Number(organizationId),
      wardId: Number(ward),
      employeeNumber: employeeNumber.trim(),
      password,
    })
  }

  const handleDevLogin = () => {
    if (devLoginMutation.isPending) return
    let targetEmployeeNumber = employeeNumber.trim()
    if (!targetEmployeeNumber) {
      const input = window.prompt('사번을 입력하세요')
      if (!input?.trim()) return
      targetEmployeeNumber = input.trim()
    }
    setErrorMessage('')
    devLoginMutation.mutate({ employeeNumber: targetEmployeeNumber })
  }

  return (
    <div className="fixed inset-0 flex w-full bg-white overflow-hidden">
      {/* Layer 1: Base */}
      <div className="absolute inset-0 z-0 bg-[linear-gradient(135deg,var(--color-brand-surface)_0%,var(--color-brand-surface)_12%,#FFFFFF_35%,#FFFFFF_65%,var(--color-brand-surface)_88%,var(--color-brand-surface)_100%)]" />

      {/* Layer 2: Center white highlight */}
      <div className="absolute inset-0 z-0 bg-[radial-gradient(ellipse_80%_65%_at_50%_45%,#FFFFFF,transparent_72%)] opacity-85" />

      {/* Layer 3: Top-right brand-primary spotlight */}
      <div className="absolute inset-0 z-0 bg-[radial-gradient(ellipse_60%_50%_at_92%_8%,var(--color-brand-primary),transparent_60%)] opacity-25" />

      {/* Layer 4: Bottom-left sub-primary spotlight */}
      <div className="absolute inset-0 z-0 bg-[radial-gradient(ellipse_70%_55%_at_8%_95%,var(--color-sub-primary),transparent_60%)] opacity-20" />

      {/* Layer 5: Saturated blobs */}
      <div className="absolute top-[-20%] left-[-12%] w-[55%] h-[55%] bg-brand-primary/22 rounded-full blur-[130px] z-1" />
      <div className="absolute bottom-[-15%] right-[8%] w-[50%] h-[50%] bg-brand-primary/18 rounded-full blur-[150px] z-1" />
      <div className="absolute top-[30%] right-[-15%] w-[42%] h-[42%] bg-sub-primary/16 rounded-full blur-[120px] z-1" />
      <div className="absolute bottom-[25%] left-[10%] w-[28%] h-[28%] bg-brand-primary/12 rounded-full blur-[100px] z-1" />

      {/* Layer 6: Soft white veil */}
      <div className="absolute inset-0 z-2 bg-gradient-to-b from-white/15 via-transparent to-white/10" />

      {/* 좌측 상단 dev 툴: 환자 라우트 진입 + DEV 전용 동작 (운영 빌드에서는 DEV 동작은 숨김) */}
      <div className="absolute top-4 left-4 z-50 flex items-center gap-2">
        <button
          onClick={() => router.push('/patient/verify?patientId=3&prefill=1')}
          className="rounded-2xl border border-action-blue-hover bg-[#e2e3e4] px-3 py-1.5 text-[22px] font-bold text-gray-600 hover:bg-[#a0afec] transition-colors"
        >
          환자 로그인
        </button>
        {isDevelopment ? (
          <div className="flex items-center gap-2 rounded-xl border border-slate-300 bg-white/80 px-2 py-1.5 shadow-sm backdrop-blur">
            <span className="text-xs font-bold text-content-muted">
              개발 환경 전용
            </span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDevLogin}
              disabled={devLoginMutation.isPending}
            >
              {devLoginMutation.isPending
                ? 'DEV 로그인 중...'
                : 'DEV 로그인 (사번만)'}
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsSignupOpen(true)}
            >
              테스트 회원가입
            </Button>
          </div>
        ) : null}
      </div>

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
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src="/images/logo_2.png"
                alt="HAPPYNURSE"
                className="object-contain mb-6 h-8 w-auto"
              />
              <Heading
                level="h1"
                className="text-sub-primary leading-[1.2] mb-6 text-5xl"
              >
                간호 업무의
                <br />
                <span className="text-brand-primary">
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
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src="/images/logo_2.png"
              alt="해피너스"
              width={120}
              height={16}
              className="object-contain"
            />
          </div>

          <div className="w-full max-w-[420px] px-4">
            {/* 상단 영역: 뒤로가기 버튼 (step 1에서도 높이 유지) */}
            <div className="mb-6">
              <button
                onClick={() => {
                  setStep(1)
                  setErrorMessage('')
                }}
                aria-hidden={step !== 2}
                tabIndex={step === 2 ? 0 : -1}
                className={`flex items-center gap-1.5 text-sm font-bold text-content-muted hover:text-brand-primary transition-opacity group ${
                  step === 2 ? 'opacity-100' : 'opacity-0 pointer-events-none'
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
                    className="text-3xl font-bold text-sub-primary mb-3"
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
                      <Building2 className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-content-muted z-10 pointer-events-none" />
                      <Select
                        value={organizationId}
                        onValueChange={handleOrganizationChange}
                        disabled={organizationsQuery.isPending}
                      >
                        <SelectTrigger className="pl-12 h-14 bg-slate-50/50 border-slate-200 focus:border-brand-primary focus:ring-4 focus:ring-brand-primary/5 rounded-2xl text-base font-bold text-content-primary transition-all">
                          <SelectValue
                            placeholder={
                              organizationsQuery.isPending
                                ? '병원 정보를 불러오는 중...'
                                : '병원을 선택하세요'
                            }
                          />
                        </SelectTrigger>
                        <SelectContent className="z-[100] rounded-xl border-border-base shadow-xl">
                          {organizationsQuery.data?.map((organization) => (
                            <SelectItem
                              key={organization.organizationId}
                              value={String(organization.organizationId)}
                            >
                              {organization.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label className="text-sm font-bold text-content-secondary ml-1">
                      근무 병동
                    </Label>
                    <div className="relative">
                      <LayoutGrid className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-content-muted z-10 pointer-events-none" />
                      <Select
                        value={ward}
                        onValueChange={setWard}
                        disabled={!organizationId || wardsQuery.isPending}
                      >
                        <SelectTrigger className="pl-12 h-14 bg-slate-50/50 border-slate-200 focus:border-brand-primary focus:ring-4 focus:ring-brand-primary/5 rounded-2xl text-base font-bold text-content-primary transition-all">
                          <SelectValue
                            placeholder={
                              !organizationId
                                ? '병원을 먼저 선택해주세요'
                                : wardsQuery.isPending
                                ? '병동 정보를 불러오는 중...'
                                : '접속할 병동을 선택하세요'
                            }
                          />
                        </SelectTrigger>
                        <SelectContent className="z-[100] rounded-xl border-border-base shadow-xl">
                          {wardsQuery.data?.map((wardOption) => (
                            <SelectItem
                              key={wardOption.wardId}
                              value={String(wardOption.wardId)}
                            >
                              {wardOption.wardName}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </div>

                <Button
                  onClick={() => setStep(2)}
                  disabled={!organizationId || !ward}
                  className="w-full h-15 bg-brand-primary hover:bg-brand-hover text-white! font-bold text-lg rounded-2xl shadow-xl shadow-brand-primary/20 transition-all flex items-center justify-center gap-2 group disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  다음 단계
                  <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </Button>
              </div>
            ) : (
              <form
                onSubmit={(event) => {
                  event.preventDefault()
                  handleLogin()
                }}
                className="space-y-8"
              >
                <div>
                  <Heading
                    level="h2"
                    className="text-3xl font-bold text-sub-primary mb-3"
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
                      사원 번호
                    </Label>
                    <div className="relative">
                      <User className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-content-muted" />
                      <Input
                        name="employeeNumber"
                        autoComplete="username"
                        placeholder="사원번호를 입력하세요"
                        value={employeeNumber}
                        onChange={(event) =>
                          setEmployeeNumber(event.target.value)
                        }
                        className="pl-12 h-14 bg-slate-50/50 border-slate-200 focus-visible:border-brand-primary focus-visible:ring-brand-primary/5 rounded-2xl text-base font-semibold transition-all"
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
                        name="password"
                        autoComplete="current-password"
                        placeholder="비밀번호를 입력하세요"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        className="pl-12 h-14 bg-slate-50/50 border-slate-200 focus-visible:border-brand-primary focus-visible:ring-brand-primary/5 rounded-2xl text-base font-semibold transition-all"
                      />
                    </div>
                  </div>
                </div>

                {errorMessage ? (
                  <p className="text-sm font-semibold text-red-500">
                    {errorMessage}
                  </p>
                ) : null}

                <Button
                  type="submit"
                  disabled={loginMutation.isPending}
                  className="w-full h-15 bg-brand-primary hover:bg-brand-hover text-white! font-bold text-lg rounded-2xl shadow-xl shadow-brand-primary/20 transition-all flex items-center justify-center gap-2"
                >
                  {loginMutation.isPending ? '로그인 중...' : '로그인'}
                </Button>
              </form>
            )}
          </div>
        </div>
      </div>

      {isDevelopment ? (
        <DevSignupModal
          isOpen={isSignupOpen}
          onClose={() => setIsSignupOpen(false)}
        />
      ) : null}
    </div>
  )
}
