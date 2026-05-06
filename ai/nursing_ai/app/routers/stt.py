from fastapi import APIRouter, UploadFile, File, HTTPException, Depends
from app.services.nursing_stt.stt_pipeline import STTPipeline
from app.services.audio_storage import AudioStorage
from app.database.db import get_db
from sqlalchemy.orm import Session
from sqlalchemy import text
from pydantic import BaseModel
from typing import Optional
import json

router = APIRouter()
audio_storage = AudioStorage()

class RecognizeRequest(BaseModel):
    patient_id: Optional[int] = None
    encounter_id: Optional[int] = None
    practitioner_id: Optional[int] = None

@router.post("/stt/recognize")
async def recognize_speech(
    audio: UploadFile = File(...),
    patient_id: Optional[int] = None,
    encounter_id: Optional[int] = None,
    practitioner_id: Optional[int] = None,
    db: Session = Depends(get_db)
):
    try:
        print(f"\n파일 수신: {audio.filename}, 타입: {audio.content_type}")

        audio_data = await audio.read()
        print(f"음성 데이터 크기: {len(audio_data)} bytes")

        # 1. 음성 파일 저장
        audio_path = audio_storage.save(audio_data, audio.filename)

        # 2. STT + 매핑 처리
        pipeline = STTPipeline(db=db)
        result = await pipeline.process(audio_data, audio.filename)

        # 3. nursing_record DB 저장 (환자 정보가 있을 때만)
        nursing_record_id = None
        if patient_id and encounter_id and practitioner_id:
            editor_state = json.dumps(result["corrections"], ensure_ascii=False)

            row = db.execute(text("""
                INSERT INTO nursing_record
                (patient_id, encounter_id, author_practitioner_id,
                 status, audio_file_url, original_stt_content,
                 editor_state_json, edit_content,
                 created_at, updated_at)
                VALUES (:pid, :eid, :prid,
                        'draft', :audio_url, :original,
                        :editor_state, :corrected,
                        NOW(), NOW())
                RETURNING nursing_record_id
            """), {
                "pid": patient_id,
                "eid": encounter_id,
                "prid": practitioner_id,
                "audio_url": audio_path,
                "original": result["original_text"],
                "editor_state": editor_state,
                "corrected": result["corrected_text"]
            })
            nursing_record_id = row.fetchone()[0]
            db.commit()
            print(f"nursing_record 저장: ID {nursing_record_id}")

        return {
            "success": True,
            "nursing_record_id": nursing_record_id,
            "audio_url": audio_path,
            "original_text": result["original_text"],
            "corrected_text": result["corrected_text"],
            "corrections": result["corrections"]
        }

    except Exception as e:
        db.rollback()
        print(f"에러 발생: {e}")
        raise HTTPException(status_code=500, detail=str(e))
