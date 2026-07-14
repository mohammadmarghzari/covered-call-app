"""A lightweight per-request container grouping all repositories.

Handlers receive a `AsyncSession` from the DB middleware and wrap it in
`Repos` to access every repository through one object, keeping the handler
code clean while preserving a single transactional scope.
"""
from __future__ import annotations

from sqlalchemy.ext.asyncio import AsyncSession

from app.repositories.log_repo import LogRepository
from app.repositories.order_repo import OrderRepository
from app.repositories.plan_repo import PlanRepository
from app.repositories.setting_repo import SettingRepository
from app.repositories.user_repo import UserRepository
from app.repositories.wallet_repo import WalletRepository


class Repos:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session
        self.users = UserRepository(session)
        self.plans = PlanRepository(session)
        self.orders = OrderRepository(session)
        self.wallets = WalletRepository(session)
        self.settings = SettingRepository(session)
        self.logs = LogRepository(session)
