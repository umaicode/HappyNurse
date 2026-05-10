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
        candidates = self.morpheme.extract_medical_candidates(original_text)

        print("\n=== 3단계: 용어 매핑 ===")
        mapping_result = self.mapper.process_text(original_text, candidates)

        return {
            "original_text": original_text,
            "corrected_text": mapping_result["corrected_text"],
            "corrections": mapping_result["corrections"]
        }