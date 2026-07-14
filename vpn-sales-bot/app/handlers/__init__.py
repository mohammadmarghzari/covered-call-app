"""Router registration."""
from __future__ import annotations

from aiogram import Dispatcher

from app.handlers import (
    my_orders,
    payment,
    purchase,
    receipt,
    start,
)
from app.handlers.admin import (
    broadcast,
    delivery,
    orders as admin_orders,
    panel,
    plans as admin_plans,
    settings as admin_settings,
    stats as admin_stats,
    users as admin_users,
    wallets as admin_wallets,
)


def register_routers(dp: Dispatcher) -> None:
    """Order matters: admin delivery (channel posts) and admin panel come first."""
    dp.include_router(delivery.router)      # admin channel reply -> deliver config
    dp.include_router(panel.router)
    dp.include_router(admin_plans.router)
    dp.include_router(admin_wallets.router)
    dp.include_router(admin_orders.router)
    dp.include_router(admin_stats.router)
    dp.include_router(admin_users.router)
    dp.include_router(broadcast.router)
    dp.include_router(admin_settings.router)

    # User-facing
    dp.include_router(start.router)
    dp.include_router(purchase.router)
    dp.include_router(payment.router)
    dp.include_router(receipt.router)
    dp.include_router(my_orders.router)
