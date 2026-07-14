"""Finite-state-machine states."""
from __future__ import annotations

from aiogram.fsm.state import State, StatesGroup


class PurchaseFlow(StatesGroup):
    waiting_for_receipt = State()


class AdminPlanFSM(StatesGroup):
    title = State()
    volume = State()
    duration = State()
    price = State()
    description = State()
    edit_value = State()


class AdminWalletFSM(StatesGroup):
    symbol = State()
    network = State()
    address = State()
    memo = State()


class AdminSettingFSM(StatesGroup):
    waiting_value = State()
    waiting_photo = State()


class AdminBroadcastFSM(StatesGroup):
    waiting_content = State()
    confirm = State()


class AdminSearchFSM(StatesGroup):
    waiting_query = State()


class AdminUserFSM(StatesGroup):
    waiting_ban_id = State()
    waiting_unban_id = State()
