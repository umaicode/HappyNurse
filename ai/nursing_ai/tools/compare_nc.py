"""NC on/off A/B 비교 CLI (라벨링-프리).

사용:
  cd ai/nursing_ai
  python -m tools.compare_nc <audio_file> [<audio_file> ...] [--out-dir reports/] [--no-db]

산출물:
- stdout       : 비교 표
- <out-dir>/<timestamp>/<filename>_spec.png : NC 전/후 mel-spectrogram + delta
- <out-dir>/<timestamp>/report.json         : 모든 메트릭 raw
"""

from __future__ import annotations

import argparse
import asyncio
import json
import sys
from collections import Counter
from datetime import datetime
from pathlib import Path

# Windows cp949 stdout 에서 일부 unicode 글리프(em-dash 등) 가 깨지지 않도록 UTF-8 로 강제
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

# `python tools/compare_nc.py` 로 직접 실행해도 import 되도록 nursing_ai 디렉토리를 path 에 추가
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.services.nursing_stt.stt_pipeline import STTPipeline  # noqa: E402
from app.services.nursing_stt.term_mapper import TermMapper  # noqa: E402
from tools.spectrogram import render_nc_comparison  # noqa: E402


def _try_open_db():
    try:
        from app.database.db import SessionLocal

        return SessionLocal()
    except Exception as e:
        print(f"[compare_nc] DB 세션 생성 실패, default 사전 사용: {e}")
        return None


def _count_overlapping_hits(hits: list) -> int:
    """`find_dictionary_matches` 가 반환한 hit 들 중, 자기보다 긴 hit 에 strict 하게 포함되는 짧은 hit 개수.

    예: text="Npo" 일 때 dict 가 "po"(1-3) 와 "Npo"(0-3) 둘 다 반환하면, "po" 는 "Npo" 에 포함되므로 1.
    이전 대화에서 발견한 사전 hit dedup 누락 버그를 자동 감지하기 위함.
    """
    count = 0
    for i, h1 in enumerate(hits):
        for j, h2 in enumerate(hits):
            if i == j:
                continue
            if (
                h2["start"] <= h1["start"]
                and h1["end"] <= h2["end"]
                and (h2["start"] != h1["start"] or h2["end"] != h1["end"])
            ):
                count += 1
                break
    return count


async def _run_one(
    pipeline: STTPipeline,
    mapper: TermMapper,
    audio_bytes: bytes,
    filename: str,
    apply_nc: bool,
) -> dict:
    result = await pipeline.process(audio_bytes, filename, apply_nc=apply_nc)

    corrections = result["corrections"]
    types = Counter(c.get("type") for c in corrections)

    if result["original_text"]:
        hits = mapper.find_dictionary_matches(result["original_text"])
        overlap = _count_overlapping_hits(hits)
        dict_hit_count = len(hits)
    else:
        overlap = 0
        dict_hit_count = 0

    segs = result.get("stt_segments") or []
    seg_confs = [s.get("confidence") for s in segs if s.get("confidence") is not None]
    mean_seg = sum(seg_confs) / len(seg_confs) if seg_confs else None
    min_seg = min(seg_confs) if seg_confs else None

    return {
        "apply_nc": apply_nc,
        "original_text": result["original_text"],
        "corrected_text": result["corrected_text"],
        "corrections": corrections,
        "stt_confidence": result.get("stt_confidence"),
        "stt_mean_segment_confidence": mean_seg,
        "stt_min_segment_confidence": min_seg,
        "nc_latency_ms": result.get("nc_latency_ms"),
        "mapping_exact": types.get("exact", 0),
        "mapping_fuzzy": types.get("fuzzy", 0),
        "mapping_candidates": types.get("candidates", 0),
        "dict_hit_count": dict_hit_count,
        "dict_hit_overlap_count": overlap,
    }


def _fmt(val, fmt=".4f"):
    if val is None:
        return "-"
    if isinstance(val, float):
        return format(val, fmt)
    return str(val)


def _print_table(rows: list) -> None:
    headers = [
        "File",
        "NC",
        "text(앞 40자)",
        "conf",
        "mean seg",
        "min seg",
        "NC ms",
        "exact/fuzzy/cand",
        "dict hit(중첩)",
    ]
    print("\n" + " | ".join(headers))
    print("-" * 150)
    for row in rows:
        for side in ("off", "on"):
            r = row[side]
            text_preview = (r["original_text"] or "")[:40]
            mapping = f"{r['mapping_exact']}/{r['mapping_fuzzy']}/{r['mapping_candidates']}"
            dict_str = f"{r['dict_hit_count']}({r['dict_hit_overlap_count']})"
            print(
                " | ".join(
                    [
                        row["file"][:30],
                        side,
                        text_preview,
                        _fmt(r["stt_confidence"]),
                        _fmt(r["stt_mean_segment_confidence"]),
                        _fmt(r["stt_min_segment_confidence"]),
                        _fmt(r["nc_latency_ms"], ".1f"),
                        mapping,
                        dict_str,
                    ]
                )
            )
        print("-" * 150)


def _print_summary(rows: list) -> None:
    """전체 파일 평균 NC on vs off delta 요약. CLOVA confidence 차이 한 줄 등."""
    if not rows:
        return

    def _avg(key, side):
        vals = [row[side].get(key) for row in rows if row[side].get(key) is not None]
        return sum(vals) / len(vals) if vals else None

    avg_off = _avg("stt_confidence", "off")
    avg_on = _avg("stt_confidence", "on")

    print("\n=== 종합 (N={}) ===".format(len(rows)))
    if avg_off is not None and avg_on is not None:
        delta = avg_on - avg_off
        arrow = "↓" if delta < 0 else ("↑" if delta > 0 else "→")
        print(
            f"  CLOVA confidence 평균: NC off={avg_off:.4f}, NC on={avg_on:.4f}, "
            f"Δ={delta:+.4f} {arrow}"
        )
    else:
        print("  CLOVA confidence: 데이터 없음")

    avg_nc_ms = _avg("nc_latency_ms", "on")
    if avg_nc_ms is not None:
        print(f"  NC 처리 평균 latency: {avg_nc_ms:.1f}ms")

    overlap_files = [
        row["file"]
        for row in rows
        if row["off"]["dict_hit_overlap_count"] > 0 or row["on"]["dict_hit_overlap_count"] > 0
    ]
    if overlap_files:
        print(f"  사전 hit 중첩 감지된 파일 {len(overlap_files)}개: {overlap_files[:5]}")


async def _main_async(paths: list, out_dir: str, use_db: bool) -> None:
    db = _try_open_db() if use_db else None
    try:
        pipeline = STTPipeline(db=db)
        mapper = pipeline.mapper

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_dir = Path(out_dir) / timestamp
        report_dir.mkdir(parents=True, exist_ok=True)

        rows = []
        full_report = []
        for path_str in paths:
            path = Path(path_str)
            if not path.exists():
                print(f"[compare_nc] 파일 없음, 스킵: {path}")
                continue

            print(f"\n========== {path.name} ==========")
            audio_bytes = path.read_bytes()

            off_result = await _run_one(pipeline, mapper, audio_bytes, path.name, apply_nc=False)
            on_result = await _run_one(pipeline, mapper, audio_bytes, path.name, apply_nc=True)

            png_path = report_dir / f"{path.stem}_spec.png"
            try:
                spec_metrics = render_nc_comparison(audio_bytes, path.name, png_path)
            except Exception as e:
                print(f"[compare_nc] 스펙트로그램 생성 실패: {e}")
                spec_metrics = {"error": str(e)}

            rows.append({"file": path.name, "off": off_result, "on": on_result})
            full_report.append(
                {
                    "file": str(path),
                    "filename": path.name,
                    "off": off_result,
                    "on": on_result,
                    "spectrogram": spec_metrics,
                }
            )

        _print_table(rows)
        _print_summary(rows)

        report_path = report_dir / "report.json"
        with report_path.open("w", encoding="utf-8") as f:
            json.dump(full_report, f, ensure_ascii=False, indent=2, default=str)
        print(f"\n[compare_nc] 리포트 저장: {report_path}")
        print(f"[compare_nc] 스펙트로그램 PNG 저장 위치: {report_dir}")
    finally:
        if db is not None:
            db.close()


def main() -> None:
    parser = argparse.ArgumentParser(description="STT NC on/off A/B 비교 (라벨링 불필요)")
    parser.add_argument("paths", nargs="+", help="비교할 오디오 파일 경로들")
    parser.add_argument(
        "--out-dir", default="tools/reports", help="리포트 저장 디렉토리 (기본: tools/reports)"
    )
    parser.add_argument(
        "--no-db",
        action="store_true",
        help="DB 매핑 사전 대신 default 사전 사용 (DB 없이 실행할 때)",
    )
    args = parser.parse_args()

    asyncio.run(_main_async(args.paths, args.out_dir, use_db=not args.no_db))


if __name__ == "__main__":
    main()
