import httpx
import os
import json
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

        params = {
            "language": "ko-KR",
            "completion": "sync",
        }

        # params = {
        #     "language": "ko-KR",
        #     "completion": "sync",
        #     "boostings": [
        #         {
        #             "words": "아세트아미노펜,세프트리악손,푸로세미드,라식스,케토롤락,디클로페낙,이부프로펜,멜록시캄,인슐린,헤파린,반코마이신,닥터노티,프리메디,바이탈,트랜스퍼,배액량,드레싱,석션,NPO,IV,IM,SC,PRN",
        #             "weight": 3
        #         }
        #     ]
        # }

        files = {
            "media": (filename, audio_data, "audio/wav"),
            "params": (None, json.dumps(params), "application/json")
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
            text = result.get("text", "")
            return text
        else:
            raise Exception(f"클로바 API 에러: {response.status_code} {response.text}")
