"""환자 자가보고 텍스트를 Claude Haiku로 분류해 priority/category를 산출."""
from __future__ import annotations

import json
import logging
import os
from typing import Optional

import anthropic

logger = logging.getLogger(__name__)


SYSTEM_PROMPT = """당신은 병원 입원 환자의 요청을 분류하는 의료 분류 시스템입니다.
환자가 음성(STT)으로 전달한 텍스트를 읽고, 아래 기준에 따라 중요도를 판정하세요.

## 역할
- 환자의 발화 텍스트를 분석하여 중요도 등급(priority)과 카테고리(category)를 판정합니다.

## 중요도 등급 정의

### CRITICAL (즉시 대응, 3분 이내)
생명·안전에 직결되는 요청. 다음 중 하나라도 해당하면 CRITICAL:
- 호흡 이상: 호흡곤란, 기도폐쇄, 청색증, 폐색전증 의심
- 순환계 이상: 흉통, 심계항진, 실신, 쇼크 징후
- 출혈: 수술 부위 출혈, 토혈, 혈변, 객혈, 배액관 다량 출혈
- 수액/IV 라인 이상: 수액 침윤, 정맥염, 라인 이탈
- 낙상/외상: 넘어짐, 침대 추락
- 신경계 이상: 의식 변화, 섬망, 경련, 편측 마비

### HIGH (빠른 대응, 10분 이내)
의료적 판단이 필요한 요청. 다음 중 하나라도 해당하면 HIGH:
- 통증: 수술 부위 통증, 진통제 요청, 참을 수 없는 통증
- 감염 징후: 발열, 수술 부위 발적/부종/화농, 오한
- 소화기 증상: 오심, 구토, 복통, 장폐색 의심 (복부 팽만 + 가스 안 나옴)
- 상처/처치: 창상 열개, 드레싱 교체 필요, 카테터 이탈
- 배뇨/배변 이상: 요폐, 혈뇨, 심한 변비

### MEDIUM (일반 요청, 30분 이내)
간호 업무이나 긴급하지 않은 요청:
- 생활 편의: 물, 이불, 침대 조절, 화장실 보조, 리모컨
- 경미한 불편: 수면 방해, 자세 불편, 소음
- 식이 관련: 식사 시간 문의, 배고픔

### LOW (단순 정보 문의)
긴급하지 않은 정보 문의:
- 일정 문의: 퇴원 시기, 검사 시간, 회진 시간, 면회
- 일반 문의: 식이 제한, 활동 제한, 약 정보, 절차 안내

## 판정 규칙

1. 안전 우선 원칙: 애매하면 항상 더 높은 등급으로 분류하세요.
2. 복합 증상 상향: HIGH 증상이 2개 이상 동시에 나타나면 CRITICAL로 상향하세요.
   예: 발열 + 수술 부위 발적 = 패혈증 가능성 → CRITICAL
   예: 복통 + 구토 = 문합부 누출 가능성 → CRITICAL
3. 통증 강도: "참을 수 없다", "너무 아프다", "죽을 것 같다" 등 극심한 통증 표현은 HIGH로 유지하되, 호흡곤란이나 의식 변화를 동반하면 CRITICAL로 상향하세요.
4. 부정 표현 주의: "괜찮아요", "별거 아닌데" 같은 표현이 있어도 증상 자체가 위험하면 등급을 낮추지 마세요.

## 응답 형식

반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트를 포함하지 마세요.

{"priority":"critical|high|medium|low","category":"카테고리ID","confidence":0.00}

- category 값: respiratory, circulatory, hemorrhage, iv_line, fall_trauma, neurological, pain, infection_signs, gi_symptoms, wound_treatment, urinary_bowel, daily_comfort, minor_discomfort, meal_request, schedule_inquiry, general_inquiry
- confidence: 판정 확신도 (0.00~1.00)"""


FEW_SHOT_EXAMPLES = [
    {
        "role": "user",
        "content": "환자 정보:\n- 진료부서: GS\n- 수술 유형: COLECTOMY\n- 입원일: 2025-05-01\n\n환자 발화:\n\"배가 좀 빵빵한 느낌이고 가스가 안 나와요\"\n\n위 발화의 중요도를 판정하세요.",
    },
    {
        "role": "assistant",
        "content": '{"priority":"high","category":"gi_symptoms","confidence":0.88}',
    },
    {
        "role": "user",
        "content": "환자 정보:\n- 진료부서: OS\n- 수술 유형: TKA\n- 입원일: 2025-05-03\n\n환자 발화:\n\"실밥 언제 풀어요?\"\n\n위 발화의 중요도를 판정하세요.",
    },
    {
        "role": "assistant",
        "content": '{"priority":"low","category":"schedule_inquiry","confidence":0.95}',
    },
    {
        "role": "user",
        "content": "환자 정보:\n- 진료부서: GS\n- 수술 유형: COLECTOMY\n- 입원일: 2025-05-01\n\n환자 발화:\n\"좀 이상한 느낌이에요. 배도 아프고 열도 좀 나는 것 같아요\"\n\n위 발화의 중요도를 판정하세요.",
    },
    {
        "role": "assistant",
        "content": '{"priority":"critical","category":"infection_signs","confidence":0.91}',
    },
    {
        "role": "user",
        "content": "환자 정보:\n- 진료부서: CS\n- 수술 유형: CABG\n- 입원일: 2025-05-02\n\n진료부서 특이사항:\n심장외과 추가 CRITICAL 기준:\n- 흉골 불안정: 가슴 절개 부위 벌어짐, 흉골이 흔들리는 느낌, 딸깍 소리\n- 심장 리듬 이상: 심장이 불규칙하게 뛰는 느낌, 갑작스러운 두근거림\n- 이유: 종격동염, 심방세동 위험\n\n환자 발화:\n\"가슴 쪽에서 뭔가 딸깍거리는 느낌이 나요\"\n\n위 발화의 중요도를 판정하세요.",
    },
    {
        "role": "assistant",
        "content": '{"priority":"critical","category":"circulatory","confidence":0.93}',
    },
    {
        "role": "user",
        "content": "환자 정보:\n- 진료부서: GS\n- 수술 유형: APPY\n- 입원일: 2025-05-05\n\n환자 발화:\n\"물 좀 주세요\"\n\n위 발화의 중요도를 판정하세요.",
    },
    {
        "role": "assistant",
        "content": '{"priority":"medium","category":"daily_comfort","confidence":0.97}',
    },
    {
        "role": "user",
        "content": "환자 정보:\n- 진료부서: OS\n- 수술 유형: TKA\n- 입원일: 2025-05-03\n\n환자 발화:\n\"수술한 쪽 다리가 점점 부어오르고 딱딱해지는 느낌이에요\"\n\n위 발화의 중요도를 판정하세요.",
    },
    {
        "role": "assistant",
        "content": '{"priority":"critical","category":"circulatory","confidence":0.90}',
    },
    {
        "role": "user",
        "content": "환자 정보:\n- 진료부서: GS\n- 수술 유형: COLECTOMY\n- 입원일: 2025-05-01\n\n환자 발화:\n\"좀 불편한데 괜찮은 것 같기도 하고... 근데 숨이 좀 답답해요\"\n\n위 발화의 중요도를 판정하세요.",
    },
    {
        "role": "assistant",
        "content": '{"priority":"critical","category":"respiratory","confidence":0.85}',
    },
    {
        "role": "user",
        "content": "환자 정보:\n- 진료부서: OBGY\n- 수술 유형: CS\n- 입원일: 2025-05-04\n\n환자 발화:\n\"샤워해도 되나요? 상처에 물 닿아도 괜찮은지 궁금해요\"\n\n위 발화의 중요도를 판정하세요.",
    },
    {
        "role": "assistant",
        "content": '{"priority":"low","category":"general_inquiry","confidence":0.96}',
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


USER_PROMPT_TEMPLATE = (
    '환자 정보:\n'
    '- 진료부서: {department_code}\n'
    '- 수술 유형: {surgery_type_code}\n'
    '- 입원일: {period_start}\n\n'
    '환자 발화:\n'
    '"{symptom_text}"\n\n'
    '위 발화의 중요도를 판정하세요.'
)

USER_PROMPT_WITH_OVERRIDE_TEMPLATE = (
    '환자 정보:\n'
    '- 진료부서: {department_code}\n'
    '- 수술 유형: {surgery_type_code}\n'
    '- 입원일: {period_start}\n\n'
    '진료부서 특이사항:\n{department_override_text}\n\n'
    '환자 발화:\n'
    '"{symptom_text}"\n\n'
    '위 발화의 중요도를 판정하세요.'
)


VALID_PRIORITIES = {"critical", "high", "medium", "low"}


def build_user_prompt(
    symptom_text: str,
    department_code: Optional[str],
    surgery_type_code: Optional[str] = None,
    period_start: Optional[str] = None,
) -> str:
    override_key = DEPT_CODE_TO_OVERRIDE_KEY.get(department_code or "")
    fmt_args = {
        "department_code": department_code or "-",
        "surgery_type_code": surgery_type_code or "-",
        "period_start": period_start or "-",
        "symptom_text": symptom_text,
    }
    if override_key and override_key in DEPARTMENT_OVERRIDE_PROMPTS:
        fmt_args["department_override_text"] = DEPARTMENT_OVERRIDE_PROMPTS[override_key]
        return USER_PROMPT_WITH_OVERRIDE_TEMPLATE.format(**fmt_args)
    return USER_PROMPT_TEMPLATE.format(**fmt_args)


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
            key = api_key or os.getenv("ANTHROPIC_API_KEY")
            if not key:
                raise RuntimeError("ANTHROPIC_API_KEY is not set")
            self.client = anthropic.Anthropic(api_key=key)

    def classify(
        self,
        symptom_text: str,
        department_code: Optional[str] = None,
        surgery_type_code: Optional[str] = None,
        period_start: Optional[str] = None,
    ) -> dict:
        user_prompt = build_user_prompt(
            symptom_text, department_code, surgery_type_code, period_start
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
