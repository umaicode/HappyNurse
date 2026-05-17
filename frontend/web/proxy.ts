/**
 * [Next.js 16] middleware.ts 대체 파일.
 * Node.js 런타임 동작.
 * 비로그인(ACCESS_TOKEN 쿠키 없음) → /login 리다이렉트.
 *
 * 루트 `/` 진입 분기는 proxy 가 아닌 `app/page.tsx` 서버 컴포넌트가 처리한다 —
 * Next.js 16 의 proxy matcher 는 bare `'/'` 패턴을 안정적으로 매칭하지 않아서
 * (path-to-regexp 의 root 처리 quirk) 정규식 회피 패치보다 server component 의
 * `cookies()` + `redirect()` 가 표준 패턴.
 *
 * [Next.js 16 변경]
 * 파일명: middleware.ts  →  proxy.ts
 * 함수명: middleware()   →  proxy()
 *
 * basePath 주의: NextResponse.redirect 에 new URL('/login', request.url) 형태를 사용하면
 * basePath 가 사라진다. request.nextUrl.clone() + pathname 변경 패턴을 사용해야
 * 배포 환경(예: NEXT_PUBLIC_BASE_PATH=/dev)에서 basePath 가 보존된다.
 */
import { NextRequest, NextResponse } from 'next/server'

export function proxy(request: NextRequest) {
  const accessToken = request.cookies.get('ACCESS_TOKEN')
  if (!accessToken) {
    const loginUrl = request.nextUrl.clone()
    loginUrl.pathname = '/login'
    loginUrl.search = ''
    return NextResponse.redirect(loginUrl)
  }
  return NextResponse.next()
}

export const config = {
  matcher: ['/dashboard/:path*', '/handover/:path*'],
}
