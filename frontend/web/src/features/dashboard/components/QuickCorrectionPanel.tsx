"use client";

import { useMemo, useState } from "react";
import { Zap } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  useAnalyzeCorrections,
  useApplyCorrection,
} from "../hooks/useCorrection";
import type { CorrectionCandidate, CorrectionItem } from "../types/correction";

const correctionKey = (correction: CorrectionItem) =>
  `${correction.start}-${correction.end}-${correction.original}`;

// 한 칩에 대해 어떤 후보가 적용됐는지(또는 원본 유지) 추적. 다시 누르면 원본으로 undo 가능.
type AppliedState = {
  // 현재 본문에 들어간 단어 (원본 유지면 original 과 동일)
  currentWord: string;
  // 사용자 마지막으로 선택한 type
  type: "exact" | "fuzzy" | "manual" | "original";
};

/**
 * STT 행 수정 모드 폼 아래에 띄우는 퀵수정 패널.
 *
 * - 마운트 시 1회 analyzeCorrections 호출 (수정 모드 진입 = 마운트).
 * - 응답의 corrections 를 start 오름차순으로 정렬해 칩 N개 표시.
 * - 칩 클릭 → Popover 로 후보 메뉴. 후보 선택 → 본문 정확 치환 + applyCorrection 피드백 저장.
 * - 한 번 적용한 칩도 목록에 그대로 남는다 (사라지지 않음). 다시 클릭해 다른 후보 또는 원본으로 되돌릴 수 있다 (undo).
 *
 * 본문 갱신은 부모(NoteRow) 의 draftContent state 를 onApply 콜백으로 위임 (slice + replace + slice).
 * 본문이 바뀌면 (start, end) offset 이 어긋날 수 있어 하이라이트는 시각적으로만 정확하지 않을 수 있다.
 * 그러나 칩의 origin 위치 정보는 분석 응답 시점 기준 그대로 유지된다.
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
  // 칩별 마지막 선택 상태. 칩이 사라지지 않고 적용 표시만 바뀐다.
  const [appliedByKey, setAppliedByKey] = useState<Map<string, AppliedState>>(
    () => new Map(),
  );

  // 본문 등장 순서 (사용자 결정) — 적용 여부와 무관하게 분석된 모든 칩을 유지.
  const sortedCorrections = useMemo<CorrectionItem[]>(
    () => [...(data?.corrections ?? [])].sort((a, b) => a.start - b.start),
    [data],
  );

  if (isPending) {
    return (
      <div className="mt-1.5 flex items-center gap-1.5 text-body-micro text-content-muted">
        <Zap className="size-3 fill-yellow-400 text-yellow-400" />
        <span>퀵수정 후보 분석 중...</span>
      </div>
    );
  }

  if (isError || sortedCorrections.length === 0) {
    return null;
  }

  return (
    <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
      <div className="flex items-center gap-1 text-[14px] leading-none font-medium text-[#323233]">
        <Zap className="size-4 fill-yellow-400 text-yellow-400" />
        <span>퀵수정 후보</span>
        <span className="inline-flex items-center justify-center size-4 rounded-full bg-amber-400 text-white text-[12px] font-semibold leading-none">
          {sortedCorrections.length}
        </span>
      </div>
      {sortedCorrections.map((correction) => {
        const key = correctionKey(correction);
        const applied = appliedByKey.get(key);
        return (
          <CorrectionChip
            key={key}
            correction={correction}
            applied={applied}
            onSelectCandidate={(candidate) => {
              // 현재 본문에 있는 단어 (적용된 단어 또는 원본)
              const currentWord = applied?.currentWord ?? correction.original;
              if (candidate.word === currentWord) {
                // 같은 후보 재선택은 noop.
                return;
              }
              // 본문 갱신 — 다른 칩의 적용으로 본문 길이가 바뀌었더라도 currentWord 기준 첫 위치를 잡는 책임은 부모에 위임.
              // 여기선 분석 시점의 (start, end) 와 currentWord 를 같이 넘긴다 (부모가 currentWord 로 치환 후 새 word 삽입).
              onApply(
                correction.start,
                correction.start + currentWord.length,
                candidate.word,
                currentWord,
                candidate.type === "original" ? "manual" : candidate.type,
              );
              if (candidate.type !== "original") {
                applyMutation.mutate({
                  nursing_record_id: nursingRecordId,
                  original_word: correction.original,
                  replaced_word: candidate.word,
                  correction_type: candidate.type,
                });
              }
              setAppliedByKey((previous) => {
                const next = new Map(previous);
                next.set(key, {
                  currentWord: candidate.word,
                  type: candidate.type,
                });
                return next;
              });
            }}
          />
        );
      })}
    </div>
  );
}

function CorrectionChip({
  correction,
  applied,
  onSelectCandidate,
}: {
  correction: CorrectionItem;
  applied: AppliedState | undefined;
  onSelectCandidate: (candidate: CorrectionCandidate) => void;
}) {
  // 칩 라벨: 적용된 단어가 있으면 그걸 표시, 없으면 원본. 본문에 무엇이 들어가 있는지가 사용자 멘탈모델에 가깝다.
  const displayWord = applied?.currentWord ?? correction.original;
  const isModified =
    applied !== undefined &&
    applied.type !== "original" &&
    applied.currentWord !== correction.original;

  return (
    <Popover>
      <PopoverTrigger asChild>
        <button
          type="button"
          title={
            isModified
              ? `원본: ${correction.original} → ${applied!.currentWord}`
              : correction.original
          }
          className={cn(
            "inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-[14px] leading-none font-medium transition-colors",
            isModified
              ? "border-brand-primary border-1.5 bg-white text-brand-primary hover:bg-brand-surface"
              : "border-brand-primary/30 bg-brand-surface text-brand-primary hover:bg-brand-primary hover:text-white",
          )}
        >
          {isModified && (
            <span className="flex items-baseline gap-1 text-content-muted">
              <span className="line-through">{correction.original}</span>
              <span className="text-content-tertiary">→</span>
            </span>
          )}
          <span className={cn(isModified && "font-semibold")}>
            {displayWord}
          </span>
        </button>
      </PopoverTrigger>
      <PopoverContent
        align="start"
        sideOffset={4}
        className="w-auto min-w-[140px] p-1"
      >
        <div className="flex flex-col">
          {correction.candidates.map((candidate, index) => {
            const isCurrent =
              applied?.currentWord === candidate.word ||
              (applied === undefined && candidate.type === "original");
            return (
              <button
                key={`${candidate.word}-${index}`}
                type="button"
                onClick={() => onSelectCandidate(candidate)}
                className={cn(
                  "w-full text-left px-2.5 py-1.5 rounded text-[14px] leading-none hover:bg-brand-surface transition-colors flex items-center justify-between gap-2",
                  isCurrent && "bg-brand-surface",
                )}
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
                <span className="flex items-center gap-1.5 shrink-0">
                  {isCurrent && (
                    <span className="text-body-micro font-bold text-brand-primary">
                      현재
                    </span>
                  )}
                  {candidate.type === "original" && (
                    <span className="text-body-micro text-content-muted">
                      원본
                    </span>
                  )}
                </span>
              </button>
            );
          })}
        </div>
      </PopoverContent>
    </Popover>
  );
}
