"""Inline keyboards shown to regular users."""
from __future__ import annotations

from aiogram.types import InlineKeyboardButton, InlineKeyboardMarkup
from aiogram.utils.keyboard import InlineKeyboardBuilder

from app.database.models import CryptoWallet, Plan
from app.keyboards.callbacks import (
    MenuCB,
    OrderNavCB,
    PayCB,
    PlanCB,
    WalletCB,
)
from app.utils.formatting import plan_button_label


def main_menu_kb() -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.button(text="🛒 خرید کانفیگ", callback_data=MenuCB(action="buy"))
    kb.button(text="📦 خریدهای من", callback_data=MenuCB(action="orders"))
    kb.button(text="❓ آموزش اتصال", callback_data=MenuCB(action="tutorial"))
    kb.button(text="📞 پشتیبانی", callback_data=MenuCB(action="support"))
    kb.adjust(1, 2, 1)
    return kb.as_markup()


def plans_kb(plans: list[Plan]) -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    for plan in plans:
        kb.button(
            text=plan_button_label(plan.title, plan.price),
            callback_data=PlanCB(plan_id=plan.id),
        )
    kb.button(text="🔙 بازگشت", callback_data=MenuCB(action="home"))
    kb.adjust(1)
    return kb.as_markup()


def payment_methods_kb(order_id: int) -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.button(text="💳 کارت بانکی", callback_data=PayCB(order_id=order_id, method="card"))
    kb.button(text="🪙 ارز دیجیتال", callback_data=PayCB(order_id=order_id, method="crypto"))
    kb.button(text="🔙 بازگشت", callback_data=MenuCB(action="buy"))
    kb.adjust(2, 1)
    return kb.as_markup()


def wallets_kb(order_id: int, wallets: list[CryptoWallet]) -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    for w in wallets:
        label = f"{w.symbol} · {w.network}" if w.network else w.symbol
        kb.button(
            text=label,
            callback_data=WalletCB(order_id=order_id, wallet_id=w.id),
        )
    kb.button(
        text="🔙 بازگشت",
        callback_data=PayCB(order_id=order_id, method="back"),
    )
    kb.adjust(2)
    return kb.as_markup()


def copy_card_kb(order_id: int, card_number: str) -> InlineKeyboardMarkup:
    """A keyboard whose main button copies the card number via switch_inline.

    Telegram has no true "copy" button, so we use `copy_text` which is
    supported by recent clients. Falls back gracefully as plain text.
    """
    kb = InlineKeyboardBuilder()
    kb.row(
        InlineKeyboardButton(
            text="📋 کپی شماره کارت",
            copy_text={"text": card_number.replace("-", "").replace(" ", "")},
        )
    )
    kb.row(
        InlineKeyboardButton(
            text="❌ انصراف",
            callback_data=OrderNavCB(action="cancel", order_id=order_id).pack(),
        )
    )
    return kb.as_markup()


def copy_wallet_kb(order_id: int, address: str) -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.row(
        InlineKeyboardButton(text="📋 کپی آدرس کیف پول", copy_text={"text": address})
    )
    kb.row(
        InlineKeyboardButton(
            text="🔙 انتخاب ارز دیگر",
            callback_data=PayCB(order_id=order_id, method="crypto").pack(),
        ),
        InlineKeyboardButton(
            text="❌ انصراف",
            callback_data=OrderNavCB(action="cancel", order_id=order_id).pack(),
        ),
    )
    return kb.as_markup()


def download_apps_kb(links: dict[str, str]) -> InlineKeyboardMarkup:
    """`links` maps platform -> url (android/iphone/windows/mac)."""
    kb = InlineKeyboardBuilder()
    mapping = [
        ("📱 اندروید", links.get("android")),
        ("🍏 آیفون", links.get("iphone")),
        ("🪟 ویندوز", links.get("windows")),
        ("💻 مک", links.get("mac")),
    ]
    for text, url in mapping:
        if url:
            kb.button(text=text, url=url)
    kb.adjust(2)
    return kb.as_markup()


def back_home_kb() -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.button(text="🔙 بازگشت به منو", callback_data=MenuCB(action="home"))
    return kb.as_markup()
