// 환자용 레이아웃
// Scope: src/app/patient/**

export default function PatientLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="flex min-h-dvh w-full items-start justify-center bg-[#e9ebef] sm:items-center">
      <div className="flex h-dvh w-full max-w-[412px] flex-col overflow-hidden bg-white">
        {children}
      </div>
    </div>
  );
}
