from kiwipiepy import Kiwi

class MorphemeAnalyzer:
    def __init__(self):
        self.kiwi = Kiwi()
        print("Kiwi 형태소 분석기 초기화 완료")

    def analyze(self, text: str) -> list:
        """
        텍스트를 형태소 분석하여 단어 목록 반환
        각 단어가 일반 단어인지, 미등록어(의료 용어 후보)인지 표시
        """
        result = self.kiwi.tokenize(text)
        
        words = []
        for token in result:
            words.append({
                "word": token.form,          # 원본 단어
                "pos": token.tag,            # 품사 태그
                "start": token.start,        # 시작 위치
                "end": token.start + token.len,  # 끝 위치
                "is_unknown": token.tag == "NNP" or token.tag == "SL" or token.tag == "UN"
                # NNP: 고유명사 (약품명 등), SL: 외국어, UN: 미등록어
            })

        return words
    
    def extract_medical_candidates(self, text: str) -> list:
        """
        텍스트에서 의료 용어 후보만 추출
        미등록어, 고유명사, 외국어를 의료 용어 후보로 판단
        """
        tokens = self.analyze(text)
        
        candidates = []
        for token in tokens:
            if token["is_unknown"]:
                candidates.append({
                    "word": token["word"],
                    "start": token["start"],
                    "end": token["end"],
                    "normalized": token["word"].replace(" ", "").lower()
                })

        print(f"형태소 분석 결과: 총 {len(tokens)}개 토큰, 의료 후보 {len(candidates)}개")
        return candidates
    