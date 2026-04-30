/**
 * Axios 인스턴스.
 *
 * - withCredentials 기본값 true → 백엔드 ACCESS_TOKEN/REFRESH_TOKEN 쿠키 자동 전송
 * - 'dev' 환경 한정 (NEXT_PUBLIC_APP_ENV === 'dev'): localStorage 의 dev 토큰을 Authorization 헤더로 주입.
 *   단, 환자 라우트(/patient/*) 에서는 주입하지 않음 — 환자 인증은 쿠키 기반이고 간호사 dev 토큰이
 *   섞이면 백엔드에서 컨텍스트가 꼬일 수 있음.
 * - 401 인터셉터: /auth/refresh 시도 → 성공 시 원 요청 재시도, 실패 시 /login 으로 이동
 *   환자 라우트(/patient/*) 는 글로벌 핸들러에서 빠짐 — 컴포넌트가 자체 처리
 */
import axios, { AxiosError, AxiosRequestConfig } from 'axios'

// 로컬(localhost) 환경에서는 same-origin 프록시(/api-proxy)로 보내고,
// 배포 환경(dev/prod)에서는 절대 URL 로 직접 호출한다.
// 로컬은 백엔드와 도메인이 달라 SameSite=Lax 쿠키가 차단되므로 Next.js rewrite 를 거쳐 same-origin 으로 만든다.
const resolveBaseUrl = (): string => {
  if (
    typeof window !== 'undefined' &&
    window.location.hostname === 'localhost'
  ) {
    return '/api-proxy'
  }
  return process.env.NEXT_PUBLIC_API_BASE_URL ?? ''
}

export const client = axios.create({
  baseURL: resolveBaseUrl(),
  withCredentials: true,
})

const DEV_ACCESS_TOKEN_KEY = 'devAccessToken'
const DEV_REFRESH_TOKEN_KEY = 'devRefreshToken'

export const devTokenStorage = {
  getAccessToken: () =>
    typeof window !== 'undefined'
      ? window.localStorage.getItem(DEV_ACCESS_TOKEN_KEY)
      : null,
  setTokens: (accessToken: string, refreshToken: string) => {
    if (typeof window === 'undefined') return
    window.localStorage.setItem(DEV_ACCESS_TOKEN_KEY, accessToken)
    window.localStorage.setItem(DEV_REFRESH_TOKEN_KEY, refreshToken)
  },
  clear: () => {
    if (typeof window === 'undefined') return
    window.localStorage.removeItem(DEV_ACCESS_TOKEN_KEY)
    window.localStorage.removeItem(DEV_REFRESH_TOKEN_KEY)
  },
}

const isPatientRoute = () =>
  typeof window !== 'undefined' &&
  window.location.pathname.startsWith('/patient')

// DEV 한정: localStorage 토큰을 Authorization 헤더로 주입. 환자 라우트는 제외.
client.interceptors.request.use((config) => {
  if (process.env.NEXT_PUBLIC_APP_ENV === 'dev' && !isPatientRoute()) {
    const accessToken = devTokenStorage.getAccessToken()
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
  }
  return config
})

type RetryConfig = AxiosRequestConfig & { _retry?: boolean }

// 인증을 "얻으려는" 엔드포인트 목록.
// 이 요청들이 401 을 받으면 자격증명 자체의 실패라 refresh 로 살릴 수 없다.
// 인터셉터에서 글로벌 처리하지 말고 호출 컴포넌트의 onError 가 안내하도록 그대로 reject.
const AUTH_ENTRY_PATHS = ['/auth/login', '/auth/dev-login', '/auth/refresh']

const isAuthEntryRequest = (url: string | undefined) =>
  !!url && AUTH_ENTRY_PATHS.some((path) => url.includes(path))

let refreshPromise: Promise<void> | null = null

const performRefresh = () => {
  if (refreshPromise) return refreshPromise
  refreshPromise = client
    .post('/auth/refresh', null, { _retry: true } as RetryConfig)
    .then(() => undefined)
    .finally(() => {
      refreshPromise = null
    })
  return refreshPromise
}

const redirectToLogin = () => {
  if (typeof window !== 'undefined') {
    const basePath = process.env.NEXT_PUBLIC_BASE_PATH ?? ''
    window.location.href = `${basePath}/login`
  }
}

client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryConfig | undefined
    const status = error.response?.status

    if (status !== 401 || !originalRequest || originalRequest._retry) {
      return Promise.reject(error)
    }

    // 환자 라우트는 글로벌 401 핸들러를 거치지 않음 (간호사 /login 으로 보내면 안 됨)
    if (isPatientRoute()) {
      return Promise.reject(error)
    }

    // 로그인/리프레시 자체의 401 은 컴포넌트가 안내. refresh 흐름으로 hard reload 트리거 금지.
    if (isAuthEntryRequest(originalRequest.url)) {
      return Promise.reject(error)
    }

    originalRequest._retry = true

    try {
      await performRefresh()
      return client(originalRequest)
    } catch (refreshError) {
      devTokenStorage.clear()
      redirectToLogin()
      return Promise.reject(refreshError)
    }
  },
)
