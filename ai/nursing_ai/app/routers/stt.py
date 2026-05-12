from fastapi import APIRouter, UploadFile, File, HTTPException, Depends, Query
from app.services.nursing_stt.stt_pipeline import get_stt_pipeline
from app.services.audio_storage import AudioStorage
from app.database.db import get_db
from app.middleware.jwt_auth import get_current_user
from sqlalchemy.orm import Session
from sqlalchemy import text
from typing import Optional
import json

router = APIRouter()
audio_storage = AudioStorage()

async def _run_recognize(
    audio: UploadFile,
    patient_id: Optional[int],
    encounter_id: Optional[int],
    current_user: dict,
    db: Session,
    apply_nc: bool,
):
    try:
        # 토큰에서 간호사 정보 자동 추출
        practitioner_id = current_user["practitioner_id"]
        print(f"\n인증된 사용자: {current_user['name']} (ID: {practitioner_id})")
        print(f"파일 수신: {audio.filename}, 타입: {audio.content_type}, NC={'on' if apply_nc else 'off'}")

        audio_data = await audio.read()
        print(f"음성 데이터 크기: {len(audio_data)} bytes")

        # 1. 음성 파일 저장 (원본 그대로 저장 — NC 적용본은 디스크에 남기지 않음)
        audio_path = audio_storage.save(audio_data, audio.filename)

        # 2. STT + 매핑 처리 (싱글톤 재사용 — Kiwi/DB 사전 init 비용 회피)
        pipeline = get_stt_pipeline()
        result = await pipeline.process(audio_data, audio.filename, apply_nc=apply_nc)

        # 3. nursing_record DB 저장 (환자 정보가 있을 때만)
        nursing_record_id = None
        if patient_id and encounter_id:
            editor_state = json.dumps(result["corrections"], ensure_ascii=False)

            row = db.execute(text("""
                INSERT INTO nursing_record
                (patient_id, encounter_id, author_practitioner_id,
                 status, audio_file_url, original_stt_content,
                 edit_content, created_at, updated_at)
                VALUES (:pid, :eid, :prid,
                        'draft', :audio_url, :original,
                        :corrected, NOW(), NOW())
                RETURNING nursing_record_id
            """), {
                "pid": patient_id,
                "eid": encounter_id,
                "prid": practitioner_id,
                "audio_url": audio_path,
                "original": result["original_text"],
                "corrected": result["corrected_text"]
            })
            nursing_record_id = row.fetchone()[0]
            db.commit()

        return {
            "success": True,
            "nursing_record_id": nursing_record_id,
            "audio_url": audio_path,
            "original_text": result["original_text"],
            "corrected_text": result["corrected_text"],
            "corrections": result["corrections"],
            "stt_confidence": result.get("stt_confidence"),
            "stt_segments": result.get("stt_segments", []),
            "nc_latency_ms": result.get("nc_latency_ms"),
        }

    except HTTPException:
        raise
    except Exception as e:
        db.rollback()
        print(f" stt.py  에러 발생: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post(
    "/stt/recognize",
    summary="음성 파일 STT 변환 (노이즈 캔슬링 미적용)",
    description="""
음성 파일을 업로드하면 CLOVA Speech로 텍스트 변환 후,
의료 용어 매핑 엔진을 통해 오인식 단어를 자동 교정합니다.

### 처리 흐름
1. 음성 파일 저장 (uploads/audio/)
2. CLOVA Speech STT 변환
3. Kiwi 형태소 분석으로 의료 용어 후보 추출
4. 매핑 사전에서 정확 매칭 → 실패 시 퍼지 매칭
5. nursing_record DB 저장 (patient_id, encounter_id 제공 시)

### 호출 모드
- 임상 간호기록: patient_id + encounter_id 모두 전달 → nursing_record 자동 저장
- STT 음성 메모 알람(타이머): patient_id, encounter_id 미전달 → corrected_text 만 응답 (DB 저장 스킵)

### 지원 포맷
wav, m4a, mp3, webm 등 (자동 wav 변환)

### 응답 예시
```json
{
    "success": true,
    "nursing_record_id": 1,
    "audio_url": "uploads/audio/20260505_abc123.wav",
    "original_text": "사무실 박환자 혈압 상승",
    "corrected_text": "3호실 박환자 혈압 상승",
    "corrections": [
        {
            "original": "사무실",
            "corrected": "3호실",
            "type": "exact",
            "confidence": 1.0
        }
    ]
}
```
    """,
    response_description="STT 변환 결과 및 교정 내역"
)
async def recognize_speech(
    audio: UploadFile = File(..., description="음성 파일 (wav, m4a, mp3, webm)"),
    patient_id: Optional[int] = Query(None, description="환자 ID (DB 저장 시 필수)"),
    encounter_id: Optional[int] = Query(None, description="입원 ID (DB 저장 시 필수)"),
    current_user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return await _run_recognize(audio, patient_id, encounter_id, current_user, db, apply_nc=False)


@router.post(
    "/stt/recognize-nc",
    summary="음성 파일 STT 변환 (노이즈 캔슬링 적용)",
    description="""
`/stt/recognize` 와 동일한 처리 흐름이지만, CLOVA Speech 호출 직전에
병원 환경 노이즈(발걸음·문 소리·모니터 알람 등)를 억제하는 노이즈 캔슬링을 적용합니다.

### 용도
같은 음성 파일을 본 엔드포인트와 `/stt/recognize` 양쪽에 보내 결과를 비교하면
NC가 인식률·교정 결과에 미치는 영향을 가늠할 수 있습니다 (A/B 비교용).

### 주의
- 응답 스키마는 `/stt/recognize` 와 동일합니다.
- 디스크에는 NC 적용 전 원본만 저장됩니다 (CLOVA 로는 정제본 전송).
- NC 모드는 `STT_NOISE_CANCEL` 환경변수로 제어합니다 (`dsp` 기본 / `off` / `ml`).
    """,
    response_description="STT 변환 결과 및 교정 내역 (NC 적용)"
)
async def recognize_speech_with_nc(
    audio: UploadFile = File(..., description="음성 파일 (wav, m4a, mp3, webm)"),
    patient_id: Optional[int] = Query(None, description="환자 ID (DB 저장 시 필수)"),
    encounter_id: Optional[int] = Query(None, description="입원 ID (DB 저장 시 필수)"),
    current_user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return await _run_recognize(audio, patient_id, encounter_id, current_user, db, apply_nc=True)