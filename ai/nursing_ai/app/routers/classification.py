"""환자 자가보고 텍스트의 중요도(priority)를 LLM으로 분류하는 라우터."""
from __future__ import annotations

import logging
from functools import lru_cache
from typing import Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.services.classification.llm_classifier import LlmClassifier

logger = logging.getLogger(__name__)

router = APIRouter()


class ClassificationRequest(BaseModel):
    symptom_text: str = Field(..., min_length=1, description="환자 발화 원문")
    department_code: Optional[str] = Field(None, description="진료부서 코드 (예: GS, OS)")
    surgery_type_code: Optional[str] = Field(None, description="수술 유형 코드")
    period_start: Optional[str] = Field(None, description="입원일 (YYYY-MM-DD)")


class ClassificationResponse(BaseModel):
    priority: str = Field(..., description="critical | high | medium | low")
    category: Optional[str] = Field(None, description="LLM이 산출한 카테고리 ID")
    confidence: Optional[float] = Field(None, description="0.00~1.00")


@lru_cache(maxsize=1)
def _get_classifier() -> LlmClassifier:
    return LlmClassifier()


@router.post(
    "/symptom/classify",
    response_model=ClassificationResponse,
    summary="환자 발화 중요도 분류",
    description="LLM(Claude Haiku)로 환자 발화의 priority(CRITICAL/HIGH/MEDIUM/LOW)를 산출.",
)
async def classify_symptom(req: ClassificationRequest) -> ClassificationResponse:
    try:
        result = _get_classifier().classify(
            symptom_text=req.symptom_text,
            department_code=req.department_code,
            surgery_type_code=req.surgery_type_code,
            period_start=req.period_start,
        )
    except RuntimeError as e:
        logger.error("Classification configuration error: %s", e)
        raise HTTPException(status_code=503, detail="classification service unavailable") from e
    except ValueError as e:
        logger.error("Classification response parsing failed: %s", e)
        raise HTTPException(status_code=502, detail="invalid LLM response") from e
    except Exception as e:
        logger.exception("Classification call failed")
        raise HTTPException(status_code=502, detail="classification call failed") from e

    return ClassificationResponse(**result)
