import type { Metadata } from 'next'
import './globals.css'
import { QueryProvider } from '@/lib/query-provider'

export const metadata: Metadata = {
  title: 'HappyNurse',
  description: '간호 기록 관리 시스템',
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="ko" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  )
}
