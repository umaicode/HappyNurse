/**
 * AI 서버 (FastAPI) axios 인스턴스.
 *
 * - 백엔드 client (lib/client.ts) 와 분리된 이유:
 *   1) baseURL 다름 — 백엔드 `/dev/api`, AI `/dev/ai`
 *   2) 응답 wrapper 다름 — 백엔드 `{success, data, ...}` 평탄화 / AI 는 raw JSON 그대로
 *   3) 인증 방식 — AI 는 ACCESS_TOKEN 쿠키로 통일 (백엔드와 동일). Bearer 헤더 주입 안 함.
 *
 * - 인증 정책: `withCredentials: true` 만으로 ACCESS_TOKEN 쿠키 자동 전송.
 *   Bearer 헤더 박지 않는 이유 — dev login 후 일반 login 으로 전환해도 localStorage 의 옛 dev token
 *   이 남아 만료된 토큰이 박혀 401 떨어지는 사례 발생. 쿠키만 의존하면 로그인 방식 무관하게
 *   백엔드가 발급한 최신 ACCESS_TOKEN 으로 일관 동작. AI 의 `get_current_user` 가 헤더 우선
 *   → 쿠키 fallback 이라 헤더 안 박는 게 위험 적음.
 *
 * - 401 인터셉터: 백엔드 client 의 performRefresh 재사용 → 두 client 가 동일 refresh promise 공유.
 *   refresh 성공 시 원 요청 재시도. 실패 시 그대로 reject (글로벌 redirect 는 백엔드 client 가 담당).
 *
 * - localhost 에선 `/ai-proxy/*` (next.config.ts rewrite) 로 same-origin 우회.
 *   배포(dev/prod) 에선 NEXT_PUBLIC_AI_BASE_URL 절대 URL 직접 호출.
 */
import axios, { AxiosError, AxiosRequestConfig } from 'axios'
import { performRefresh } from './client'

const resolveAiBaseUrl = (): string => {
  if (
    typeof window !== 'undefined' &&
    window.location.hostname === 'localhost'
  ) {
    return '/ai-proxy'
  }
  return process.env.NEXT_PUBLIC_AI_BASE_URL ?? ''
}

export const aiClient = axios.create({
  baseURL: resolveAiBaseUrl(),
  withCredentials: true,
})

type RetryConfig = AxiosRequestConfig & { _retry?: boolean }

aiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryConfig | undefined
    const status = error.response?.status

    if (status !== 401 || !originalRequest || originalRequest._retry) {
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      await performRefresh()
      return aiClient(originalRequest)
    } catch (refreshError) {
      return Promise.reject(refreshError)
    }
  },
)
