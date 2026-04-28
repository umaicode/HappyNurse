'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { getMe } from '@/features/auth/api'
import { useAuthStore } from '@/features/auth/stores/auth'
import { Spinner } from '@/components/common/Spinner'

/**
 * 간호사 웹 라우트 그룹의 인증 게이트.
 *
 * proxy.ts 가 ACCESS_TOKEN 쿠키 존재 여부만 검사하므로, 만료된 쿠키로 진입 시 API 401 가 발생할 수 있다.
 * 이 컴포넌트는 진입 시 /practitioners/me 로 세션 store 를 복원한다.
 * 토큰이 만료된 경우엔 client.ts 의 401 인터셉터가 /auth/refresh → 재시도 흐름을 처리하고,
 * 끝까지 실패하면 /login 으로 이동한다.
 *
 * 로그인 직후 라우트 이동(store.user 가 이미 채워진 상태)에는 호출을 생략한다.
 */
export default function WebLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter()
  const user = useAuthStore((state) => state.user)
  const setUser = useAuthStore((state) => state.setUser)

  const { isError, isPending, isSuccess } = useQuery({
    queryKey: ['auth', 'session'],
    queryFn: async () => {
      const data = await getMe()
      setUser(data)
      return data
    },
    retry: false,
    enabled: !user,
    staleTime: Infinity,
  })

  useEffect(() => {
    if (isError) {
      router.replace('/login')
    }
  }, [isError, router])

  if (!user && (isPending || !isSuccess)) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner />
      </div>
    )
  }

  if (!user) return null

  return <>{children}</>
}
