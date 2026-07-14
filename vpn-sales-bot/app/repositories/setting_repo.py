"""Data-access for the key/value settings store."""
from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.database.models import Setting


class SettingRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get(self, key: str, default: str | None = None) -> str | None:
        result = await self.session.execute(
            select(Setting.value).where(Setting.key == key)
        )
        value = result.scalar_one_or_none()
        return value if value is not None else default

    async def get_many(self, keys: list[str]) -> dict[str, str | None]:
        result = await self.session.execute(
            select(Setting.key, Setting.value).where(Setting.key.in_(keys))
        )
        found = {row[0]: row[1] for row in result.all()}
        return {key: found.get(key) for key in keys}

    async def set(self, key: str, value: str) -> None:
        stmt = (
            pg_insert(Setting)
            .values(key=key, value=value)
            .on_conflict_do_update(index_elements=["key"], set_={"value": value})
        )
        await self.session.execute(stmt)

    async def seed_defaults(self, defaults: dict[str, str]) -> None:
        """Insert default values only for keys that do not exist yet."""
        for key, value in defaults.items():
            stmt = (
                pg_insert(Setting)
                .values(key=key, value=value)
                .on_conflict_do_nothing(index_elements=["key"])
            )
            await self.session.execute(stmt)
