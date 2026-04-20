/**
 * 대시보드 공통 레이아웃.
 * proxy.ts에서 비로그인 시 /login 리다이렉트.
 */
import { Suspense } from "react";
import { Header } from "@/components/layout/Header";
import { Sidebar } from "@/components/layout/Sidebar";

export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <Header />
      <div className="flex flex-1">
        <Suspense
          fallback={<div className="w-64 border-r bg-white shrink-0" />}
        >
          <Sidebar />
        </Suspense>
        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </>
  );
}
