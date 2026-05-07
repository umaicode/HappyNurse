"use client";

import { useMemo, useState } from "react";
import { Sparkles } from "lucide-react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useAnalyzeCorrections, useApplyCorrection } from "../hooks/useCorrection";
import type {
  CorrectionCandidate,
  CorrectionItem,
} from "../types/correction";

const correctionKey = (correction: CorrectionItem) =>
  `${correction.start}-${correction.end}-${correction.original}`;

/**
 * STT 행 수정 모드 폼 아래에 띄우는 퀵수정 패널.
 *
 * - 마운트 시 1회 analyzeCorrections 호출 (수정 모드 진입 = 마운트).
 * - 응답의 corrections 를 start 오름차순으로 정렬해 칩 N개 표시.
 * - 칩 클릭 → Popover 로 후보 메뉴. 후보 선택 → 본문 정확 치환 + applyCorrection 피드백 저장.
 *
 * 본문 갱신은 부모(NoteRow) 의 draftContent state 를 onApply 콜백으로 위임 (slice + replace + slice).
 */
export function QuickCorrectionPanel({
  nursingRecordId,
  content,
  onApply,
}: {
  nursingRecordId: number;
  content: string;
  // (start, end, replaced, original, type) — 부모가 draftContent slice 후 치환
  onApply: (
    start: number,
    end: number,
    replaced: string,
    original: string,
    correctionType: "exact" | "fuzzy" | "manual",
  ) => void;
}) {
  const { data, isPending, isError } = useAnalyzeCorrections(
    nursingRecordId,
    content,
    true,
  );
  const applyMutation = useApplyCorrection();
  // 한 번 처리된(적용 또는 원본 유지) 칩은 화면에서 제거 — 같은 위치를 다시 누를 일 없음.
  const [resolvedKeys, setResolvedKeys] = useState<Set<string>>(() => new Set());

  // 본문 등장 순서 (사용자 결정) + 이미 처리된 항목 제외.
  const sortedCorrections = useMemo<CorrectionItem[]>(
    () =>
      [...(data?.corrections ?? [])]
        .filter((correction) => !resolvedKeys.has(correctionKey(correction)))
        .sort((a, b) => a.start - b.start),
    [data, resolvedKeys],
  );

  if (isPending) {
    return (
      <div className="mt-1.5 flex items-center gap-1.5 text-body-micro text-content-muted">
        <Sparkles className="size-3" />
        <span>퀵수정 후보 분석 중...</span>
      </div>
    );
  }

  if (isError || sortedCorrections.length === 0) {
    return null;
  }

  return (
    <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
      <div className="flex items-center gap-1 text-body-micro font-bold text-brand-primary">
        <Sparkles className="size-3" />
        <span>퀵수정 후보 ({sortedCorrections.length})</span>
      </div>
      {sortedCorrections.map((correction) => (
        <CorrectionChip
          key={correctionKey(correction)}
          correction={correction}
          onSelectCandidate={(candidate) => {
            const key = correctionKey(correction);
            // "원본 유지" 는 본문/피드백 모두 noop — 칩만 사라진다 (사용자가 명시적으로 "안 바꿈" 선택).
            if (candidate.type !== "original") {
              onApply(
                correction.start,
                correction.end,
                candidate.word,
                correction.original,
                candidate.type,
              );
              applyMutation.mutate({
                nursing_record_id: nursingRecordId,
                original_word: correction.original,
                replaced_word: candidate.word,
                correction_type: candidate.type,
              });
            }
            setResolvedKeys((previous) => {
              const next = new Set(previous);
              next.add(key);
              return next;
            });
          }}
        />
      ))}
    </div>
  );
}

function CorrectionChip({
  correction,
  onSelectCandidate,
}: {
  correction: CorrectionItem;
  onSelectCandidate: (candidate: CorrectionCandidate) => void;
}) {
  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          className="px-2 py-0.5 rounded-full border border-brand-primary/30 bg-brand-surface text-brand-primary text-body-micro font-bold hover:bg-brand-primary hover:text-white transition-colors"
        >
          {correction.original}
        </button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        sideOffset={4}
        className="w-auto min-w-[140px] p-1"
      >
        <div className="flex flex-col">
          {correction.candidates.map((candidate, index) => (
            <button
              key={`${candidate.word}-${index}`}
              type="button"
              onClick={() => onSelectCandidate(candidate)}
              className="w-full text-left px-2.5 py-1.5 rounded text-body-sm hover:bg-brand-surface transition-colors flex items-center justify-between gap-2"
            >
              <span
                className={
                  candidate.type === "original"
                    ? "text-content-tertiary"
                    : "text-content-primary font-medium"
                }
              >
                {candidate.word}
              </span>
              {candidate.type === "original" && (
                <span className="text-body-micro text-content-muted">원본</span>
              )}
            </button>
          ))}
        </div>
      </PopoverContent>
    </Popover>
  );
}
