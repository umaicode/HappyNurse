from fastapi import APIRouter, UploadFile, File, HTTPException, Depends
from app.services.stt_pipeline import STTPipeline
from app.database.db import get_db
from sqlalchemy.orm import Session

router = APIRouter()

@router.post("/stt/recognize")
async def recognize_speech(
    audio: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    try:
        print(f"\n파일 수신: {audio.filename}, 타입: {audio.content_type}")

        audio_data = await audio.read()
        print(f"음성 데이터 크기: {len(audio_data)} bytes")

        pipeline = STTPipeline(db=db)
        result = await pipeline.process(audio_data, audio.filename)

        return {
            "success": True,
            "original_text": result["original_text"],
            "corrected_text": result["corrected_text"],
            "corrections": result["corrections"]
        }

    except Exception as e:
        print(f"에러 발생: {e}")
        raise HTTPException(status_code=500, detail=str(e))