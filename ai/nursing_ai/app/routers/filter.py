"""환자 자유텍스트 부적절 발화 정제 엔드포인트.

흐름: BE WebappService 가 환자 자유텍스트를 받아 본 엔드포인트로 정제 → 정제된 텍스트로
사전 매칭·LLM 분류·저장·이벤트 발행 진행. 카드 선택 케이스는 호출하지 않음.

응답 의미:
- cleaned_text=null: 정상 발화 (정제 불필요)
- cleaned_text="<문자열>": 부적절 표현 일부 + 정상 내용 → 삭제된 본문
- cleaned_text="": 부적절 표현만 → BE 가 placeholder 로 치환
"""
from __future__ import annotations

import logging
from functools import lru_cache
from typing import Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.services.classification.abuse_filter import AbuseFilter

logger = logging.getLogger(__name__)

router = APIRouter()


# 기존 classification.py 의 _get_classifier 와 동일한 싱글톤 패턴 (lru_cache).
@lru_cache(maxsize=1)
def _get_filter() -> AbuseFilter:
    return AbuseFilter()


class FilterRequest(BaseModel):
    symptom_text: str = Field(..., min_length=1, max_length=2000, description="환자 발화 원문")


class FilterResponse(BaseModel):
    cleaned_text: Optional[str] = Field(
        None,
        description=(
            "부적절 발화 삭제 결과. 정상 발화면 null, "
            "부적절 표현만 있어서 정제 후 비면 빈 문자열, "
            "그 외엔 삭제된 본문."
        ),
    )


@router.post(
    "/symptom/filter",
    response_model=FilterResponse,
    summary="환자 발화 부적절 표현 정제",
    description=(
        "LLM(Claude Haiku, temperature=0)으로 욕설·우회 욕설·성희롱·모욕·기분 나쁜 발화를 "
        "삭제한 cleaned_text 를 반환. 정상 발화는 null."
    ),
)
async def filter_symptom_text(req: FilterRequest) -> FilterResponse:
    try:
        result = _get_filter().filter(req.symptom_text)
    except RuntimeError as e:
        # AbuseFilter 가 ANTHROPIC_API_KEY 누락 시 RuntimeError
        logger.error("Filter configuration error: %s", e)
        raise HTTPException(status_code=503, detail="filter service unavailable") from e
    except ValueError as e:
        # _parse_filter_response 가 LLM 응답 파싱 실패 시 ValueError
        logger.error("Filter response parsing failed: %s", e)
        raise HTTPException(status_code=502, detail="invalid LLM response") from e
    except Exception as e:
        logger.exception("Filter call failed")
        raise HTTPException(status_code=502, detail="filter call failed") from e

    return FilterResponse(cleaned_text=result.get("cleaned_text"))
