"""Data-access for orders."""
from __future__ import annotations

from datetime import datetime

from sqlalchemy import func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import joinedload

from app.database.models import Order, OrderStatus, PaymentType


class OrderRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def create(
        self, user_id: int, plan_id: int, plan_title: str, price: int
    ) -> Order:
        order = Order(
            user_id=user_id,
            plan_id=plan_id,
            plan_title=plan_title,
            price=price,
            status=OrderStatus.AWAITING_PAYMENT,
        )
        self.session.add(order)
        await self.session.flush()
        return order

    async def get(self, order_id: int) -> Order | None:
        return await self.session.get(Order, order_id)

    async def get_with_user(self, order_id: int) -> Order | None:
        result = await self.session.execute(
            select(Order).options(joinedload(Order.user)).where(Order.id == order_id)
        )
        return result.scalar_one_or_none()

    async def get_by_admin_message(self, admin_message_id: int) -> Order | None:
        result = await self.session.execute(
            select(Order)
            .options(joinedload(Order.user))
            .where(Order.admin_message_id == admin_message_id)
        )
        return result.scalar_one_or_none()

    async def set_payment_method(
        self, order_id: int, payment_type: PaymentType, detail: str | None
    ) -> None:
        order = await self.get(order_id)
        if order:
            order.payment_type = payment_type
            order.payment_detail = detail

    async def attach_receipt(
        self, order_id: int, receipt_file_id: str
    ) -> Order | None:
        order = await self.get_with_user(order_id)
        if order:
            order.receipt_file_id = receipt_file_id
            order.status = OrderStatus.UNDER_REVIEW
        return order

    async def set_admin_message_id(self, order_id: int, message_id: int) -> None:
        order = await self.get(order_id)
        if order:
            order.admin_message_id = message_id

    async def mark_completed(
        self, order_id: int, config_link: str, delivered_by: int
    ) -> None:
        order = await self.get(order_id)
        if order:
            order.config_link = config_link
            order.delivered_by = delivered_by
            order.status = OrderStatus.COMPLETED

    async def set_status(self, order_id: int, status: OrderStatus) -> None:
        order = await self.get(order_id)
        if order:
            order.status = status

    async def list_by_user(self, user_id: int, limit: int = 20) -> list[Order]:
        result = await self.session.execute(
            select(Order)
            .where(Order.user_id == user_id)
            .order_by(Order.created_at.desc())
            .limit(limit)
        )
        return list(result.scalars().all())

    async def list_recent(self, limit: int = 10, offset: int = 0) -> list[Order]:
        result = await self.session.execute(
            select(Order)
            .options(joinedload(Order.user))
            .order_by(Order.created_at.desc())
            .limit(limit)
            .offset(offset)
        )
        return list(result.scalars().all())

    async def search(self, query: str, limit: int = 15) -> list[Order]:
        """Search by order id or by user's telegram id / username."""
        from app.database.models import User

        conditions = []
        if query.lstrip("#").isdigit():
            num = int(query.lstrip("#"))
            conditions.append(Order.id == num)
            conditions.append(User.telegram_id == num)
        username = query.lstrip("@").lower()
        conditions.append(func.lower(User.username) == username)

        result = await self.session.execute(
            select(Order)
            .join(User, Order.user_id == User.id)
            .options(joinedload(Order.user))
            .where(or_(*conditions))
            .order_by(Order.created_at.desc())
            .limit(limit)
        )
        return list(result.scalars().all())

    # --- stats helpers ----------------------------------------------------
    async def count_by_status(self, status: OrderStatus) -> int:
        result = await self.session.execute(
            select(func.count(Order.id)).where(Order.status == status)
        )
        return int(result.scalar_one())

    async def total_revenue(self) -> int:
        result = await self.session.execute(
            select(func.coalesce(func.sum(Order.price), 0)).where(
                Order.status == OrderStatus.COMPLETED
            )
        )
        return int(result.scalar_one())

    async def revenue_since(self, since: datetime) -> int:
        result = await self.session.execute(
            select(func.coalesce(func.sum(Order.price), 0)).where(
                Order.status == OrderStatus.COMPLETED, Order.updated_at >= since
            )
        )
        return int(result.scalar_one())

    async def count_completed_since(self, since: datetime) -> int:
        result = await self.session.execute(
            select(func.count(Order.id)).where(
                Order.status == OrderStatus.COMPLETED, Order.updated_at >= since
            )
        )
        return int(result.scalar_one())

    async def total_count(self) -> int:
        result = await self.session.execute(select(func.count(Order.id)))
        return int(result.scalar_one())
