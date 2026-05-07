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

    async def process(self, audio_data: bytes, filename: str = "audio.wav", skip_correction: bool = False) -> dict:
        print("\n=== 1단계: 클로바 STT ===")
        original_text = await self.clova.recognize(audio_data, filename)
        print(f"STT 결과: {original_text}")

        if not original_text:
            return {
                "original_text": "",
                "corrected_text": "",
                "corrections": []
            }

        # 환자 모드 등 raw STT만 필요한 경우 Stage 2/3 우회.
        # 환자가 자연어로 말하므로 의료 용어 자동 교정이 의도를 왜곡할 위험을 회피.
        if skip_correction:
            print("=== skip_correction=True: 형태소/용어 매핑 우회 ===")
            return {
                "original_text": original_text,
                "corrected_text": original_text,
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