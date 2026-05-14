import { cookies } from "next/headers";
import { redirect } from "next/navigation";

// 루트 진입 시 쿠키 유무로 분기 — 서버 컴포넌트에서 1회 결정.
// Next.js 13.4+ 의 redirect() 는 basePath 를 자동 적용한다 (옛 주석의 "basePath 가 사라진다" 는 13 이전 이슈).
export default async function RootPage() {
  const cookieStore = await cookies();
  const hasAccessToken = cookieStore.has("ACCESS_TOKEN");
  redirect(hasAccessToken ? "/dashboard" : "/login");
}
