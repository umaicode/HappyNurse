# 퀵수정(`/api/correction/analyze`) 개선 정리

수정 파일: `ai/nursing_ai/app/routers/correction.py`
연관 파일(읽기만): `ai/nursing_ai/app/services/nursing_stt/stt_pipeline.py`, `term_mapper.py`, `morpheme.py`

---

## 1. 콜드 스타트 제거 — 매 요청 초기화 → 싱글톤 재사용

### 증상
`/api/correction/analyze` 호출마다 서버 로그에 다음이 매번 출력되고 응답이 느렸음.
```
Kiwi 형태소 분석기 초기화 완료
DB에서 매핑 사전 로드 완료
매핑 사전 초기화: 정확 매칭 411개, 퍼지 매칭 대상 248개
```

### 원인
`analyze_quick_corrections` 안에서 매 요청마다 새 인스턴스 생성.
- `MorphemeAnalyzer()` → Kiwi 한국어 사전 로드(~1–3초)
- `TermMapper(db=db)` → `quick_correction_dictionary` 659행 SELECT

이미 프로젝트에는 STT 파이프라인용 싱글톤이 만들어져 있었음.
- `stt_pipeline.py`의 `get_stt_pipeline()` — 프로세스 라이프타임 동안 1회 인스턴스화
- `STTPipeline.load_dictionary(db)` — 사전만 reload, Kiwi는 유지
- `app/main.py` startup에서 앱 기동 시 1회 DB 사전 로드
- `routers/stt.py`는 이미 이 패턴 사용 중. `correction.py`만 안 쓰고 있었음.

### 수정 내용

**(1) `analyze_quick_corrections` — 싱글톤 재사용**
```python
# 변경 전
from app.services.nursing_stt.morpheme import MorphemeAnalyzer
from app.services.nursing_stt.term_mapper import TermMapper
morpheme = MorphemeAnalyzer()
mapper = TermMapper(db=db)

# 변경 후
from app.services.nursing_stt.stt_pipeline import get_stt_pipeline
pipeline = get_stt_pipeline()
morpheme = pipeline.morpheme
mapper = pipeline.mapper
```

**(2) 사전 INSERT 시점에 캐시 무효화 헬퍼 추가**
모듈 상단(`router = APIRouter()` 직후):
```python
def _reload_pipeline_dictionary(db: Session) -> None:
    """사전 INSERT 후 STT 싱글톤의 매핑 사전을 재로드 (Kiwi 는 유지, 사전만 교체)."""
    from app.services.nursing_stt.stt_pipeline import get_stt_pipeline
    try:
        get_stt_pipeline().load_dictionary(db)
    except Exception as e:
        # reload 실패가 사용자 응답을 깨면 안 됨 — 다음 startup/요청에서 자연 복구
        print(f"[correction] pipeline dictionary reload 실패(무시): {e}")
```

**(3) `apply_correction` — 새 dictionary INSERT 분기에서만 reload**
- `dictionary_inserted = False` 플래그 도입
- 새 매핑 INSERT 분기에서 `dictionary_inserted = True`
- `db.commit()` 직후 `if dictionary_inserted: _reload_pipeline_dictionary(db)`
- rollback 경로에서는 reload 호출 안 됨 → 트랜잭션 실패한 사전을 캐시에 올리는 일 없음

**(4) `approve_to_dictionary` — commit 직후 무조건 reload**
- 이 엔드포인트는 항상 INSERT가 일어나는 전제이므로 분기 없음

### 효과
- 첫 요청부터 startup에서 로드된 싱글톤을 그대로 사용 → Kiwi/SQL 비용 0
- 사전이 실제로 바뀌는 시점(`apply_correction`의 새 단어, `approve_to_dictionary`)에만 mapper 한 번 reload → 다른 사용자에게도 즉시 반영
- 회귀 위험 낮음: STT 엔드포인트(`/api/stt/...`)와 동일 싱글톤을 공유하지만 인터페이스 변경 없음

### 검증 포인트
1. 서버 재시작 → 첫 `/api/correction/analyze` 호출에서 위 3개 init 메시지가 **추가로 찍히지 않음**
2. 같은 엔드포인트 5회 연속 호출 시에도 재초기화 없음, 응답 시간 단축
3. 신규 단어로 `apply_correction` 호출 직후 로그에 "매핑 사전 초기화…" 1회만 찍히고, 그 다음 analyze 결과에 새 단어 후보가 잡힘

---

## 2. "세프트리악손 악손" 중복 버그 — 겹치는 후보 longest-match 필터링

### 증상
입력: `"세포트리 악손"`
- 백엔드가 두 후보를 모두 반환:
  - `(start=0, end=7)` "세포트리 악손" → "세프트리악손"
  - `(start=0, end=4)` "세포트리" → "세프트리악손"
- 사용자가 짧은 후보 "세포트리"(end=4) 선택 → 결과: `"세프트리악손" + " 악손" = "세프트리악손 악손"` (뒤의 "악손"이 그대로 남음)

### 원인
- `term_mapper.find_dictionary_matches()`가 longest-match 처리 없이 사전 매핑된 모든 매칭을 반환
- `correction.py`의 중복 제거 로직(`seen_positions = set()`)이 `(start, end)` 튜플 동일성만 보기 때문에 **겹치지만 다른 범위**의 후보는 그대로 통과
- 프론트는 후보의 `(start, end)`로 정확한 위치 치환을 수행 → end=4까지만 잘리고 뒤의 " 악손"은 보존됨

> 결정한 정책: **긴 매칭 우선** (사용자 확인). 사전 자체는 두 매핑 모두 정상이므로 사전을 건드리지 않고, 후보 응답 단계에서만 흡수.

### 수정 내용
`analyze_quick_corrections`에서 `corrections` 리스트 빌드 직후, return 직전에 한 블록 추가:
```python
# 겹치는 후보 정리: 더 긴 범위가 짧은 범위를 흡수 (longest-match 우선)
# 예) (0,7) "세포트리 악손" 이 있으면 (0,4) "세포트리", (5,7) "악손" 은 제거
corrections.sort(key=lambda c: (c["start"], -(c["end"] - c["start"])))
filtered = []
for c in corrections:
    contained = any(
        kept["start"] <= c["start"] and c["end"] <= kept["end"]
        and (kept["start"], kept["end"]) != (c["start"], c["end"])
        for kept in filtered
    )
    if contained:
        continue
    filtered.append(c)
corrections = filtered
```

### 동작 정리
- **strictly contained**(완전히 포함되고 같지 않음)인 후보만 제거
- 동일 `(start, end)`는 기존 `seen_positions` 로직이 이미 제거하므로 중복 처리 없음
- 부분 겹침(예: `(0,4)`와 `(3,7)`)은 그대로 유지 — 발생 가능성 낮고 자르기 까다로워 보수적 처리
- `dict_matches`가 먼저 들어오는 기존 우선순위 유지 → 같은 범위에 dict 후보가 있으면 morpheme 후보가 밀리는 동작은 그대로

### 손대지 않은 것
- `term_mapper.py` — exact_dict에 `"세포트리 악손"`, `"세포트리"` 두 매핑이 다 있는 건 단어 의미상 정상(각각 단독 STT 오인식 가능). 사전 정리는 별도 이슈로 분리.
- `NursingTab.tsx`의 `onApply` 콜백 — 이번 버그는 첫 적용에서도 재현되어 프론트 `replace()` 분기와 무관. 손대지 않음.

### 검증 포인트
1. `POST /api/correction/analyze` body: `{"content": "세포트리 악손"}`
   - 기대: `corrections`에 `(0,7)` "세포트리 악손" 1개만, `(0,4)` "세포트리"는 없음
2. 프론트에서 적용 → textarea 값이 `"세프트리악손"` 단일 문자열
3. 회귀: `"사무실 가서 카타플람 먹어"` 같은 겹치지 않는 다중 오인식어는 모두 후보로 노출
4. 단일 단어 입력 `"세포트리"` → `(0,4)` 후보 1개 정상 노출
