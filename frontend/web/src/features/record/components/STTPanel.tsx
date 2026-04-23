/**
 * [우패널] STT기록 + 포코기록 목록.
 * 미이관(주황) / 이관완료 상태 구분 · 이관 버튼.
 * → app/(web)/patients/[id]/page.tsx 우측 영역.
 */
"use client";

import { transfer } from "../api";
import type { STTRecord } from "../types";

export function STTPanel({ data }: { data: STTRecord[] }) {
  const handleTransfer = async (id: string) => {
    await transfer(id);
  };

  return (
    <section className="stt-panel w-1/2 overflow-y-auto p-4">
      <h2 className="font-bold mb-4">STT 기록</h2>
      <ul className="space-y-3">
        {data.map((record) => (
          <li
            key={record.id}
            className={`border rounded p-3 ${
              record.status === "PENDING"
                ? "border-orange-300 bg-orange-50"
                : "border-gray-200 bg-gray-50"
            }`}
          >
            <span className="text-xs font-medium">
              {record.status === "PENDING" ? "미이관" : "이관완료"}
            </span>
            <p className="mt-1">{record.originalText}</p>
            {record.ragText && (
              <p className="mt-1 text-blue-600 text-sm">{record.ragText}</p>
            )}
            <div className="flex items-center justify-between mt-2">
              <span className="text-xs text-gray-400">
                {record.nurseName} · {record.createdAt}
              </span>
              {record.status === "PENDING" && (
                <button
                  onClick={() => handleTransfer(record.id)}
                  className="text-xs px-2 py-1 bg-blue-500 text-white rounded"
                >
                  이관
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
