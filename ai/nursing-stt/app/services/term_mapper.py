from rapidfuzz import fuzz, process
from sqlalchemy.orm import Session
from sqlalchemy import text

class TermMapper:
    def __init__(self, db: Session = None):
        self.db = db
        self.exact_dict = {}
        self.medical_terms = []

        if db:
            self._load_from_db()
        else:
            self._load_default()

        print(f"매핑 사전 초기화: 정확 매칭 {len(self.exact_dict)}개, 퍼지 매칭 대상 {len(self.medical_terms)}개")

    def _load_from_db(self):
        """DB에서 매핑 사전 로드"""
        try:
            # 정확 매칭 사전 로드
            rows = self.db.execute(text("""
                SELECT stt_word, correct_word, stt_word_normalized
                FROM quick_correction_dictionary
                WHERE is_active = true
            """)).fetchall()

            for row in rows:
                self.exact_dict[row[0]] = row[1]           # 원본 매칭
                self.exact_dict[row[2]] = row[1]           # 정규화 매칭

            # 퍼지 매칭 대상 (정식 용어 목록)
            terms = self.db.execute(text("""
                SELECT DISTINCT correct_word
                FROM quick_correction_dictionary
                WHERE is_active = true
            """)).fetchall()

            self.medical_terms = [row[0] for row in terms]

            # suggestion 테이블에서도 추가
            suggestions = self.db.execute(text("""
                SELECT DISTINCT suggested_word
                FROM quick_correction_suggestion
                WHERE is_active = true
            """)).fetchall()

            for row in suggestions:
                if row[0] not in self.medical_terms:
                    self.medical_terms.append(row[0])

            print("DB에서 매핑 사전 로드 완료")

        except Exception as e:
            print(f"DB 로드 실패, 기본 사전 사용: {e}")
            self._load_default()

    def _load_default(self):
        """테스트용 기본 사전"""
        self.exact_dict = {
            "사무실": "3호실",
            "좌우실": "4호실",
            "세포트리악손": "세프트리악손",
            "아세타미노펜": "아세트아미노펜",
            "세트 아미노세트": "아세트아미노펜",
            "게토롤라": "케토롤락",
            "배행량": "배액량",
            "엠피오": "NPO",
            "다크 노티": "닥터노티",
            "닭통을": "닥터노티",
            "트랜스포": "트랜스퍼",
            "프리미이": "프리메디",
            "유니티 피아": "유닛 피하",
        }

        self.medical_terms = [
            "아세트아미노펜", "세프트리악손", "푸로세미드", "라식스",
            "케토롤락", "디클로페낙", "이부프로펜", "멜록시캄",
            "인슐린", "헤파린", "반코마이신", "메트포르민",
            "닥터노티", "프리메디", "바이탈", "트랜스퍼",
            "배액량", "드레싱", "석션", "NPO",
            "1호실", "2호실", "3호실", "4호실", "5호실",
        ]

    def exact_match(self, word: str) -> str | None:
        if word in self.exact_dict:
            return self.exact_dict[word]
        normalized = word.replace(" ", "").lower()
        if normalized in self.exact_dict:
            return self.exact_dict[normalized]
        return None

    def fuzzy_match(self, word: str, threshold: int = 70) -> list:
        normalized = word.replace(" ", "")

        # rapidfuzz 라이브러리
        matches = process.extract(
            normalized,         # 찾으려는 오타 단어 (예: "아세트아미노팬")
            self.medical_terms, # 아까 __init__에서 만든 정답 단어 리스트
            scorer=fuzz.ratio,  # 레벤슈타인 거리 기반의 알고리즘 사용
            limit=3             # 다 찾지 말고, 제일 비슷한 거 딱 3개만 가져와!
        )

        candidates = []
        for match_word, score, _ in matches:
            if score >= threshold:
                candidates.append({
                    "suggested_word": match_word,
                    "confidence_score": round(score / 100, 2)
                })

        return candidates

    def process_text(self, text: str, medical_candidates: list) -> dict:
        corrected_text = text
        corrections = []

        for candidate in medical_candidates:
            word = candidate["word"]

            exact_result = self.exact_match(word)
            if exact_result:
                corrected_text = corrected_text.replace(word, exact_result)
                corrections.append({
                    "original": word,
                    "corrected": exact_result,
                    "type": "exact",
                    "confidence": 1.0
                })
                print(f"  정확 매칭: {word} → {exact_result}")
                continue

            # 퍼지 매칭
            fuzzy_results = self.fuzzy_match(word)
            if fuzzy_results:
                best = fuzzy_results[0]
                if best["confidence_score"] >= 0.85:
                    corrected_text = corrected_text.replace(word, best["suggested_word"])
                    corrections.append({
                        "original": word,
                        "corrected": best["suggested_word"], # 문장 바꿈
                        "type": "fuzzy",
                        "confidence": best["confidence_score"], # 후보는 적어줌
                        "candidates": fuzzy_results
                    })
                    print(f"  퍼지 매칭 (자동): {word} → {best['suggested_word']} ({best['confidence_score']})")
                else:
                    corrections.append({
                        "original": word,
                        "corrected": None,  # 문장 안바꿈
                        "type": "candidates",
                        "confidence": best["confidence_score"],
                        "candidates": fuzzy_results
                    })
                    print(f"  퍼지 매칭 (후보 제시): {word} → {fuzzy_results}")

        return {
            "corrected_text": corrected_text,
            "corrections": corrections
        }