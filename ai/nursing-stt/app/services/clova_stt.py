import httpx
import os
from dotenv import load_dotenv

load_dotenv()

class ClovaSTTClient:
    def __init__(self):
        self.secret_key = os.getenv("CLOVA_SECRET_KEY")
        self.invoke_url = os.getenv("CLOVA_INVOKE_URL")

    async def recognize(self, audio_data: bytes, filename: str = "audio.wav") -> str:
        headers = {
            "X-CLOVASPEECH-API-KEY": self.secret_key
        }

        # 장문 인식 요청 설정
        params = {
            "language": "ko-KR",
            "completion": "sync",  # 동기 방식 (결과 바로 반환)
        }

        files = {
            "media": (filename, audio_data, "audio/wav"),
            "params": (None, str(params).replace("'", '"'), "application/json")
        }

        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(
                f"{self.invoke_url}/recognizer/upload",
                headers=headers,
                files=files
            )

        print(f"응답 상태 코드: {response.status_code}")
        print(f"응답 원본: {response.text}")

        if response.status_code == 200:
            result = response.json()
            # 인식된 텍스트 추출
            text = result.get("text", "")
            return text
        else:
            raise Exception(f"클로바 API 에러: {response.status_code} {response.text}")
        