/**
 * 인증 API 함수.
 * login() · logout() · refreshToken()
 */
import { client } from '@/lib/client'
import type { LoginRequest, AuthResponse } from '../types'

export const login = (body: LoginRequest) =>
  client.post<AuthResponse>('/auth/login', body)

export const logout = () =>
  client.post('/auth/logout')

export const refreshToken = () =>
  client.post<{ accessToken: string }>('/auth/refresh')
