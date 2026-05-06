# ─────────────────────────────────────────────
# Stage 1: Build (pip install)
# ─────────────────────────────────────────────
FROM python:3.12-slim AS builder


WORKDIR /app

# 빌드에 필요한 C 라이브러리 (kiwipiepy 등 일부 패키지가 C 확장 컴파일)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential gcc g++ && \
    rm -rf /var/lib/apt/lists/*

# pip upgrade + wheel로 설치
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install --user --no-cache-dir -r requirements.txt

# ─────────────────────────────────────────────
# Stage 2: Runtime (slim + installed packages)
# ─────────────────────────────────────────────
FROM python:3.12-slim

WORKDIR /app

# kiwipiepy 런타임 의존 라이브러리 (최소)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libgomp1 ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# non-root 유저
RUN groupadd -r fastapi && useradd -r -g fastapi -m -d /home/fastapi fastapi

# 빌더에서 설치한 패키지 복사
COPY --from=builder /root/.local /home/fastapi/.local

# 소스 복사
COPY --chown=fastapi:fastapi nursing-stt ./nursing-stt

USER fastapi
ENV PATH=/home/fastapi/.local/bin:$PATH
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

EXPOSE 8000

# nursing-stt/app/main.py 에 FastAPI 인스턴스 `app`이 있다고 가정
# 실제 엔트리가 다르면 이 명령 수정
WORKDIR /app/nursing-stt
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]