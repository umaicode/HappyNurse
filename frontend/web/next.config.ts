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

const nextConfig: NextConfig = {
  output: 'standalone',
  basePath,
  // basePath 가 있는 환경(/dev)에서 nginx 가 슬래시 없는 요청을 슬래시 있는 형태로 301 시키므로,
  // Next.js 도 동일 방향으로 통일해 루프를 끊는다.
  trailingSlash: true,
  reactCompiler: true,
}

export default nextConfig

