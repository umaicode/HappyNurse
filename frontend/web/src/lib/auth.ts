/**
 * NextAuth.js 설정.
 * JWT 전략 · 세션 타임아웃 15분.
 */
import NextAuth from 'next-auth'
import CredentialsProvider from 'next-auth/providers/credentials'

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [
    CredentialsProvider({
      name: 'Credentials',
      credentials: {
        hospitalCode: { label: '병원코드', type: 'text' },
        userId:       { label: '아이디',   type: 'text' },
        password:     { label: '비밀번호', type: 'password' },
        role:         { label: '직급',     type: 'text' },
      },
      async authorize(credentials) {
        // TODO: 실제 로그인 API 연동 (features/auth/api/index.ts login() 사용)
        return null
      },
    }),
  ],
  session: {
    strategy: 'jwt',
    maxAge: 15 * 60, // 세션 타임아웃 15분
  },
})
