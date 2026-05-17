"""환자 음성 호출용 STT 엔드포인트.

기존 /api/stt/recognize (간호사 차팅용) 와 달리:
- 의료용어 매핑(Kiwi + TermMapper) 미실행 — 환자 발화엔 잘못된 보정 위험
- nursing_record DB INSERT 미실행 — 간호기록 오염 방지
- 응답에 차팅용 필드 제외 — 단순 {text, stt_confidence, nc_latency_ms}

흐름: 환자 웹 마이크 → audio Blob → 본 엔드포인트 → text → FE 가 textarea 채움 →
사용자 검토 후 기존 /patients/{id}/symptoms 자가보고 흐름 합류 (Phase A/B 정제까지 자동 적용).
"""
from __future__ import annotations

import logging
from typing import Optional

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from pydantic import BaseModel, Field

from app.middleware.jwt_auth import get_current_user
from app.services.nursing_stt.stt_pipeline import get_stt_pipeline

logger = logging.getLogger(__name__)

router = APIRouter()

MAX_AUDIO_BYTES = 10 * 1024 * 1024  # 10 MB


class PatientSTTResponse(BaseModel):
    text: str = Field(..., description="STT 변환된 텍스트")
    stt_confidence: Optional[float] = Field(None, description="CLOVA STT 신뢰도 (0.0~1.0)")
    nc_latency_ms: Optional[float] = Field(None, description="노이즈 캔슬링 처리 시간 (ms)")


@router.post(
    "/stt/patient",
    response_model=PatientSTTResponse,
    summary="환자 음성 호출 STT",
    description=(
        "환자가 마이크로 녹음한 음성을 CLOVA Speech + 노이즈 캔슬링으로 텍스트 변환. "
        "의료용어 매핑·DB INSERT 미실행. 환자 JWT 토큰 (role=PATIENT) 만 허용."
    ),
)
async def recognize_patient_voice(
    file: UploadFile = File(..., description="음성 파일 (webm/m4a/mp3/wav, 최대 10MB)"),
    current_user: dict = Depends(get_current_user),
) -> PatientSTTResponse:
    # 환자 토큰만 허용 — 간호사 토큰은 /api/stt/recognize 사용
    if current_user.get("role") != "PATIENT":
        raise HTTPException(status_code=403, detail="환자 인증 토큰이 필요합니다.")

    audio_bytes = await file.read()
    if not audio_bytes:
        raise HTTPException(status_code=400, detail="empty audio")
    if len(audio_bytes) > MAX_AUDIO_BYTES:
        raise HTTPException(status_code=413, detail="audio too large")

    try:
        pipeline = get_stt_pipeline()
        result = await pipeline.clova.recognize(
            audio_bytes,
            filename=file.filename or "patient.webm",
            apply_nc=True,
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("[STT-patient] recognize 실패")
        raise HTTPException(status_code=502, detail="STT 처리 중 오류") from e

    return PatientSTTResponse(
        text=result.get("text", ""),
        stt_confidence=result.get("confidence"),
        nc_latency_ms=result.get("nc_latency_ms"),
    )
