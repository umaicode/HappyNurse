/**
 * Next.js 16 설정.
 * Turbopack 기본값 · React Compiler 선택 적용.
 *
 * [Next.js 16 변경]
 * Before: "dev": "next dev --turbopack"
 * After:  "dev": "next dev"  → 플래그 불필요 (Turbopack이 기본값)
 */
import type { NextConfig } from 'next'

const basePath = process.env.NEXT_PUBLIC_BASE_PATH || ''
const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || ''
const aiBaseUrl = process.env.NEXT_PUBLIC_AI_BASE_URL || ''

const nextConfig: NextConfig = {
  output: 'standalone',
  basePath,
  // basePath 가 있는 환경(/dev)에서 nginx 가 슬래시 없는 요청을 슬래시 있는 형태로 301 시키므로,
  // Next.js 도 동일 방향으로 통일해 루프를 끊는다.
  trailingSlash: true,
  reactCompiler: true,
  // 로컬(localhost) 개발 환경 한정 same-origin 프록시.
  // 백엔드(k14e101.p.ssafy.io)와 도메인이 달라 SameSite=Lax 쿠키가 차단되는 문제를 우회.
  // 클라이언트는 hostname 이 localhost 일 때만 /api-proxy/* 로 요청 (lib/client.ts 분기).
  // 배포(dev/prod)에서는 client 가 절대 URL 로 직접 호출하므로 이 rewrite 는 호출되지 않음 (dormant).
  async rewrites() {
    const rules = []
    if (apiBaseUrl) {
      rules.push({
        source: '/api-proxy/:path*',
        destination: `${apiBaseUrl}/:path*`,
      })
    }
    if (aiBaseUrl) {
      rules.push({
        source: '/ai-proxy/:path*',
        destination: `${aiBaseUrl}/:path*`,
      })
    }
    return rules
  },
}

export default nextConfig

