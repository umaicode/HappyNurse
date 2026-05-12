import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from app.routers import stt, correction
from app.routers import handover
from app.routers import stt, correction, classification

load_dotenv()

app = FastAPI(
    title="HappyNurse AI API",
    root_path=os.getenv("ROOT_PATH", ""), 
    description="""
## 해피너스 AI API

### 주요 기능
- **STT (Speech-to-Text)**: 간호사 음성을 텍스트로 변환 (CLOVA Speech)
- **의료 용어 매핑**: STT 오인식 단어를 정식 의료 용어로 자동 교정
- **피드백 루프**: 간호사 수정 이력을 학습하여 매핑 정확도 향상

### 인증
- **운영(브라우저)**: Spring Boot 로그인 시 발급되는 HttpOnly 쿠키 `ACCESS_TOKEN`이 자동 전송됨
- **Swagger/Postman 테스트**: 우상단 **Authorize** 버튼에 JWT를 입력하면 `Authorization: Bearer {token}` 헤더로 호출 가능
    """,
    version="1.0.0",
    swagger_ui_parameters={"persistAuthorization": True},
    openapi_tags=[
        {"name": "STT 음성인식", "description": "음성 파일 → 텍스트 변환 + 의료 용어 매핑"},
        {"name": "용어 교정 피드백", "description": "교정 이력 저장 및 매핑 사전 관리"},
        {"name": "중요도 분류", "description": "환자 발화 텍스트 → priority(LLM 분류)"},
        {"name": "서버 상태", "description": "서버 상태 확인"},
        {"name": "AI 인수인계", "description": "PASS-BAR 인수인계 리포트 생성 및 조회"},
    ]
)

# CORS 설정 추가
# HttpOnly 쿠키(ACCESS_TOKEN)를 동봉하므로 allow_credentials=True 사용 →
# 브라우저 스펙상 allow_origins="*" 와 함께 쓸 수 없어 명시적 origin 화이트리스트 필요.
cors_origins = [
    o.strip()
    for o in os.getenv(
        "CORS_ALLOWED_ORIGINS",
        "http://localhost:5173,"
        "http://localhost:3000,"
        "https://k14e101.p.ssafy.io",
    ).split(",")
    if o.strip()
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(stt.router, prefix="/api", tags=["STT 음성인식"])
app.include_router(correction.router, prefix="/api", tags=["용어 교정 피드백"])
app.include_router(handover.router, prefix="/api/handover", tags=["AI 인수인계"])


@app.on_event("startup")
async def _startup_stt_pipeline():
    """STT 파이프라인 pre-warm — Kiwi 형태소 분석기와 DB 매핑 사전을 부팅 시 1회 로드.

    이렇게 안 하면 매 /api/stt/recognize 요청마다 1-3초의 init 비용이 추가됨
    (Kiwi 한국어 사전 로드 + quick_correction_dictionary 풀 SELECT).
    """
    from app.services.nursing_stt.stt_pipeline import get_stt_pipeline
    from app.database.db import SessionLocal

    pipeline = get_stt_pipeline()
    db = SessionLocal()
    try:
        pipeline.load_dictionary(db)
    finally:
        db.close()
    print("[startup] STT 파이프라인 pre-warm 완료")


@app.on_event("shutdown")
async def _shutdown_stt_pipeline():
    """앱 종료 시 httpx 연결 풀 정리."""
    from app.services.nursing_stt.stt_pipeline import _PIPELINE

    if _PIPELINE is not None:
        await _PIPELINE.clova.aclose()


@app.on_event("startup")                                     # 추가
async def _startup_handover():
    from app.services.handover.config import get_settings
    from app.database.async_db import make_session_factory
    from app.services.handover.lexicon.store import LexiconStore
    from app.services.handover.rules.engine import RuleEngine
    from app.services.handover.clients.llm_client import LLMClient
    from app.services.handover.db.persister import Persister
    from app.services.handover.coordination.roster import RosterService
    from app.services.handover.clinical.record_loader import RecordLoader
    from app.services.handover.coordination.job_coordinator import JobCoordinator
    from app.services.handover.db.freshness import FreshnessRepo
    from app.services.handover.db.repositories import DBRecordRepository, DBRosterRepository

    settings = get_settings()
    sf = make_session_factory(settings)

    record_repo = DBRecordRepository(sf)
    roster_repo = DBRosterRepository(sf)

    lexicon        = LexiconStore(settings.lexicon_dir)
    rule_engine    = RuleEngine(settings.rules_dir)
    llm            = LLMClient(settings)
    persister      = Persister()
    roster_service = RosterService(repo=roster_repo)
    record_loader  = RecordLoader(repo=record_repo)
    job_coordinator = JobCoordinator()
    freshness_repo = FreshnessRepo(session_factory=sf, record_repo=record_repo)

    settings_meta = {
        "model":           settings.llm_model,
        "lexicon_version": settings.lexicon_version,
        "rule_set_version": settings.rule_set_version,
    }

    handover.configure(
        pipeline=None,
        roster_service=roster_service,
        session_factory=sf,
        record_loader=record_loader,
        rule_engine=rule_engine,
        lexicon=lexicon,
        llm=llm,
        persister=persister,
        settings_meta=settings_meta,
        job_coordinator=job_coordinator,
        freshness_repo=freshness_repo,
    )
app.include_router(classification.router, prefix="/api", tags=["중요도 분류"])

@app.get("/", tags=["서버 상태"])
def root():
    """서버 상태 확인용 API"""
    return {"message": "Nursing STT Server Running"}