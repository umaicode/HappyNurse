// 환자용 반응형 레이아웃

export default function PatientLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="flex min-h-dvh w-full flex-col items-center bg-[#ececec] font-sans">
      <div className="flex w-full max-w-[420px] flex-1 flex-col px-5 py-10 bg-white">
        {children}
      </div>
    </div>
  );
}
