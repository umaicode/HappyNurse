import os
import base64
import jwt
from dotenv import load_dotenv
from fastapi import Cookie, Header, HTTPException

load_dotenv()

JWT_SECRET_BASE64 = os.getenv("JWT_SECRET_KEY")
JWT_SECRET = base64.b64decode(JWT_SECRET_BASE64)
JWT_ALGORITHM = "HS256"
COOKIE_NAME = "ACCESS_TOKEN"


def verify_token(token: str) -> dict:
    """JWT 토큰 검증 후 payload 반환"""
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="토큰이 만료되었습니다")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="유효하지 않은 토큰입니다")

    return {
        "practitioner_id": int(payload.get("sub")),
        "employee_number": payload.get("employeeNumber"),
        "name": payload.get("name"),
        "role": payload.get("role"),
        "session_id": payload.get("sessionId"),
        "organization_id": payload.get("organizationId"),
        "ward_id": payload.get("wardId"),
    }


async def get_current_user(
    access_token: str | None = Cookie(default=None, alias=COOKIE_NAME),
    authorization: str | None = Header(default=None),
) -> dict:
    """쿠키(ACCESS_TOKEN) 우선, Authorization 헤더 fallback으로 JWT 추출 후 검증"""
    token: str | None = None
    if authorization and authorization.lower().startswith("bearer "):
        token = authorization.split(" ", 1)[1].strip()
    elif access_token:
        token = access_token

    if not token:
        raise HTTPException(status_code=401, detail="인증 토큰이 없습니다")
    return verify_token(token)
