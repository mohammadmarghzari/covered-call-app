"""Application entrypoint — starts the bot in long-polling mode."""
from __future__ import annotations

import asyncio

from redis.asyncio import Redis

from app.bot import create_bot, create_dispatcher, on_startup
from app.config import settings
from app.logger import get_logger, setup_logging

log = get_logger("main")


async def main() -> None:
    setup_logging()

    redis = Redis.from_url(settings.redis_url, decode_responses=True)
    bot = create_bot()
    dp = create_dispatcher(redis)

    dp.startup.register(on_startup)

    try:
        # Drop any updates accumulated while the bot was offline.
        await bot.delete_webhook(drop_pending_updates=True)
        await dp.start_polling(
            bot,
            allowed_updates=dp.resolve_used_update_types(),
        )
    finally:
        await bot.session.close()
        await redis.aclose()


def run() -> None:
    try:
        import uvloop  # type: ignore

        uvloop.install()
    except Exception:  # pragma: no cover - uvloop optional / not on windows
        pass
    try:
        asyncio.run(main())
    except (KeyboardInterrupt, SystemExit):
        log.info("shutdown")


if __name__ == "__main__":
    run()
