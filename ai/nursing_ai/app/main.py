import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from dotenv import load_dotenv
from app.routers import stt, correction, classification

load_dotenv()

app = FastAPI(
    title="HappyNurse AI API",
    root_path=os.getenv("ROOT_PATH", ""), 
    description="""
## 간호사 음성 인식 및 의료 용어 매핑 API

### 주요 기능
- **STT (Speech-to-Text)**: 간호사 음성을 텍스트로 변환 (CLOVA Speech)
- **의료 용어 매핑**: STT 오인식 단어를 정식 의료 용어로 자동 교정
- **피드백 루프**: 간호사 수정 이력을 학습하여 매핑 정확도 향상

### 인증
- Spring Boot에서 발급한 JWT 토큰을 `Authorization: Bearer {token}` 헤더에 포함
- 오른쪽 위 **Authorize** 버튼을 클릭하여 토큰 입력
    """,
    version="1.0.0",
    swagger_ui_parameters={"persistAuthorization": True},
    openapi_tags=[
        {"name": "STT 음성인식", "description": "음성 파일 → 텍스트 변환 + 의료 용어 매핑"},
        {"name": "용어 교정 피드백", "description": "교정 이력 저장 및 매핑 사전 관리"},
        {"name": "중요도 분류", "description": "환자 발화 텍스트 → priority(LLM 분류)"},
        {"name": "서버 상태", "description": "서버 상태 확인"},
    ]
)

# CORS 설정 추가
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 개발 단계에서는 전체 허용
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(stt.router, prefix="/api", tags=["STT 음성인식"])
app.include_router(correction.router, prefix="/api", tags=["용어 교정 피드백"])
app.include_router(classification.router, prefix="/api", tags=["중요도 분류"])

@app.get("/", tags=["서버 상태"])
def root():
    """서버 상태 확인용 API"""
    return {"message": "Nursing STT Server Running"}