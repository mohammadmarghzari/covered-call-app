"""Application configuration loaded from environment variables."""
from __future__ import annotations

from functools import cached_property, lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Strongly-typed application settings.

    Only infrastructure / secret values live here. Everything that a
    non-technical admin may want to change at runtime (card number, wallets,
    texts, download links, ...) is stored in the database instead.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    # --- Telegram ---------------------------------------------------------
    bot_token: str = Field(..., alias="BOT_TOKEN")
    # Kept as a raw string so pydantic-settings doesn't try to JSON-decode it;
    # parsed into a list via the `admin_ids` property below.
    admin_ids_raw: str = Field("", alias="ADMIN_IDS")
    admin_channel_id: int = Field(..., alias="ADMIN_CHANNEL_ID")

    # --- Database ---------------------------------------------------------
    database_url: str = Field(..., alias="DATABASE_URL")

    # --- Redis ------------------------------------------------------------
    redis_url: str = Field("redis://redis:6379/0", alias="REDIS_URL")

    # --- Behaviour --------------------------------------------------------
    rate_limit_messages: int = Field(5, alias="RATE_LIMIT_MESSAGES")
    rate_limit_window: int = Field(3, alias="RATE_LIMIT_WINDOW")
    log_level: str = Field("INFO", alias="LOG_LEVEL")
    currency: str = Field("تومان", alias="CURRENCY")
    timezone: str = Field("Asia/Tehran", alias="TIMEZONE")

    @cached_property
    def admin_ids(self) -> list[int]:
        raw = self.admin_ids_raw or ""
        return [int(x.strip()) for x in raw.split(",") if x.strip()]

    def is_admin(self, user_id: int) -> bool:
        return user_id in self.admin_ids


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()  # type: ignore[call-arg]


settings = get_settings()
