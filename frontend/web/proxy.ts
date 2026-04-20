/**
 * [Next.js 16] middleware.ts 대체 파일.
 * Node.js 런타임 동작.
 * 비로그인 → /login 리다이렉트
 * RBAC 역할별 접근 제어.
 *
 * [Next.js 16 변경]
 * 파일명: middleware.ts  →  proxy.ts
 * 함수명: middleware()   →  proxy()
 */
import { NextRequest, NextResponse } from 'next/server'

export function proxy(req: NextRequest) {
  const token = req.cookies.get('token')
  if (!token)
    return NextResponse.redirect(new URL('/login', req.url))

  return NextResponse.next()
}

export const config = { matcher: ['/(web)/:path*'] }
