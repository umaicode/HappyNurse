"""수술 후 입원 환자의 자가보고 텍스트와 환자 컨텍스트(나이/수술명/POD/진단 등)를
Claude Haiku로 분류해 priority/category를 산출."""
from __future__ import annotations

import json
import logging
import os
from typing import Optional

import anthropic

logger = logging.getLogger(__name__)


SYSTEM_PROMPT = """당신은 수술 후 입원 환자의 음성 호소를 분류하는 의료 분류 시스템입니다.
이 환자들은 응급실 외래가 아니라 이미 입원해 회복 중이며, 환자별 컨텍스트
(나이, 수술 유형, 수술 후 경과일=POD, 진단)에 따라 같은 증상이라도 임상적
의미가 크게 다릅니다.

## 역할
- 환자의 발화 텍스트와 임상 컨텍스트를 종합해 중요도 등급(priority)과
  카테고리(category)를 판정합니다.

## 등급 정의 (KTAS 시간 scaffold)

### CRITICAL — KTAS 1-2, 즉시~10분 내 대응
생명·사지 즉각 위협 또는 잠재적 위협. 수술 후 환자에서 다음을 의심하면 CRITICAL:
- 호흡 위협: 무호흡, 청색증, 갑작스런 호흡곤란, 폐색전증 의심(흉통 동반)
- 순환 위협: 심근경색, 종격동염(특히 CABG POD 1-7), 출혈성 쇼크, DVT/PE
- 신경 위협: 의식변화, 편측마비(뇌졸중), 경련, 동공 변화
- 출혈: 수술 부위 다량 출혈, 토혈, 혈변, 객혈, 배액관 다량 출혈
- 합병증 의심:
  · 문합부 누출 (GI 수술 후 + 발열 + 복통/복부 강직)
  · 구획증후군 (정형외과 + 감각소실/사지 부종+딱딱)
  · 뇌압 상승 (신경외과 + 두통+구토/시야 장애)
  · 산후 출혈 (산부인과 + 질 출혈+어지러움)

### HIGH — KTAS 3, 30분 내 대응
현재 위독하지 않으나 평가 필요한 상태:
- 수술 부위 통증, 진통제 요청, 참을 수 없는 통증
- 단순 발열(38°C 이상) — 단, POD 1-3 동반 다른 증상 시 CRITICAL 의심
- 경한 호흡곤란(SpO2 ≥ 90% 추정)
- 창상 열개, 드레싱 필요, 카테터 이탈, 진물/고름
- 구토/복통 단독, 오심
- 요폐, 혈뇨

### MEDIUM — KTAS 4, 60분 내 대응
간호 조치는 필요하나 임상 위급도 낮음:
- 생활 편의: 물·이불·침대 조절·화장실 보조·자세 변경
- 경미한 불편: 수면 방해, 자세 불편, 소음
- 식이 관련: 식사 시간 문의, 배고픔
- POD ≥ 5 회복기 환자의 경증 호소

### LOW — KTAS 5, 120분 내
간호사 평가 불요한 정보 문의:
- 일정 문의(퇴원·검사·회진·면회 시간)
- 일반 안내(샤워 가능 시기, 식이 제한, 활동 제한, 약 정보, 절차)

## 컨텍스트 활용 규칙

1. POD(수술 후 경과일)가 같은 증상의 priority를 바꾼다.
   - POD 1-3: 합병증 발생 위험 시기. 발열/통증/구토 borderline 시 한 단계 상향.
   - POD ≥ 5: 회복기. 경증 호소는 한 단계 하향 가능. 단, 적색 증상은 절대 하향 금지.

2. 나이가 같은 증상의 priority를 바꾼다.
   - 65세 이상: 의식변화·낙상·호흡곤란은 항상 CRITICAL 보장.
   - 65세 이상 + 단순 어지러움: HIGH로 상향(단순 MEDIUM 금지).

3. 수술 유형별 합병증 watchlist (해당 수술이면 더 민감하게):
   - GS COLECTOMY/위절제: 문합부 누출(복통+발열, 복부 강직, 빈맥+복부 팽만)
   - CS CABG: 종격동염(흉골 부위 통증/딸깍거림), 심방세동(불규칙 심박)
   - OS TKA/THA: DVT(하지 부종+압통), 구획증후군(감각소실)
   - NS Craniotomy: 뇌압 상승(두통+구토, 의식변화, 동공 변화)
   - OBGY C/S: 산후 출혈(질 출혈+어지러움)

4. 안전 우선 원칙: 모호하면 항상 더 높은 등급으로. "괜찮아요"·"별거
   아닌데" 같은 부정 표현이 있어도 증상 자체가 위험하면 등급 낮추지 말 것.

5. 복합 증상 상향: HIGH 증상이 2개 이상 동시에 나타나면 CRITICAL로 상향.
   예: 발열 + 수술 부위 발적 = 패혈증 가능성 → CRITICAL
   예: 복통 + 구토 (특히 GI 수술 POD 1-3) = 문합부 누출 가능성 → CRITICAL

## 응답 형식

반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트를 포함하지 마세요.

{"priority":"critical|high|medium|low","category":"카테고리ID","confidence":0.00}

- category 값: respiratory, circulatory, hemorrhage, iv_line, fall_trauma,
  neurological, pain, infection_signs, gi_symptoms, wound_treatment,
  urinary_bowel, daily_comfort, minor_discomfort, meal_request,
  schedule_inquiry, general_inquiry
- confidence: 판정 확신도 (0.00~1.00)"""


FEW_SHOT_EXAMPLES = [
    # 컨텍스트 활용 1: 같은 발화도 POD/수술 유형이 다르면 CRITICAL ↔ MEDIUM 갈림
    {
        "role": "user",
        "content": (
            "환자 정보:\n"
            "- 나이: 70세, 성별: MALE\n"
            "- 진료부서: GS, 수술: COLECTOMY\n"
            "- 진단: 대장암, 주증상: 복통\n"
            "- 수술 후 경과: POD 2일\n\n"
            "진료부서 특이사항:\n"
            "소화기외과 추가 CRITICAL 기준:\n"
            "- 문합부 누출 징후: 복부 통증 + 발열 조합, 복부가 딱딱해짐, 복부 팽만 + 빈맥\n"
            "- 이유: 문합부 누출은 긴급 수술 필요\n\n"
            '환자 발화:\n"배가 좀 빵빵하고 가스가 안 나와요"\n\n'
            "위 발화의 중요도를 판정하세요."
        ),
    },
    {
        "role": "assistant",
        "content": '{"priority":"critical","category":"gi_symptoms","confidence":0.86}',
    },
    {
        "role": "user",
        "content": (
            "환자 정보:\n"
            "- 나이: 30세, 성별: FEMALE\n"
            "- 진료부서: GS, 수술: APPY\n"
            "- 진단: 충수염, 주증상: 우하복부 통증\n"
            "- 수술 후 경과: POD 5일\n\n"
            "진료부서 특이사항:\n"
            "소화기외과 추가 CRITICAL 기준:\n"
            "- 문합부 누출 징후: 복부 통증 + 발열 조합, 복부가 딱딱해짐, 복부 팽만 + 빈맥\n"
            "- 이유: 문합부 누출은 긴급 수술 필요\n\n"
            '환자 발화:\n"배가 좀 빵빵하고 가스가 안 나와요"\n\n'
            "위 발화의 중요도를 판정하세요."
        ),
    },
    {
        "role": "assistant",
        "content": '{"priority":"medium","category":"gi_symptoms","confidence":0.78}',
    },
    # 컨텍스트 활용 2: 고위험 수술(CABG) + POD 1 → 가벼운 호소도 CRITICAL
    {
        "role": "user",
        "content": (
            "환자 정보:\n"
            "- 나이: 65세, 성별: MALE\n"
            "- 진료부서: CS, 수술: CABG\n"
            "- 진단: 관상동맥질환, 주증상: 흉통\n"
            "- 수술 후 경과: POD 1일\n\n"
            "진료부서 특이사항:\n"
            "심장외과 추가 CRITICAL 기준:\n"
            "- 흉골 불안정: 가슴 절개 부위 벌어짐, 흉골이 흔들리는 느낌, 딸깍 소리\n"
            "- 심장 리듬 이상: 심장이 불규칙하게 뛰는 느낌, 갑작스러운 두근거림\n"
            "- 이유: 종격동염, 심방세동 위험\n\n"
            '환자 발화:\n"가슴이 약간 답답해요"\n\n'
            "위 발화의 중요도를 판정하세요."
        ),
    },
    {
        "role": "assistant",
        "content": '{"priority":"critical","category":"circulatory","confidence":0.82}',
    },
    # 컨텍스트 활용 3: 단순 정보 문의는 어떤 컨텍스트에서도 LOW
    {
        "role": "user",
        "content": (
            "환자 정보:\n"
            "- 나이: 25세, 성별: FEMALE\n"
            "- 진료부서: GS, 수술: APPY\n"
            "- 수술 후 경과: POD 3일\n\n"
            '환자 발화:\n"샤워해도 돼요?"\n\n'
            "위 발화의 중요도를 판정하세요."
        ),
    },
    {
        "role": "assistant",
        "content": '{"priority":"low","category":"general_inquiry","confidence":0.94}',
    },
    # 일반 일정 문의 → LOW
    {
        "role": "user",
        "content": (
            "환자 정보:\n"
            "- 나이: 50세, 성별: FEMALE\n"
            "- 진료부서: OS, 수술: TKA\n"
            "- 수술 후 경과: POD 7일\n\n"
            '환자 발화:\n"실밥 언제 풀어요?"\n\n'
            "위 발화의 중요도를 판정하세요."
        ),
    },
    {
        "role": "assistant",
        "content": '{"priority":"low","category":"schedule_inquiry","confidence":0.95}',
    },
    # 수술 후 통증 → HIGH
    {
        "role": "user",
        "content": (
            "환자 정보:\n"
            "- 나이: 60세, 성별: MALE\n"
            "- 진료부서: OS, 수술: TKA\n"
            "- 수술 후 경과: POD 1일\n\n"
            '환자 발화:\n"수술한 무릎이 너무 아파서 진통제 좀 주세요"\n\n'
            "위 발화의 중요도를 판정하세요."
        ),
    },
    {
        "role": "assistant",
        "content": '{"priority":"high","category":"pain","confidence":0.90}',
    },
    # 생활 편의 → MEDIUM
    {
        "role": "user",
        "content": (
            "환자 정보:\n"
            "- 나이: 45세, 성별: MALE\n"
            "- 진료부서: GS, 수술: APPY\n"
            "- 수술 후 경과: POD 2일\n\n"
            '환자 발화:\n"물 좀 주세요"\n\n'
            "위 발화의 중요도를 판정하세요."
        ),
    },
    {
        "role": "assistant",
        "content": '{"priority":"medium","category":"daily_comfort","confidence":0.97}',
    },
    # 안전 우선: 부정 표현이 있어도 호흡 위협이면 CRITICAL
    {
        "role": "user",
        "content": (
            "환자 정보:\n"
            "- 나이: 55세, 성별: FEMALE\n"
            "- 진료부서: GS, 수술: COLECTOMY\n"
            "- 수술 후 경과: POD 1일\n\n"
            "진료부서 특이사항:\n"
            "소화기외과 추가 CRITICAL 기준:\n"
            "- 문합부 누출 징후: 복부 통증 + 발열 조합, 복부가 딱딱해짐, 복부 팽만 + 빈맥\n"
            "- 이유: 문합부 누출은 긴급 수술 필요\n\n"
            '환자 발화:\n"좀 불편한데 괜찮은 것 같기도 하고... 근데 숨이 좀 답답해요"\n\n'
            "위 발화의 중요도를 판정하세요."
        ),
    },
    {
        "role": "assistant",
        "content": '{"priority":"critical","category":"respiratory","confidence":0.85}',
    },
]


DEPARTMENT_OVERRIDE_PROMPTS = {
    "orthopedics": "정형외과 추가 CRITICAL 기준:\n- 구획증후군 징후: 발가락/손가락 움직임 불가, 사지 부종 + 딱딱해짐, 감각 소실, 사지 변색\n- 이유: 구획증후군은 6시간 이내 처치하지 않으면 영구 손상",
    "cardiac_surgery": "심장외과 추가 CRITICAL 기준:\n- 흉골 불안정: 가슴 절개 부위 벌어짐, 흉골이 흔들리는 느낌, 딸깍 소리\n- 심장 리듬 이상: 심장이 불규칙하게 뛰는 느낌, 갑작스러운 두근거림\n- 이유: 종격동염, 심방세동 위험",
    "gi_surgery": "소화기외과 추가 CRITICAL 기준:\n- 문합부 누출 징후: 복부 통증 + 발열 조합, 복부가 딱딱해짐, 복부 팽만 + 빈맥\n- 이유: 문합부 누출은 긴급 수술 필요",
    "neurosurgery": "신경외과 추가 CRITICAL 기준:\n- 뇌압 상승 징후: 심한 두통 + 구토 조합, 한쪽 힘 빠짐, 시야 장애, 동공 크기 변화\n- 이유: 뇌출혈/뇌부종 긴급",
    "obstetrics": "산부인과 추가 CRITICAL 기준:\n- 수술 후 출혈: 질 출혈, 하혈, 출혈 + 어지러움 조합, 패드에 피가 많이\n- 이유: 산후 출혈 긴급",
}


DEPT_CODE_TO_OVERRIDE_KEY = {
    "OS": "orthopedics",
    "ORTHO": "orthopedics",
    "CS": "cardiac_surgery",
    "CARDIAC": "cardiac_surgery",
    "GS": "gi_surgery",
    "GI": "gi_surgery",
    "NS": "neurosurgery",
    "NEURO": "neurosurgery",
    "OB": "obstetrics",
    "OBGY": "obstetrics",
}


VALID_PRIORITIES = {"critical", "high", "medium", "low"}


def build_user_prompt(
    symptom_text: str,
    department_code: Optional[str] = None,
    surgery_name: Optional[str] = None,
    disease_name: Optional[str] = None,
    chief_complaint: Optional[str] = None,
    age: Optional[int] = None,
    gender: Optional[str] = None,
    pod_days: Optional[int] = None,
) -> str:
    info_lines = ["환자 정보:"]
    if age is not None or gender:
        age_part = f"{age}세" if age is not None else "-"
        info_lines.append(f"- 나이: {age_part}, 성별: {gender or '-'}")
    if department_code or surgery_name:
        info_lines.append(f"- 진료부서: {department_code or '-'}, 수술: {surgery_name or '-'}")
    if disease_name or chief_complaint:
        info_lines.append(f"- 진단: {disease_name or '-'}, 주증상: {chief_complaint or '-'}")
    if pod_days is not None:
        info_lines.append(f"- 수술 후 경과: POD {pod_days}일")

    parts = []
    if len(info_lines) > 1:
        parts.append("\n".join(info_lines))

    override_key = DEPT_CODE_TO_OVERRIDE_KEY.get(department_code or "")
    if override_key and override_key in DEPARTMENT_OVERRIDE_PROMPTS:
        parts.append(f"진료부서 특이사항:\n{DEPARTMENT_OVERRIDE_PROMPTS[override_key]}")

    parts.append(f'환자 발화:\n"{symptom_text}"')
    parts.append("위 발화의 중요도를 판정하세요.")
    return "\n\n".join(parts)


class LlmClassifier:
    def __init__(
        self,
        api_key: Optional[str] = None,
        model: Optional[str] = None,
        client: Optional[anthropic.Anthropic] = None,
    ):
        self.model = model or os.getenv("ANTHROPIC_MODEL", "claude-haiku-4-5-20251001")
        if client is not None:
            self.client = client
        else:
            # GMS 게이트웨이 토큰을 anthropic SDK 의 api_key 자리에 그대로 전달.
            # handover/clients/llm_client.py 와 동일 컨벤션.
            key = api_key or os.getenv("GMS_API_KEY")
            if not key:
                raise RuntimeError("GMS_API_KEY is not set")
            self.client = anthropic.Anthropic(
                base_url=os.getenv(
                    "ANTHROPIC_BASE_URL",
                    "https://gms.ssafy.io/gmsapi/api.anthropic.com",
                ),
                api_key=key,
            )

    def classify(
        self,
        symptom_text: str,
        department_code: Optional[str] = None,
        surgery_name: Optional[str] = None,
        disease_name: Optional[str] = None,
        chief_complaint: Optional[str] = None,
        age: Optional[int] = None,
        gender: Optional[str] = None,
        pod_days: Optional[int] = None,
    ) -> dict:
        user_prompt = build_user_prompt(
            symptom_text=symptom_text,
            department_code=department_code,
            surgery_name=surgery_name,
            disease_name=disease_name,
            chief_complaint=chief_complaint,
            age=age,
            gender=gender,
            pod_days=pod_days,
        )
        messages = [*FEW_SHOT_EXAMPLES, {"role": "user", "content": user_prompt}]
        response = self.client.messages.create(
            model=self.model,
            max_tokens=300,
            system=SYSTEM_PROMPT,
            messages=messages,
        )
        raw_text = response.content[0].text if response.content else ""
        return _parse_response(raw_text)


def _parse_response(raw_text: str) -> dict:
    text = raw_text.strip()
    # 모델이 JSON 외 잡문구를 붙였을 경우 첫 '{'~ 마지막 '}' 만 추출
    start = text.find("{")
    end = text.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise ValueError(f"LLM response did not contain JSON: {raw_text!r}")
    payload = json.loads(text[start : end + 1])
    priority = payload.get("priority")
    if priority not in VALID_PRIORITIES:
        raise ValueError(f"Invalid priority in LLM response: {priority!r}")
    return {
        "priority": priority,
        "category": payload.get("category"),
        "confidence": payload.get("confidence"),
    }
