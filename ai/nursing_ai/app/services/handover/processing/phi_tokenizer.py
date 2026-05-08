import re
from typing import Mapping


_TOKEN_RE = re.compile(r"@@PHI_[A-Z]+_\d+@@")


class PHITokenizer:
    """요청 단위 메모리 토큰화. 영속화 절대 금지 (스펙 § 11.1)."""

    _CATEGORY_PREFIX = {
        "이름": "NAME",
        "주민번호": "RRN",
        "환자번호": "MRN",
        "전화": "TEL",
        "병실": "ROOM",
        "병동": "WARD",
        "보호자명": "GUARDIAN",
        "외부의료기관": "EXTHOSP",
    }

    def mask(self, text: str, fields: Mapping[str, str]) -> tuple[str, dict[str, str]]:
        mapping: dict[str, str] = {}
        seen_value_to_token: dict[str, str] = {}
        counters: dict[str, int] = {}
        for label, value in fields.items():
            if not value:
                continue
            prefix = self._CATEGORY_PREFIX.get(label, "MISC")
            if value in seen_value_to_token:
                token = seen_value_to_token[value]
            else:
                counters[prefix] = counters.get(prefix, 0) + 1
                token = f"@@PHI_{prefix}_{counters[prefix]}@@"
                seen_value_to_token[value] = token
                mapping[token] = value
            text = text.replace(value, token)
        return text, mapping

    def unmask(self, text: str, mapping: Mapping[str, str]) -> str:
        for token, value in mapping.items():
            text = text.replace(token, value)
        return text
