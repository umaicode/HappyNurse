/**
 * Axios 인스턴스.
 *
 * - withCredentials 기본값 true → 백엔드 ACCESS_TOKEN/REFRESH_TOKEN 쿠키 자동 전송
 * - 'dev' 환경 한정 (NEXT_PUBLIC_APP_ENV === 'dev'): localStorage 의 dev 토큰을 Authorization 헤더로 주입
 * - 401 인터셉터: /auth/refresh 시도 → 성공 시 원 요청 재시도, 실패 시 /login 으로 이동
 *   환자 라우트(/patient/*) 는 글로벌 핸들러에서 빠짐 — 컴포넌트가 자체 처리
 */
import axios, { AxiosError, AxiosRequestConfig } from 'axios'

export const client = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
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

// DEV 한정: localStorage 토큰을 Authorization 헤더로 주입
client.interceptors.request.use((config) => {
  if (process.env.NEXT_PUBLIC_APP_ENV === 'dev') {
    const accessToken = devTokenStorage.getAccessToken()
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
  }
  return config
})

type RetryConfig = AxiosRequestConfig & { _retry?: boolean }

let refreshPromise: Promise<void> | null = null

const isPatientRoute = () =>
  typeof window !== 'undefined' &&
  window.location.pathname.startsWith('/patient')

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
