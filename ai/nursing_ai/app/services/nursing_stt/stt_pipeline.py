from app.services.nursing_stt.clova_stt import ClovaSTTClient
from app.services.nursing_stt.morpheme import MorphemeAnalyzer
from app.services.nursing_stt.term_mapper import TermMapper
from sqlalchemy.orm import Session

class STTPipeline:
    def __init__(self, db: Session = None):
        self.clova = ClovaSTTClient()
        self.morpheme = MorphemeAnalyzer()
        self.mapper = TermMapper(db=db)
        print("STT 파이프라인 초기화 완료")

    async def process(self, audio_data: bytes, filename: str = "audio.wav", apply_nc: bool = False) -> dict:
        print(f"\n=== 1단계: 클로바 STT (NC={'on' if apply_nc else 'off'}) ===")
        original_text = await self.clova.recognize(audio_data, filename, apply_nc=apply_nc)
        print(f"STT 결과: {original_text}")

        if not original_text:
            return {
                "original_text": "",
                "corrected_text": "",
                "corrections": []
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
            "corrections": mapping_result["corrections"]
        }