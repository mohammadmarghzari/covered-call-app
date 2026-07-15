"""Idempotent data seeder for initial plans (and optionally wallets).

Run once after the first deploy:

    docker compose run --rm bot python -m app.seed

Re-running is safe: a plan is only inserted if no plan with the same title
and duration already exists, so your later edits from the admin panel are
never overwritten.
"""
from __future__ import annotations

import asyncio

from sqlalchemy import select

from app.database.base import session_factory
from app.database.models import Plan
from app.logger import get_logger, setup_logging

log = get_logger("seed")

# volume_gb = 0 means unlimited. price is in Toman.
INITIAL_PLANS: list[dict] = [
    # --- یک‌ماهه (30 روز) ---
    {"title": "یک‌ماهه ۱۰ گیگ", "volume_gb": 10, "duration_days": 30, "price": 15_000, "sort_order": 10},
    {"title": "یک‌ماهه ۲۰ گیگ", "volume_gb": 20, "duration_days": 30, "price": 30_000, "sort_order": 11},
    {"title": "یک‌ماهه ۴۵ گیگ", "volume_gb": 45, "duration_days": 30, "price": 45_000, "sort_order": 12},
    {"title": "یک‌ماهه ۵۰ گیگ", "volume_gb": 50, "duration_days": 30, "price": 60_000, "sort_order": 13},
    # --- دوماهه (60 روز) ---
    {"title": "دوماهه ۱۰۰ گیگ", "volume_gb": 100, "duration_days": 60, "price": 100_000, "sort_order": 20},
    {"title": "دوماهه ۲۰۰ گیگ", "volume_gb": 200, "duration_days": 60, "price": 200_000, "sort_order": 21},
    {"title": "دوماهه ۳۰۰ گیگ", "volume_gb": 300, "duration_days": 60, "price": 300_000, "sort_order": 22},
    # --- سه‌ماهه (90 روز) ---
    {"title": "سه‌ماهه نامحدود", "volume_gb": 0, "duration_days": 90, "price": 450_000, "sort_order": 30},
]


async def seed_plans() -> None:
    async with session_factory() as session:
        result = await session.execute(select(Plan.title, Plan.duration_days))
        existing = {(t, d) for t, d in result.all()}

        added = 0
        for data in INITIAL_PLANS:
            key = (data["title"], data["duration_days"])
            if key in existing:
                continue
            session.add(
                Plan(
                    title=data["title"],
                    volume_gb=data["volume_gb"],
                    duration_days=data["duration_days"],
                    price=data["price"],
                    sort_order=data["sort_order"],
                    is_active=True,
                )
            )
            added += 1
        await session.commit()
        log.info("plans_seeded", added=added, skipped=len(INITIAL_PLANS) - added)


async def main() -> None:
    setup_logging()
    await seed_plans()


if __name__ == "__main__":
    asyncio.run(main())
