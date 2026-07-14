"""Data-access for plans."""
from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database.models import Plan


class PlanRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def list_active(self) -> list[Plan]:
        result = await self.session.execute(
            select(Plan)
            .where(Plan.is_active.is_(True))
            .order_by(Plan.sort_order, Plan.price)
        )
        return list(result.scalars().all())

    async def list_all(self) -> list[Plan]:
        result = await self.session.execute(
            select(Plan).order_by(Plan.sort_order, Plan.price)
        )
        return list(result.scalars().all())

    async def get(self, plan_id: int) -> Plan | None:
        return await self.session.get(Plan, plan_id)

    async def create(
        self,
        title: str,
        volume_gb: int,
        duration_days: int,
        price: int,
        description: str | None = None,
    ) -> Plan:
        plan = Plan(
            title=title,
            volume_gb=volume_gb,
            duration_days=duration_days,
            price=price,
            description=description,
        )
        self.session.add(plan)
        await self.session.flush()
        return plan

    async def delete(self, plan_id: int) -> bool:
        plan = await self.get(plan_id)
        if plan is None:
            return False
        await self.session.delete(plan)
        return True

    async def count(self) -> int:
        result = await self.session.execute(select(Plan))
        return len(list(result.scalars().all()))
