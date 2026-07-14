"""Typed callback-data factories (aiogram CallbackData)."""
from __future__ import annotations

from aiogram.filters.callback_data import CallbackData


class MenuCB(CallbackData, prefix="menu"):
    action: str  # buy | orders | support | tutorial | home


class PlanCB(CallbackData, prefix="plan"):
    plan_id: int


class PayCB(CallbackData, prefix="pay"):
    order_id: int
    method: str  # card | crypto


class WalletCB(CallbackData, prefix="wallet"):
    order_id: int
    wallet_id: int


class OrderNavCB(CallbackData, prefix="order"):
    action: str  # cancel | back
    order_id: int


class AdminCB(CallbackData, prefix="adm"):
    section: str          # plans | wallets | orders | stats | users | broadcast | settings | texts | card | home
    action: str = "open"  # open | add | del | edit | list | toggle | ...
    item_id: int = 0
    page: int = 0
