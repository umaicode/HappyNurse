from app.services.clova_stt import ClovaSTTClient
from app.services.morpheme import MorphemeAnalyzer
from app.services.term_mapper import TermMapper

class STTPipeline:
    def __init__(self):
        self.clova = ClovaSTTClient()
        self.morpheme = MorphemeAnalyzer()
        self.mapper = TermMapper()
        print("STT 파이프라인 초기화 완료")

    async def process(self, audio_data: bytes, filename: str = "audio.wav") -> dict:
        """
        음성 → STT → 형태소 분석 → 매핑 → 정제된 텍스트
        """
        # 1. 클로바 STT
        print("\n=== 1단계: 클로바 STT ===")
        original_text = await self.clova.recognize(audio_data, filename)
        print(f"STT 결과: {original_text}")

        if not original_text:
            return {
                "original_text": "",
                "corrected_text": "",
                "corrections": []
            }

        # 2. 형태소 분석
        print("\n=== 2단계: 형태소 분석 ===")
        candidates = self.morpheme.extract_medical_candidates(original_text)

        # 3. 매핑 처리
        print("\n=== 3단계: 용어 매핑 ===")
        mapping_result = self.mapper.process_text(original_text, candidates)

        return {
            "original_text": original_text,
            "corrected_text": mapping_result["corrected_text"],
            "corrections": mapping_result["corrections"]
        }
    