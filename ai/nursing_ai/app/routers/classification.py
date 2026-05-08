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
    surgery_name: Optional[str] = Field(None, description="수술명 (예: COLECTOMY)")
    disease_name: Optional[str] = Field(None, description="진단/병명")
    chief_complaint: Optional[str] = Field(None, description="입원 시 주증상")
    age: Optional[int] = Field(None, description="환자 나이 (만)")
    gender: Optional[str] = Field(None, description="성별 (MALE/FEMALE 등)")
    pod_days: Optional[int] = Field(None, description="수술 후 경과일 (POD). 음수면 수술 전")


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
    description="LLM(Claude Haiku)로 환자 발화의 priority(CRITICAL/HIGH/MEDIUM/LOW)를 산출. 수술 후 환자 컨텍스트(나이/수술/POD/진단/주증상)를 반영.",
)
async def classify_symptom(req: ClassificationRequest) -> ClassificationResponse:
    try:
        result = _get_classifier().classify(
            symptom_text=req.symptom_text,
            department_code=req.department_code,
            surgery_name=req.surgery_name,
            disease_name=req.disease_name,
            chief_complaint=req.chief_complaint,
            age=req.age,
            gender=req.gender,
            pod_days=req.pod_days,
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
