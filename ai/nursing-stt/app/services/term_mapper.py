from rapidfuzz import fuzz, process

class TermMapper:
    def __init__(self):
        # 테스트용 매핑 사전 (나중에 DB로 교체)
        self.exact_dict = {
            # STT 오인식 → 정식 용어
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

        # 정식 의료 용어 목록 (퍼지 매칭 대상)
        self.medical_terms = [
            "아세트아미노펜", "세프트리악손", "푸로세미드", "라식스",
            "케토롤락", "디클로페낙", "이부프로펜", "멜록시캄",
            "인슐린", "헤파린", "반코마이신", "메트포르민",
            "닥터노티", "프리메디", "바이탈", "트랜스퍼",
            "배액량", "드레싱", "석션", "NPO",
            "1호실", "2호실", "3호실", "4호실", "5호실",
        ]

        print(f"매핑 사전 초기화: 정확 매칭 {len(self.exact_dict)}개, 퍼지 매칭 대상 {len(self.medical_terms)}개")

    def exact_match(self, word: str) -> str | None:
        """1차: 정확 매칭"""
        # 원본으로 먼저 찾기
        if word in self.exact_dict:
            return self.exact_dict[word]
        # 공백 제거 후 찾기
        normalized = word.replace(" ", "")
        if normalized in self.exact_dict:
            return self.exact_dict[normalized]
        return None

    def fuzzy_match(self, word: str, threshold: int = 70) -> list:
        """2차: 퍼지 매칭 - 유사한 후보 단어 제공"""
        normalized = word.replace(" ", "")
        
        matches = process.extract(
            normalized,
            self.medical_terms,
            scorer=fuzz.ratio,
            limit=3
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
        """전체 텍스트에 대해 매핑 처리"""
        corrected_text = text
        corrections = []

        for candidate in medical_candidates:
            word = candidate["word"]

            # 1차: 정확 매칭
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

            # 2차: 퍼지 매칭
            fuzzy_results = self.fuzzy_match(word)
            if fuzzy_results:
                # 최고 점수 후보로 자동 치환
                best = fuzzy_results[0]
                if best["confidence_score"] >= 0.85:
                    corrected_text = corrected_text.replace(word, best["suggested_word"])
                    corrections.append({
                        "original": word,
                        "corrected": best["suggested_word"],
                        "type": "fuzzy",
                        "confidence": best["confidence_score"],
                        "candidates": fuzzy_results
                    })
                    print(f"  퍼지 매칭 (자동): {word} → {best['suggested_word']} ({best['confidence_score']})")
                else:
                    # 신뢰도 낮으면 후보만 제시
                    corrections.append({
                        "original": word,
                        "corrected": None,
                        "type": "candidates",
                        "confidence": best["confidence_score"],
                        "candidates": fuzzy_results
                    })
                    print(f"  퍼지 매칭 (후보 제시): {word} → {fuzzy_results}")

        return {
            "corrected_text": corrected_text,
            "corrections": corrections
        }
    