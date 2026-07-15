"""Bot / dispatcher factory and startup helpers."""
from __future__ import annotations

from aiogram import Bot, Dispatcher
from aiogram.client.default import DefaultBotProperties
from aiogram.client.session.aiohttp import AiohttpSession
from aiogram.enums import ParseMode
from aiogram.fsm.storage.redis import RedisStorage
from aiogram.types import BotCommand, BotCommandScopeChat
from redis.asyncio import Redis

from app.config import settings
from app.database.base import session_factory
from app.handlers import register_routers
from app.logger import get_logger
from app.middlewares.ban import BanMiddleware
from app.middlewares.db import DBSessionMiddleware
from app.middlewares.throttling import ThrottlingMiddleware
from app.repositories.setting_repo import SettingRepository
from app.runtime import runtime
from app.utils.constants import DEFAULT_SETTINGS

log = get_logger("bot")


def create_bot() -> Bot:
    session = None
    if settings.telegram_proxy:
        session = AiohttpSession(proxy=settings.telegram_proxy)
        log.info("telegram_proxy_enabled", proxy=settings.telegram_proxy)
    return Bot(
        token=settings.bot_token,
        session=session,
        default=DefaultBotProperties(parse_mode=ParseMode.HTML),
    )


def create_dispatcher(redis: Redis) -> Dispatcher:
    storage = RedisStorage(redis=redis)
    dp = Dispatcher(storage=storage)

    # Middlewares. DB session is registered at the update level so that even
    # channel posts (config delivery) receive a repository container.
    dp.update.outer_middleware(DBSessionMiddleware())

    throttling = ThrottlingMiddleware(redis)
    ban = BanMiddleware()
    for observer in (dp.message, dp.callback_query):
        observer.outer_middleware(throttling)
        observer.outer_middleware(ban)

    register_routers(dp)
    return dp


async def _resolve_admin_channel(bot: Bot) -> None:
    """Resolve the admin channel to a numeric id and verify bot access."""
    if settings.admin_channel_numeric_id is not None:
        runtime.admin_channel_id = settings.admin_channel_numeric_id
    try:
        chat = await bot.get_chat(settings.admin_channel_ref)
        runtime.admin_channel_id = chat.id
        log.info("admin_channel_resolved", chat_id=chat.id, title=chat.title)
    except Exception as exc:  # pragma: no cover - depends on Telegram/admin rights
        log.warning(
            "admin_channel_unresolved",
            ref=str(settings.admin_channel_ref),
            error=str(exc),
            hint="Make sure the bot is an ADMIN of the channel.",
        )


async def on_startup(bot: Bot) -> None:
    """Seed default settings, resolve the channel and register bot commands."""
    async with session_factory() as session:
        repo = SettingRepository(session)
        await repo.seed_defaults(DEFAULT_SETTINGS)
        await session.commit()

    await _resolve_admin_channel(bot)

    await bot.set_my_commands(
        [
            BotCommand(command="start", description="شروع / منوی اصلی"),
        ]
    )
    for admin_id in settings.admin_ids:
        try:
            await bot.set_my_commands(
                [
                    BotCommand(command="start", description="شروع / منوی اصلی"),
                    BotCommand(command="admin", description="پنل مدیریت"),
                ],
                scope=BotCommandScopeChat(chat_id=admin_id),
            )
        except Exception as exc:  # pragma: no cover
            log.warning("set_admin_commands_failed", admin=admin_id, error=str(exc))

    me = await bot.get_me()
    log.info("bot_started", username=me.username, admins=len(settings.admin_ids))
