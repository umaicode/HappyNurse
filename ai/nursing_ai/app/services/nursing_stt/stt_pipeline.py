from app.services.nursing_stt.clova_stt import ClovaSTTClient
from app.services.nursing_stt.morpheme import MorphemeAnalyzer
from app.services.nursing_stt.term_mapper import TermMapper
from sqlalchemy.orm import Session

class STTPipeline:
    def __init__(self, db: Session = None):
        self.clova = ClovaSTTClient()
        self.morpheme = MorphemeAnalyzer()
        # db 가 주어지면 즉시 DB 사전 로드 (기존 호출자 호환).
        # 싱글톤 라이프타임 동안 한 번만 init 하고, 사전 로드는 load_dictionary() 로 미루는 게 권장.
        self.mapper = TermMapper(db=db)
        print("STT 파이프라인 초기화 완료")

    def load_dictionary(self, db: Session) -> None:
        """싱글톤 라이프타임 동안 한 번 호출 — TermMapper 의 사전을 DB 에서 reload.

        이미 default 사전으로 init 된 mapper 를 DB 사전으로 교체한다.
        Kiwi/ClovaSTTClient 는 재초기화 안 함 (비용 큰 컴포넌트 유지).
        """
        self.mapper = TermMapper(db=db)

    async def process(self, audio_data: bytes, filename: str = "audio.wav", apply_nc: bool = False) -> dict:
        print(f"\n=== 1단계: 클로바 STT (NC={'on' if apply_nc else 'off'}) ===")
        stt_result = await self.clova.recognize(audio_data, filename, apply_nc=apply_nc)
        original_text = stt_result["text"]
        stt_confidence = stt_result.get("confidence")
        stt_segments = stt_result.get("segments", [])
        nc_latency_ms = stt_result.get("nc_latency_ms")
        print(f"STT 결과: {original_text}")

        if not original_text:
            return {
                "original_text": "",
                "corrected_text": "",
                "corrections": [],
                "stt_confidence": stt_confidence,
                "stt_segments": stt_segments,
                "nc_latency_ms": nc_latency_ms,
            }

        print("\n=== 2단계: 형태소 분석 ===")
        morpheme_candidates = self.morpheme.extract_medical_candidates(original_text)

        # 사전 매칭이 형태소 후보보다 정확하므로 dict hit 을 먼저 둠.
        # 형태소 후보가 사전 hit 범위 안에 포함되거나 같은 시작점이면 버림
        # (예: Kiwi 가 "오메프라졸졸"을 "오메프라졸"+"졸"로 분해해 dict hit 을 가리는 케이스 방지).
        dictionary_hits = self.mapper.find_dictionary_matches(original_text)
        candidates = list(dictionary_hits)
        hit_ranges = [(h["start"], h["end"]) for h in dictionary_hits]
        for cand in morpheme_candidates:
            covered = any(s <= cand["start"] and cand["end"] <= e for s, e in hit_ranges)
            if not covered:
                candidates.append(cand)
        print(f"사전 hit {len(dictionary_hits)}개, 형태소 후보 {len(morpheme_candidates)}개, 병합 {len(candidates)}개")

        print("\n=== 3단계: 용어 매핑 ===")
        mapping_result = self.mapper.process_text(original_text, candidates)

        return {
            "original_text": original_text,
            "corrected_text": mapping_result["corrected_text"],
            "corrections": mapping_result["corrections"],
            "stt_confidence": stt_confidence,
            "stt_segments": stt_segments,
            "nc_latency_ms": nc_latency_ms,
        }


_PIPELINE: STTPipeline | None = None


def get_stt_pipeline() -> STTPipeline:
    """프로세스 라이프타임 싱글톤 액세서.

    첫 호출 시 STTPipeline() 을 1회 인스턴스화 (Kiwi 로드 ~1-3초, default 사전).
    DB 사전은 app 시작 시 main.py 가 load_dictionary() 로 채움 — 그게 누락된 경우
    여기서 lazy 하게 호출자가 load_dictionary 를 부르도록 default 사전 그대로 유지.
    """
    global _PIPELINE
    if _PIPELINE is None:
        _PIPELINE = STTPipeline(db=None)
    return _PIPELINE
