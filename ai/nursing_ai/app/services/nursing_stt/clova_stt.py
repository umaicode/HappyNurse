import httpx
import json
import os
import io
import numpy as np
from dotenv import load_dotenv
from pydub import AudioSegment

from app.services.nursing_stt.noise_cancel import get_noise_canceller

load_dotenv()

class ClovaSTTClient:
    def __init__(self):
        self.secret_key = os.getenv("CLOVA_SECRET_KEY")
        self.invoke_url = os.getenv("CLOVA_INVOKE_URL")
        self.noise_canceller = get_noise_canceller()
        print(f"Secret Key 확인: {self.secret_key[:10]}...")

    def convert_to_wav(self, audio_data: bytes, filename: str) -> bytes:
        """다양한 오디오 포맷을 WAV(16kHz, mono)로 변환. NC 미적용."""
        ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else "wav"

        if ext == "wav":
            return audio_data

        try:
            # m4a, mp3, webm, ogg 등 → wav 변환
            audio = AudioSegment.from_file(io.BytesIO(audio_data), format=ext)
            audio = audio.set_frame_rate(16000).set_channels(1).set_sample_width(2)

            wav_buffer = io.BytesIO()
            audio.export(wav_buffer, format="wav")
            wav_data = wav_buffer.getvalue()

            print(f"오디오 변환: {ext} → wav ({len(audio_data)} → {len(wav_data)} bytes)")
            return wav_data
        except Exception as e:
            print(f"오디오 변환 실패: {e}, 원본 그대로 전송")
            return audio_data

    def convert_to_wav_with_nc(self, audio_data: bytes, filename: str) -> bytes:
        """모든 입력을 16kHz/mono/PCM16으로 정규화 후 노이즈 캔슬링 적용."""
        ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else "wav"

        try:
            audio = AudioSegment.from_file(io.BytesIO(audio_data), format=ext)
            audio = audio.set_frame_rate(16000).set_channels(1).set_sample_width(2)

            samples = np.array(audio.get_array_of_samples(), dtype=np.int16)
            pcm = samples.astype(np.float32) / 32768.0

            pcm = self.noise_canceller.apply(pcm, sr=16000)

            cleaned_int16 = np.clip(pcm * 32768.0, -32768, 32767).astype(np.int16)
            cleaned = AudioSegment(
                cleaned_int16.tobytes(),
                frame_rate=16000,
                sample_width=2,
                channels=1,
            )

            wav_buffer = io.BytesIO()
            cleaned.export(wav_buffer, format="wav")
            wav_data = wav_buffer.getvalue()

            print(
                f"오디오 변환·정제: {ext} → wav "
                f"({len(audio_data)} → {len(wav_data)} bytes, NC={self.noise_canceller.name})"
            )
            return wav_data
        except Exception as e:
            print(f"오디오 변환·정제 실패: {e}, NC 없는 fallback 변환 사용")
            return self.convert_to_wav(audio_data, filename)

    async def recognize(self, audio_data: bytes, filename: str = "audio.wav", apply_nc: bool = False) -> str:
        headers = {
            "X-CLOVASPEECH-API-KEY": self.secret_key
        }

        # WAV로 변환 (apply_nc=True 면 NC 적용 경로 사용)
        wav_data = (
            self.convert_to_wav_with_nc(audio_data, filename)
            if apply_nc
            else self.convert_to_wav(audio_data, filename)
        )

        params = {
            "language": "ko-KR",
            "completion": "sync",
        }

        files = {
            "media": ("audio.wav", wav_data, "audio/wav"),
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