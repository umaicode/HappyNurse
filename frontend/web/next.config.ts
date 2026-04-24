/**
 * Next.js 16 설정.
 * Turbopack 기본값 · React Compiler 선택 적용.
 *
 * [Next.js 16 변경]
 * Before: "dev": "next dev --turbopack"
 * After:  "dev": "next dev"  → 플래그 불필요 (Turbopack이 기본값)
 */
import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  output: 'standalone',
  reactCompiler: true, // React Compiler 활성화 (선택)
}

export default nextConfig
