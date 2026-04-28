'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { Modal } from '@/components/common/Modal'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { devLogin, devSignup } from '../api'
import type { RoleCode } from '../types'
import { useAuthStore } from '../stores/auth'
import { devTokenStorage } from '@/lib/client'

// TODO: organizationId · wardId 조회 API 연동 시 사용자 선택 기반으로 교체
const FALLBACK_ORGANIZATION_ID = 1
const FALLBACK_WARD_ID = 1
const FALLBACK_ROLE_CODE: RoleCode = 'nurse'

interface Props {
  isOpen: boolean
  onClose: () => void
}

export function DevSignupModal({ isOpen, onClose }: Props) {
  const router = useRouter()
  const setUser = useAuthStore((state) => state.setUser)

  const [employeeNumber, setEmployeeNumber] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const signupMutation = useMutation({
    mutationFn: devSignup,
    onSuccess: () => {
      autoLoginMutation.mutate({ employeeNumber })
    },
    onError: () => {
      setErrorMessage('회원가입에 실패했습니다. 입력값을 확인해주세요.')
    },
  })

  const autoLoginMutation = useMutation({
    mutationFn: devLogin,
    onSuccess: (data) => {
      devTokenStorage.setTokens(data.accessToken, data.refreshToken)
      setUser({
        practitionerId: data.practitionerId,
        name: data.name,
        employeeNumber: data.employeeNumber,
        roleCode: data.roleCode,
        organizationId: data.organizationId,
        wardId: data.wardId,
      })
      onClose()
      router.push('/dashboard')
    },
    onError: () => {
      setErrorMessage('자동 로그인에 실패했습니다.')
    },
  })

  const isSubmitting = signupMutation.isPending || autoLoginMutation.isPending
  const isFormValid =
    employeeNumber.trim() &&
    password.trim() &&
    name.trim() &&
    phone.trim()

  const handleSubmit = () => {
    if (!isFormValid || isSubmitting) return
    setErrorMessage('')
    signupMutation.mutate({
      employeeNumber: employeeNumber.trim(),
      password,
      name: name.trim(),
      phone: phone.trim(),
      organizationId: FALLBACK_ORGANIZATION_ID,
      wardId: FALLBACK_WARD_ID,
      roleCode: FALLBACK_ROLE_CODE,
    })
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="flex w-[360px] flex-col gap-4">
        <div>
          <h2 className="text-xl font-bold text-[var(--color-sub-primary)]">
            테스트 회원가입
          </h2>
          <p className="mt-1 text-sm text-content-tertiary">
            DEV 환경 전용. 가입 성공 시 자동으로 로그인됩니다.
          </p>
        </div>

        <div className="space-y-3">
          <div className="space-y-1.5">
            <Label className="text-sm font-bold">사원 번호</Label>
            <Input
              value={employeeNumber}
              onChange={(event) => setEmployeeNumber(event.target.value)}
              placeholder="DEV001"
            />
          </div>

          <div className="space-y-1.5">
            <Label className="text-sm font-bold">비밀번호</Label>
            <Input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="비밀번호 (8자 이상)"
            />
          </div>

          <div className="space-y-1.5">
            <Label className="text-sm font-bold">이름</Label>
            <Input
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="홍길동"
            />
          </div>

          <div className="space-y-1.5">
            <Label className="text-sm font-bold">연락처</Label>
            <Input
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              placeholder="010-1111-2222"
            />
          </div>
        </div>

        {errorMessage ? (
          <p className="text-sm font-semibold text-red-500">{errorMessage}</p>
        ) : null}

        <div className="flex gap-2">
          <Button
            type="button"
            variant="outline"
            className="flex-1"
            onClick={onClose}
            disabled={isSubmitting}
          >
            취소
          </Button>
          <Button
            type="button"
            className="flex-1 bg-[var(--color-brand-primary)] hover:bg-[var(--color-brand-hover)] !text-white"
            onClick={handleSubmit}
            disabled={!isFormValid || isSubmitting}
          >
            {isSubmitting ? '처리 중...' : '가입 후 로그인'}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
