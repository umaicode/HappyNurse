from fastapi import APIRouter, HTTPException, Depends, Query
from sqlalchemy.orm import Session
from sqlalchemy import text
from pydantic import BaseModel, Field
from typing import Optional
from app.database.db import get_db
from app.middleware.jwt_auth import get_current_user

router = APIRouter()

# === 요청/응답 모델 ===

class CorrectionRequest(BaseModel):
    nursing_record_id: int = Field(..., description="간호 기록 ID")
    original_word: str = Field(..., description="교정 전 원본 단어", example="사무실")
    replaced_word: str = Field(..., description="교정 후 단어", example="3호실")
    correction_type: str = Field("manual", description="교정 방식 (exact/fuzzy/manual)")
    suggestion_id: Optional[int] = Field(None, description="선택한 후보 ID (없으면 자동 생성)")

class DictionaryApproveRequest(BaseModel):
    stt_word: str = Field(..., description="STT 오인식 단어", example="해프트리 압손")
    correct_word: str = Field(..., description="정식 용어", example="세프트리악손")
    category: str = Field("other", description="카테고리 (medication/symptom/body_part/procedure/vital/other)")



# === 1. 수정 이력 저장 ===

@router.post(
    "/correction/apply",
    summary="용어 교정 이력 저장",
    description="""
간호사가 STT 결과에서 단어를 수정했을 때 호출합니다.

### 처리 흐름
1. 수정 이력을 nursing_record_correction_applied 테이블에 저장
2. 해당 매핑의 usage_count 증가
3. 같은 수정이 5회 이상 반복되면 suggest_dictionary: true 반환

### 응답 예시
```json
{
    "success": true,
    "message": "수정 이력 저장 완료",
    "repeat_count": 5,
    "suggest_dictionary": true
}
```
    """
)
async def apply_correction(
    req: CorrectionRequest,
    current_user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):

    practitioner_id = current_user["practitioner_id"]

    """간호사가 단어를 수정했을 때 수정 이력 저장"""
    try:
        # suggestion_id가 없으면 임시로 생성
        if not req.suggestion_id:
            # 기존 dictionary에서 찾기
            dict_row = db.execute(text("""
                SELECT correction_id FROM quick_correction_dictionary
                WHERE correct_word = :word AND is_active = true
                LIMIT 1
            """), {"word": req.replaced_word}).fetchone()

            if dict_row:
                correction_id = dict_row[0]
            else:
                # dictionary에 없으면 새로 추가
                result = db.execute(text("""
                    INSERT INTO quick_correction_dictionary 
                    (stt_word, correct_word, stt_word_normalized, category, 
                     usage_count, source, is_active, created_at, updated_at)
                    VALUES (:stt, :correct, :normalized, 'other', 
                            0, 'feedback', true, NOW(), NOW())
                    RETURNING correction_id
                """), {
                    "stt": req.original_word,
                    "correct": req.replaced_word,
                    "normalized": req.original_word.replace(" ", "").lower()
                })
                correction_id = result.fetchone()[0]

            # suggestion 생성
            result = db.execute(text("""
                INSERT INTO quick_correction_suggestion
                (correction_id, suggested_word, is_active, created_at)
                VALUES (:cid, :word, true, NOW())
                RETURNING suggestion_id
            """), {
                "cid": correction_id,
                "word": req.replaced_word
            })
            suggestion_id = result.fetchone()[0]
        else:
            suggestion_id = req.suggestion_id

        # 수정 이력 저장
        db.execute(text("""
            INSERT INTO nursing_record_correction_applied
            (nursing_record_id, suggestion_id, practitioner_id,
             original_word, replaced_word, correction_type,
             promoted_to_dictionary, applied_at)
            VALUES (:nrid, :sid, :pid, :orig, :repl, :ctype, false, NOW())
        """), {
            "nrid": req.nursing_record_id,
            "sid": suggestion_id,
            "pid": req.practitioner_id,
            "orig": req.original_word,
            "repl": req.replaced_word,
            "ctype": req.correction_type
        })

        # dictionary의 usage_count 증가
        db.execute(text("""
            UPDATE quick_correction_dictionary
            SET usage_count = usage_count + 1, updated_at = NOW()
            WHERE correction_id IN (
                SELECT correction_id FROM quick_correction_suggestion
                WHERE suggestion_id = :sid
            )
        """), {"sid": suggestion_id})

        db.commit()

        # 같은 수정 반복 횟수 확인
        count_row = db.execute(text("""
            SELECT COUNT(*) FROM nursing_record_correction_applied
            WHERE original_word = :orig AND replaced_word = :repl
            AND promoted_to_dictionary = false
        """), {"orig": req.original_word, "repl": req.replaced_word}).fetchone()

        repeat_count = count_row[0]

        return {
            "success": True,
            "message": "수정 이력 저장 완료",
            "repeat_count": repeat_count,
            "suggest_dictionary": repeat_count >= 5
        }

    except Exception as e:
        db.rollback()
        print(f"에러: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# === 2. 반복 수정 목록 조회 (관리자용) ===

@router.get(
    "/correction/frequent",
    summary="반복 교정 목록 조회 (관리자용)",
    description="""
같은 교정이 N회 이상 반복된 단어 목록을 조회합니다.
관리자(admin) 또는 수간호사(head_nurse)만 접근 가능합니다.

매핑 사전에 등록할 후보를 확인하는 용도입니다.
    """
)

async def get_frequent_corrections(
    min_count: int = 5,
    current_user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """같은 수정이 N회 이상 반복된 목록 조회"""

    # 관리지만 접근 허용 코드
    # if current_user["role"] not in ["admin", "head_nurse"]:
    #     raise HTTPException(status_code=403, detail="관리자 권한이 필요합니다")

    try:
        rows = db.execute(text("""
            SELECT original_word, replaced_word, COUNT(*) as repeat_count,
                   COUNT(DISTINCT practitioner_id) as unique_practitioners
            FROM nursing_record_correction_applied
            WHERE promoted_to_dictionary = false
            GROUP BY original_word, replaced_word
            HAVING COUNT(*) >= :min_count
            ORDER BY repeat_count DESC
        """), {"min_count": min_count}).fetchall()

        suggestions = []
        for row in rows:
            suggestions.append({
                "original_word": row[0],
                "replaced_word": row[1],
                "repeat_count": row[2],
                "unique_practitioners": row[3]
            })

        return {
            "success": True,
            "count": len(suggestions),
            "suggestions": suggestions
        }

    except Exception as e:
        print(f"에러: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# === 3. 사전 등록 승인 (관리자용) ===

@router.post(
    "/correction/approve",
    summary="매핑 사전 등록 승인 (관리자용)",
    description="""
반복된 교정을 매핑 사전에 공식 등록합니다.
관리자(admin) 또는 수간호사(head_nurse)만 접근 가능합니다.

등록 후 해당 오인식 패턴은 자동으로 교정됩니다.
    """
)

async def approve_to_dictionary(
    req: DictionaryApproveRequest,
    current_user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):

    # 관리자만 허용
    # if current_user["role"] not in ["admin", "head_nurse"]:
    #     raise HTTPException(status_code=403, detail="관리자 권한이 필요합니다")

    """관리자가 승인하면 매핑 사전에 추가"""
    try:
        # 이미 같은 매핑이 있는지 확인
        existing = db.execute(text("""
            SELECT correction_id FROM quick_correction_dictionary
            WHERE stt_word = :stt AND correct_word = :correct AND is_active = true
        """), {"stt": req.stt_word, "correct": req.correct_word}).fetchone()

        if existing:
            return {
                "success": False,
                "message": "이미 사전에 등록된 매핑입니다"
            }

        # 사전에 추가
        db.execute(text("""
            INSERT INTO quick_correction_dictionary
            (stt_word, correct_word, stt_word_normalized, category,
             usage_count, source, is_active, 
             created_by_practitioner_id, created_at, updated_at)
            VALUES (:stt, :correct, :normalized, :category,
                    0, 'admin', true, :pid, NOW(), NOW())
        """), {
            "stt": req.stt_word,
            "correct": req.correct_word,
            "normalized": req.stt_word.replace(" ", "").lower(),
            "category": req.category,
            "pid": req.approved_by_practitioner_id
        })

        # 관련 수정 이력 promoted 처리
        db.execute(text("""
            UPDATE nursing_record_correction_applied
            SET promoted_to_dictionary = true
            WHERE original_word = :orig AND replaced_word = :repl
            AND promoted_to_dictionary = false
        """), {"orig": req.stt_word, "repl": req.correct_word})

        db.commit()

        return {
            "success": True,
            "message": f"'{req.stt_word}' → '{req.correct_word}' 사전 등록 완료"
        }

    except Exception as e:
        db.rollback()
        print(f"에러: {e}")
        raise HTTPException(status_code=500, detail=str(e))