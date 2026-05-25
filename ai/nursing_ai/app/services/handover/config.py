from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    gms_api_key: str
    anthropic_base_url: str = "https://gms.ssafy.io/gmsapi/api.anthropic.com"
    llm_model: str = "claude-sonnet-4-5-20250929"
    llm_max_tokens: int = 8192
    llm_total_timeout_s: float = 120.0

    database_url: str

    lexicon_dir: str = "./app/services/handover/data/lexicon"
    rules_dir: str = "./app/services/handover/data/rules"
    lexicon_version: str = "2026.05.01"
    rule_set_version: str = "2026.05.01"


def get_settings() -> Settings:
    return Settings()  # type: ignore[call-arg]