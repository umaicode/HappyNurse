import asyncio

import anthropic

from app.services.handover.config import Settings

_MAX_ATTEMPTS = 2
RETRYABLE = (anthropic.APIConnectionError, anthropic.APITimeoutError, asyncio.TimeoutError)


class LLMClient:
    def __init__(self, settings: Settings):
        self._model = settings.llm_model
        self._max_tokens = settings.llm_max_tokens
        self._timeout = settings.llm_total_timeout_s
        self._client = anthropic.AsyncAnthropic(
            base_url=settings.anthropic_base_url,
            api_key=settings.gms_api_key,
        )

    async def chat_json(
        self,
        system: str,
        user: str,
        json_schema: dict,
        temperature: float = 0.0,
    ) -> dict:
        """Tool use로 JSON 스키마를 강제해 구조화된 응답을 반환한다."""
        last_err: Exception | None = None
        for _ in range(_MAX_ATTEMPTS):
            try:
                resp = await asyncio.wait_for(
                    self._client.messages.create(
                        model=self._model,
                        max_tokens=self._max_tokens,
                        system=system,
                        messages=[{"role": "user", "content": user}],
                        tools=[{
                            "name": "extract_handover",
                            "description": "Extract structured clinical handover data",
                            "input_schema": json_schema,
                        }],
                        tool_choice={"type": "tool", "name": "extract_handover"},
                        temperature=temperature,
                    ),
                    timeout=self._timeout,
                )
                for block in resp.content:
                    if block.type == "tool_use":
                        return block.input
                raise ValueError("Anthropic 응답에 tool_use 블록이 없습니다")
            except RETRYABLE as e:
                last_err = e
                continue
        raise last_err  # type: ignore[misc]
