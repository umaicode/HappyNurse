/**
 * Axios 인스턴스.
 * baseURL · JWT 토큰 인터셉터 · 401 자동 로그아웃.
 */
import axios from 'axios'

export const client = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL,
})

const getToken = () =>
  typeof window !== 'undefined' ? localStorage.getItem('token') : null

// JWT 토큰 인터셉터 — 모든 요청에 Authorization 헤더 자동 주입
client.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 401 자동 로그아웃 — 토큰 만료 시 /login 리다이렉트
client.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)
