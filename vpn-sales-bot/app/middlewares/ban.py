"""Middleware that blocks banned users from interacting with the bot."""
from __future__ import annotations

from collections.abc import Awaitable, Callable
from typing import Any

from aiogram import BaseMiddleware
from aiogram.types import CallbackQuery, Message, TelegramObject

from app.config import settings
from app.services.container import Repos


class BanMiddleware(BaseMiddleware):
    async def __call__(
        self,
        handler: Callable[[TelegramObject, dict[str, Any]], Awaitable[Any]],
        event: TelegramObject,
        data: dict[str, Any],
    ) -> Any:
        user = data.get("event_from_user") or getattr(event, "from_user", None)
        if user is None or settings.is_admin(user.id):
            return await handler(event, data)

        repos: Repos | None = data.get("repos")
        if repos is not None and await repos.users.is_banned(user.id):
            text = "⛔️ دسترسی شما به ربات مسدود شده است. برای پیگیری با پشتیبانی تماس بگیرید."
            if isinstance(event, CallbackQuery):
                await event.answer(text, show_alert=True)
            elif isinstance(event, Message):
                await event.answer(text)
            return None

        return await handler(event, data)
