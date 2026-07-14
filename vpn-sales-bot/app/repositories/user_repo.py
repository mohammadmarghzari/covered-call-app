"""Data-access for users."""
from __future__ import annotations

from sqlalchemy import func, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.database.models import User


class UserRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get_by_telegram_id(self, telegram_id: int) -> User | None:
        result = await self.session.execute(
            select(User).where(User.telegram_id == telegram_id)
        )
        return result.scalar_one_or_none()

    async def get_or_create(
        self,
        telegram_id: int,
        username: str | None,
        first_name: str | None,
        last_name: str | None,
    ) -> User:
        user = await self.get_by_telegram_id(telegram_id)
        if user is None:
            user = User(
                telegram_id=telegram_id,
                username=username,
                first_name=first_name,
                last_name=last_name,
            )
            self.session.add(user)
            await self.session.flush()
        else:
            # keep profile info fresh
            user.username = username
            user.first_name = first_name
            user.last_name = last_name
        return user

    async def set_banned(self, telegram_id: int, banned: bool) -> bool:
        result = await self.session.execute(
            update(User).where(User.telegram_id == telegram_id).values(is_banned=banned)
        )
        return result.rowcount > 0

    async def is_banned(self, telegram_id: int) -> bool:
        result = await self.session.execute(
            select(User.is_banned).where(User.telegram_id == telegram_id)
        )
        value = result.scalar_one_or_none()
        return bool(value)

    async def all_active_ids(self) -> list[int]:
        result = await self.session.execute(
            select(User.telegram_id).where(User.is_banned.is_(False))
        )
        return [row[0] for row in result.all()]

    async def count(self) -> int:
        result = await self.session.execute(select(func.count(User.id)))
        return int(result.scalar_one())

    async def count_banned(self) -> int:
        result = await self.session.execute(
            select(func.count(User.id)).where(User.is_banned.is_(True))
        )
        return int(result.scalar_one())

    async def count_new_since(self, since) -> int:  # noqa: ANN001
        result = await self.session.execute(
            select(func.count(User.id)).where(User.created_at >= since)
        )
        return int(result.scalar_one())
