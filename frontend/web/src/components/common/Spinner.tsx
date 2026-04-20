/**
 * 로딩 스피너.
 */
export function Spinner() {
  return (
    <div
      className="inline-block w-6 h-6 border-2 border-gray-300 border-t-blue-500 rounded-full animate-spin"
      aria-label="로딩 중"
    />
  )
}
