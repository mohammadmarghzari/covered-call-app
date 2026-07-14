"""Redis-backed anti-spam / rate limiting middleware."""
from __future__ import annotations

from collections.abc import Awaitable, Callable
from typing import Any

from aiogram import BaseMiddleware
from aiogram.types import CallbackQuery, Message, TelegramObject
from redis.asyncio import Redis

from app.config import settings
from app.logger import get_logger

log = get_logger("throttling")


class ThrottlingMiddleware(BaseMiddleware):
    """Sliding-window limiter using an INCR + EXPIRE counter per user.

    Admins are never throttled.
    """

    def __init__(self, redis: Redis) -> None:
        self.redis = redis
        self.limit = settings.rate_limit_messages
        self.window = settings.rate_limit_window

    async def __call__(
        self,
        handler: Callable[[TelegramObject, dict[str, Any]], Awaitable[Any]],
        event: TelegramObject,
        data: dict[str, Any],
    ) -> Any:
        user = data.get("event_from_user") or getattr(event, "from_user", None)
        if user is None or settings.is_admin(user.id):
            return await handler(event, data)

        key = f"throttle:{user.id}"
        try:
            count = await self.redis.incr(key)
            if count == 1:
                await self.redis.expire(key, self.window)
        except Exception as exc:  # pragma: no cover - never block on redis issues
            log.warning("throttle_redis_error", error=str(exc))
            return await handler(event, data)

        if count > self.limit:
            # Notify only on the first breach to avoid its own spam.
            if count == self.limit + 1:
                await self._warn(event)
            return None

        return await handler(event, data)

    @staticmethod
    async def _warn(event: TelegramObject) -> None:
        text = "⏳ لطفاً کمی آرام‌تر! چند لحظه صبر کنید و دوباره تلاش کنید."
        try:
            if isinstance(event, CallbackQuery):
                await event.answer(text, show_alert=False)
            elif isinstance(event, Message):
                await event.answer(text)
        except Exception:  # pragma: no cover
            pass
