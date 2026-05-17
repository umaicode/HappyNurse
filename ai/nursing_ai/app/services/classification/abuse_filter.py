"""환자 자유텍스트의 부적절 발화(욕설·우회 욕설·성희롱·모욕·기분 나쁜 발화)를
LLM으로 식별하고 삭제한 cleaned_text 를 산출.

분류기(llm_classifier)와 다른 점:
- temperature=0 (결정적) — 같은 입력 → 같은 출력
- 응답 스키마 minimal: {cleaned_text: str | null}
- 분류·라벨링 X — 정제만 담당
"""
from __future__ import annotations

import json
import logging
import os
from typing import Optional

import anthropic

logger = logging.getLogger(__name__)


SYSTEM_PROMPT = """당신은 병원 환자 호출 시스템의 발화 정제기입니다.
환자가 보낸 자유텍스트에서 부적절한 표현을 **삭제** 하고, 의료 내용은 절대 손상시키지 않습니다.

## 정제 대상 (부적절 발화)

1. 직접 욕설/비속어: 씨발, 좆같다, 개새끼, 미친 등
2. 우회 욕설: 자모 분리(ㅅㅂ, ㅈㄴ), 특수기호 끼움(씨@발, ㅅ.발), 로마자(shibal, ssibal), 의도적 띄어쓰기
3. 성희롱: 외모 평가(예쁘다, 몸매), 사적 접근(연락처, 데이트), 성적 발언, 의료 맥락 외 인격 대상화
4. 모욕·혐오: 인격 비하, 직업·서비스 비난을 인격으로 환원, 욕설성 표현
5. 기분 나쁜 발화: 비아냥·과한 압박·강압적 명령

## 절대 손대지 않는 것

- 의료 내용: 증상, 부위, 강도, 시간, 빈도
- 정상 통증 호소: "죽을 것 같다", "못 견디겠다", "참을 수가 없어요" 등
- 일반 호소: "도와주세요", "빨리 와주세요", "급해요"
- **자살·자해 신호**: "죽고 싶어요", "살기 싫어요", "끝내고 싶어요" 등 — 임상적으로 중요한 신호이므로 절대 보존
- **정신과 자가인식 호소**: "내가 미친 것 같아요", "정신이 나가는 것 같아요", "환각이 보여요" 등 — 정신과 임상 신호, 절대 보존

## 정제 규칙

1. 부적절 표현 **삭제**. 절대 새 단어를 추가하지 말 것. 환자 원문 단어 외의 것은 출력 금지.
2. 자연스러운 문장 흐름을 위해 다음만 허용:
   - 부적절 표현에 붙은 조사(이/가/을/를/은/는/도) 같이 제거
   - 연속된 공백·구두점은 1개로 정리
3. 정상 발화면 cleaned_text = null
4. 정제 후에도 의료·일반 내용이 남으면, **그 남은 텍스트 자체를 cleaned_text 값으로 출력**.
   (절대 "<...>", "[...]", "정제된 본문" 같은 자리표시자(placeholder) 문자열을 그대로 출력하지 말 것.
   예: 원문 "씨발 진짜 아파" → cleaned_text="진짜 아파" 처럼 실제 남은 단어를 그대로 출력.)
5. 부적절 표현만 있어서 정제 후 텍스트가 비면 cleaned_text = ""

## 응답 형식

반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트 포함 금지.

{"cleaned_text": "실제 정제 후 텍스트" 또는 null 또는 ""}
"""


FEW_SHOT_EXAMPLES = [
    # 1. 정상 발화 — null
    {"role": "user", "content": "머리가 너무 아파요"},
    {"role": "assistant", "content": '{"cleaned_text":null}'},

    # 2. 직접 욕설 + 의료 — 삭제
    {"role": "user", "content": "씨발 진짜 아파"},
    {"role": "assistant", "content": '{"cleaned_text":"진짜 아파"}'},

    # 3. 우회 욕설 — 삭제
    {"role": "user", "content": "ㅅㅂ 답답해 죽겠어요"},
    {"role": "assistant", "content": '{"cleaned_text":"답답해 죽겠어요"}'},

    # 4. 성희롱 + 의료 — 부적절만 삭제
    {"role": "user", "content": "간호사님 예뻐요 ㅎㅎ 빨리 와주세요"},
    {"role": "assistant", "content": '{"cleaned_text":"빨리 와주세요"}'},

    # 5. 부적절 발화만 — 빈 문자열
    {"role": "user", "content": "ㅅㅂ"},
    {"role": "assistant", "content": '{"cleaned_text":""}'},

    # 6. 모욕 + 의료 — 부적절만 삭제
    {"role": "user", "content": "이딴 서비스로 무슨 병원이라고 빨리 좀 와봐요"},
    {"role": "assistant", "content": '{"cleaned_text":"빨리 좀 와봐요"}'},

    # 7. 정상 통증 호소 보존 (false positive 방지)
    {"role": "user", "content": "죽을 것 같이 아파요"},
    {"role": "assistant", "content": '{"cleaned_text":null}'},

    # 8. 자살 신호 — 임상적으로 중요, 절대 보존
    {"role": "user", "content": "약을 먹으면 죽고 싶어요"},
    {"role": "assistant", "content": '{"cleaned_text":null}'},

    # 9. 정신과 자가인식 호소 — 임상적으로 중요, 절대 보존
    {"role": "user", "content": "내가 미친 것 같아요 환각이 보여요"},
    {"role": "assistant", "content": '{"cleaned_text":null}'},
]


class AbuseFilter:
    """환자 자유텍스트에서 부적절 표현을 LLM으로 삭제하는 정제기."""

    def __init__(
        self,
        api_key: Optional[str] = None,
        model: Optional[str] = None,
        client: Optional[anthropic.Anthropic] = None,
    ):
        self.model = model or os.getenv("ANTHROPIC_MODEL", "claude-haiku-4-5-20251001")
        if client is not None:
            self.client = client
        else:
            # GMS 게이트웨이 토큰을 anthropic SDK 의 api_key 자리에 그대로 전달.
            # handover/clients/llm_client.py 와 동일 컨벤션.
            key = api_key or os.getenv("GMS_API_KEY")
            if not key:
                raise RuntimeError("GMS_API_KEY is not set")
            self.client = anthropic.Anthropic(
                base_url=os.getenv(
                    "ANTHROPIC_BASE_URL",
                    "https://gms.ssafy.io/gmsapi/api.anthropic.com",
                ),
                api_key=key,
            )

    def filter(self, symptom_text: str) -> dict:
        """부적절 표현을 삭제한 cleaned_text 산출.

        Returns:
            dict: `{"cleaned_text": str | None}` —
                정상 발화면 None, 부적절 표현 일부 + 정상 내용이면 정제된 문자열,
                부적절 표현만이면 빈 문자열.
        """
        messages = [*FEW_SHOT_EXAMPLES, {"role": "user", "content": symptom_text}]
        response = self.client.messages.create(
            model=self.model,
            max_tokens=400,
            temperature=0,  # 결정적 — 같은 입력 → 같은 출력
            system=SYSTEM_PROMPT,
            messages=messages,
        )
        raw_text = response.content[0].text if response.content else ""
        return _parse_filter_response(raw_text)


def _parse_filter_response(raw_text: str) -> dict:
    text = raw_text.strip()
    # 모델이 JSON 외 잡문구를 붙였을 경우 첫 '{'~ 마지막 '}' 만 추출
    start = text.find("{")
    end = text.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise ValueError(f"LLM response did not contain JSON: {raw_text!r}")
    payload = json.loads(text[start : end + 1])
    cleaned = payload.get("cleaned_text")
    if cleaned is not None and not isinstance(cleaned, str):
        raise ValueError(f"cleaned_text must be string or null, got {type(cleaned).__name__}")
    return {"cleaned_text": cleaned}
