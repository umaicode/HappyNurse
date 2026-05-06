import os
import uuid
from datetime import datetime

class AudioStorage:
    def __init__(self, base_path: str = "uploads/audio"):
        self.base_path = base_path
        os.makedirs(self.base_path, exist_ok=True)

    def save(self, audio_data: bytes, original_filename: str) -> str:
        """음성 파일 저장 후 경로 반환"""
        # 고유 파일명 생성: 날짜_UUID.wav
        date_str = datetime.now().strftime("%Y%m%d")
        file_id = uuid.uuid4().hex[:8]
        ext = original_filename.rsplit(".", 1)[-1] if "." in original_filename else "wav"
        filename = f"{date_str}_{file_id}.{ext}"

        filepath = os.path.join(self.base_path, filename)

        with open(filepath, "wb") as f:
            f.write(audio_data)

        print(f"음성 파일 저장: {filepath} ({len(audio_data)} bytes)")
        return filepath
    