import os
import jwt
from dotenv import load_dotenv
from fastapi import HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import base64

load_dotenv()

JWT_SECRET_BASE64 = os.getenv("JWT_SECRET_KEY")
JWT_SECRET = base64.b64decode(JWT_SECRET_BASE64)  # Base64 디코딩
JWT_ALGORITHM = "HS256"

security = HTTPBearer()

def verify_token(token: str) -> dict:
    """JWT 토큰 검증 후 payload 반환"""
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        return {
            "practitioner_id": int(payload.get("sub")),
            "employee_number": payload.get("employeeNumber"),
            "name": payload.get("name"),
            "role": payload.get("role"),
            "session_id": payload.get("sessionId"),
            "organization_id": payload.get("organizationId"),
            "ward_id": payload.get("wardId"),
        }
    # except jwt.ExpiredSignatureError:
    #     raise HTTPException(status_code=401, detail="토큰이 만료되었습니다")
    # except jwt.InvalidTokenError:
    #     raise HTTPException(status_code=401, detail="유효하지 않은 토큰입니다")

    except jwt.ExpiredSignatureError:
      print("만료됨")
    except jwt.InvalidSignatureError:
      print("서명 불일치 ← secret 다를 때")
    except jwt.DecodeError as e:
      print(f"디코드 실패: {e}")

async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security)
) -> dict:
    """요청에서 JWT 토큰을 추출하고 검증"""
    return verify_token(credentials.credentials)