"""Aggregated statistics for the admin dashboard."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from app.database.models import OrderStatus
from app.services.container import Repos


@dataclass
class Stats:
    total_users: int
    banned_users: int
    new_users_today: int
    total_orders: int
    completed_orders: int
    pending_orders: int
    total_revenue: int
    revenue_today: int
    revenue_month: int
    sales_today: int
    sales_month: int


async def build_stats(repos: Repos) -> Stats:
    now = datetime.now(timezone.utc)
    start_today = now.replace(hour=0, minute=0, second=0, microsecond=0)
    start_month = now - timedelta(days=30)

    return Stats(
        total_users=await repos.users.count(),
        banned_users=await repos.users.count_banned(),
        new_users_today=await repos.users.count_new_since(start_today),
        total_orders=await repos.orders.total_count(),
        completed_orders=await repos.orders.count_by_status(OrderStatus.COMPLETED),
        pending_orders=await repos.orders.count_by_status(OrderStatus.UNDER_REVIEW),
        total_revenue=await repos.orders.total_revenue(),
        revenue_today=await repos.orders.revenue_since(start_today),
        revenue_month=await repos.orders.revenue_since(start_month),
        sales_today=await repos.orders.count_completed_since(start_today),
        sales_month=await repos.orders.count_completed_since(start_month),
    )
