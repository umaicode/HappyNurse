"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

// server-side redirect() 는 basePath 를 자동 적용하지 않아 dev 배포(/dev)에서
// /dev/login 이 아닌 /login 으로 가버린다. client router 는 basePath 자동 처리되므로
// useRouter 로 우회한다.
export default function RootPage() {
  const router = useRouter();
  useEffect(() => {
    router.replace("/login");
  }, [router]);
  return null;
}
