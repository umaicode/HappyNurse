# STT 알람 등록 지연 단축 — STTPipeline 싱글톤화

> 작업일: 2026-05-12
> 범위: AI 서버 (`ai/nursing_ai`)
> 관련 PR/브랜치: `ai/feat-stt-timer`

## 1. 문제 — 사용자 입장에서 뭐가 느렸나

워치(`frontend/mobile/wear`)에서 음성 메모 알람을 등록하는 흐름:

```
[녹음 종료]
   ↓
① POST /api/stt/recognize          ← AI 서버 (가장 느림)
   ↓
② POST /reminders/stt/preview      ← Java 백엔드 (빠름)
   ↓
[사용자 확인]
   ↓
③ POST /reminders/stt              ← Java 백엔드 create (빠름)
```

체감 지연의 거의 전부가 ① 단계. ②③ 는 합쳐도 100ms 안쪽.

## 2. 진짜 병목 — 매 요청마다 인스턴스 throwaway

`app/routers/stt.py` 의 `_run_recognize` 안:

```python
# 매 HTTP 요청마다 이 줄이 실행됨
pipeline = STTPipeline(db=db)
result = await pipeline.process(audio_data, audio.filename, apply_nc=apply_nc)
```

그리고 `STTPipeline.__init__`:

```python
class STTPipeline:
    def __init__(self, db=None):
        self.clova = ClovaSTTClient()        # 가벼움
        self.morpheme = MorphemeAnalyzer()   # Kiwi() — 한국어 사전 로드
        self.mapper = TermMapper(db=db)      # DB SELECT 3번
```

| 컴포넌트 | 요청당 비용 |
|---|---|
| `ClovaSTTClient()` | 가벼움 |
| `MorphemeAnalyzer()` → `Kiwi()` | **1-3초** (한국어 형태소 사전 로드) |
| `TermMapper(db=db)` → `_load_from_db()` | ~200-500ms (SQL 3번) |
| `httpx.AsyncClient` (CLOVA 호출용) | ~100-300ms (TLS handshake + 연결 풀) |
| **CLOVA API 호출 자체** | 1-5초 (외부 서비스, 불가피) |

이전 세션 로그에 매 요청마다 다음 줄이 반복적으로 찍히는 게 증거:

```
[NC] 노이즈 캔슬링 모드: dsp
Secret Key 확인: 9665c87e7e...
Kiwi 형태소 분석기 초기화 완료
DB에서 매핑 사전 로드 완료
매핑 사전 초기화: 정확 매칭 412개, 퍼지 매칭 대상 250개
STT 파이프라인 초기화 완료
```

## 3. 흔한 오해 — "의료 용어 매핑이 느린가?"

**아님**. 매핑 로직 자체 (`term_mapper.process_text`, `fuzzy_match`) 의 실행 시간은 < 10ms.

느린 건 매핑 *로직* 이 아니라, **매핑 사전을 들고 있는 객체의 init 비용**. 즉:
- TermMapper 안의 inmemory dict 구조 자체는 잘 짜여 있음 (init 1번 → 메모리 dict → 이후 read-only).
- 문제는 그 객체를 **매 HTTP 요청마다 throwaway 하고 새로 만든다는 점**.
- 따라서 inmemory 캐시 효과가 매번 0 으로 리셋됨.

Kiwi 도 동일 — `Kiwi()` 자체는 thread-safe 하고 재사용 가능하게 설계됐는데, 매번 새로 인스턴스화하면서 사전 로드 1-3초 비용을 매번 지불.

## 4. 해결 — STTPipeline 싱글톤 + 부팅 시 pre-warm

### 4.1. `STTPipeline` 모듈 레벨 싱글톤

`app/services/nursing_stt/stt_pipeline.py`:

```python
_PIPELINE: STTPipeline | None = None


def get_stt_pipeline() -> STTPipeline:
    """프로세스 라이프타임 싱글톤 액세서.

    첫 호출 시 STTPipeline() 을 1회 인스턴스화 (Kiwi 로드 ~1-3초, default 사전).
    DB 사전은 app 시작 시 main.py 가 load_dictionary() 로 채움.
    """
    global _PIPELINE
    if _PIPELINE is None:
        _PIPELINE = STTPipeline(db=None)
    return _PIPELINE
```

### 4.2. 사전 로드를 별도 메서드로 분리

`STTPipeline` 에 `load_dictionary(db)` 추가 — 부팅 시 1회만 호출, 운영 중 사전 갱신 필요할 때도 사용 가능:

```python
def load_dictionary(self, db: Session) -> None:
    """싱글톤 라이프타임 동안 한 번 호출 — TermMapper 의 사전을 DB 에서 reload.

    이미 default 사전으로 init 된 mapper 를 DB 사전으로 교체한다.
    Kiwi/ClovaSTTClient 는 재초기화 안 함 (비용 큰 컴포넌트 유지).
    """
    self.mapper = TermMapper(db=db)
```

> ⚠️ 기존 `STTPipeline(db=db)` 시그니처는 그대로 유지 — `tools/compare_nc.py` 같은 직접 호출자가 안 깨지도록.

### 4.3. FastAPI 부팅 시 pre-warm

`app/main.py` 의 startup/shutdown 핸들러:

```python
@app.on_event("startup")
async def _startup_stt_pipeline():
    """STT 파이프라인 pre-warm — Kiwi 형태소 분석기와 DB 매핑 사전을 부팅 시 1회 로드."""
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
```

### 4.4. 라우터에서 싱글톤 사용

`app/routers/stt.py`:

```python
# Before
pipeline = STTPipeline(db=db)

# After
pipeline = get_stt_pipeline()
```

### 4.5. `httpx.AsyncClient` 재사용 (보너스)

`app/services/nursing_stt/clova_stt.py` 에서 매 호출마다 새 클라이언트 만들던 것을 인스턴스 attribute 로 변경:

```python
class ClovaSTTClient:
    def __init__(self):
        ...
        # 매 요청마다 새 AsyncClient 를 만들면 TLS handshake + 연결 풀 재생성 비용
        # (~100-300ms) 이 누적된다. 싱글톤 라이프타임 동안 1개를 재사용.
        self._http = httpx.AsyncClient(timeout=60.0)

    async def aclose(self) -> None:
        """앱 종료 시 호출 — keep-alive 연결 정리."""
        await self._http.aclose()

    async def recognize(self, ...):
        ...
        # async with httpx.AsyncClient(...) 대신
        response = await self._http.post(...)
```

## 5. 변경 파일 요약

| 파일 | 변경 내용 |
|---|---|
| `app/services/nursing_stt/stt_pipeline.py` | 싱글톤 + `get_stt_pipeline()` + `load_dictionary(db)` 추가 |
| `app/services/nursing_stt/clova_stt.py` | `httpx.AsyncClient` 인스턴스 attribute 화 + `aclose()` 추가 |
| `app/main.py` | startup/shutdown 핸들러 추가 |
| `app/routers/stt.py` | `STTPipeline(db=db)` → `get_stt_pipeline()` |

## 6. 검증

### 6.1. 회귀 (동일 m4a, 결과 동일성)

동일 m4a (`uploads/audio/20260511_102c5747.m4a`) 로 `tools/compare_nc` 재실행:

| 지표 | 변경 전 baseline | 변경 후 | 결과 |
|---|---|---|---|
| NC=off conf | 0.7275 | 0.7275 | ✓ |
| NC=on conf | 0.6730 | 0.6730 | ✓ |
| `original_text` | "Npo 해제되어..." | 동일 | ✓ |
| `corrected_text` | "NPO 해제되어..." | 동일 | ✓ |
| `corrections` 길이 | 1 (Npo→NPO) | 동일 | ✓ |

→ 로직은 일체 안 바뀌었음을 보장.

### 6.2. 앱 import 검증

```bash
python -c "
from app.main import app
print('startup handlers:', len(app.router.on_startup))   # 2 (STT + handover)
print('shutdown handlers:', len(app.router.on_shutdown)) # 1 (STT)
"
```

### 6.3. 실서버 로그 확인 (사용자가 확인할 부분)

서버 재시작 후:
- **부팅 시 1번만** 출력되어야 할 줄:
  ```
  Kiwi 형태소 분석기 초기화 완료
  DB에서 매핑 사전 로드 완료
  매핑 사전 초기화: ...
  STT 파이프라인 초기화 완료
  [startup] STT 파이프라인 pre-warm 완료
  ```
- **이후 STT 요청에선** 이 줄들이 안 보여야 정상.

## 7. 예상 효과

| 시점 | 절약 |
|---|---|
| **서버 부팅 직후 첫 요청** | 0 (init 이 부팅에 흡수돼서 사용자는 안 보임) |
| **이후 모든 요청** | **약 1-3 초** (Kiwi 로드 + DB 사전 SELECT 제거) |
| **CLOVA 호출당 추가로** | ~100-300ms (TLS handshake + 연결 풀 재사용) |

## 8. 스코프 밖 (나중 고려용)

| 항목 | 비고 |
|---|---|
| **CLOVA API 호출 자체 단축** | 외부 서비스, 1-5초 불가피 |
| **CLOVA boostings 적용** | `quick_correction_dictionary` 를 CLOVA 힌트로 보내 source-level 매핑 강화 가능 — 별도 PR |
| **단위/숫자 정규화 확장** | `normalize_units` 에 단위 anchor 한글 숫자 패턴 추가 가능 — 별도 PR |
| **Java 백엔드 `SttReminderService` 최적화** | 이미 충분히 빠름 (~100ms 안쪽). 굳이 손댈 필요 없음 |
| **`correction.py` 의 TermMapper 인스턴스화** | 매 요청마다 TermMapper 새로 만드는 부분 (line 181) — 다른 엔드포인트이지만 같은 패턴. 필요하면 동일 싱글톤 재사용 가능 |

## 9. 한 줄 요약

> TermMapper / Kiwi 의 inmemory 캐시 구조는 원래 잘 짜여 있었지만, `STTPipeline` 인스턴스 자체가 매 HTTP 요청마다 throwaway 되면서 캐시 효과가 매번 0 으로 리셋됐다. 싱글톤화 + 부팅 시 pre-warm 으로 요청당 1-3초 절약.
