"""Inline keyboards for the admin panel."""
from __future__ import annotations

from aiogram.types import InlineKeyboardMarkup
from aiogram.utils.keyboard import InlineKeyboardBuilder

from app.database.models import CryptoWallet, Plan
from app.keyboards.callbacks import AdminCB
from app.utils.constants import EDITABLE_TEXT_KEYS, EDITABLE_TEXTS
from app.utils.formatting import format_price, format_volume


def admin_home_kb() -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.button(text="📦 پلن‌ها", callback_data=AdminCB(section="plans"))
    kb.button(text="🪙 کیف پول‌ها", callback_data=AdminCB(section="wallets"))
    kb.button(text="💳 کارت بانکی", callback_data=AdminCB(section="card"))
    kb.button(text="🧾 سفارش‌ها", callback_data=AdminCB(section="orders"))
    kb.button(text="🔎 جستجوی سفارش", callback_data=AdminCB(section="orders", action="search"))
    kb.button(text="📊 آمار", callback_data=AdminCB(section="stats"))
    kb.button(text="👥 کاربران", callback_data=AdminCB(section="users"))
    kb.button(text="📢 پیام همگانی", callback_data=AdminCB(section="broadcast"))
    kb.button(text="✏️ ویرایش متن‌ها", callback_data=AdminCB(section="texts"))
    kb.button(text="🖼 عکس تحویل", callback_data=AdminCB(section="settings", action="photo"))
    kb.adjust(2, 2, 1, 2, 2, 1)
    return kb.as_markup()


def _back(kb: InlineKeyboardBuilder, section: str = "home") -> None:
    kb.button(
        text="🔙 بازگشت",
        callback_data=AdminCB(section="home") if section == "home" else AdminCB(section=section),
    )


def plans_admin_kb(plans: list[Plan]) -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    for p in plans:
        status = "🟢" if p.is_active else "🔴"
        kb.button(
            text=f"{status} {p.title} — {format_price(p.price)}",
            callback_data=AdminCB(section="plans", action="view", item_id=p.id),
        )
    kb.button(text="➕ افزودن پلن", callback_data=AdminCB(section="plans", action="add"))
    kb.button(text="🔙 بازگشت", callback_data=AdminCB(section="home"))
    kb.adjust(1)
    return kb.as_markup()


def plan_detail_kb(plan: Plan) -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.button(text="✏️ عنوان", callback_data=AdminCB(section="plans", action="edit_title", item_id=plan.id))
    kb.button(text="💵 قیمت", callback_data=AdminCB(section="plans", action="edit_price", item_id=plan.id))
    kb.button(text="📦 حجم", callback_data=AdminCB(section="plans", action="edit_volume", item_id=plan.id))
    kb.button(text="📅 مدت", callback_data=AdminCB(section="plans", action="edit_duration", item_id=plan.id))
    kb.button(text="📝 توضیحات", callback_data=AdminCB(section="plans", action="edit_desc", item_id=plan.id))
    toggle = "🔴 غیرفعال‌سازی" if plan.is_active else "🟢 فعال‌سازی"
    kb.button(text=toggle, callback_data=AdminCB(section="plans", action="toggle", item_id=plan.id))
    kb.button(text="🗑 حذف", callback_data=AdminCB(section="plans", action="del", item_id=plan.id))
    kb.button(text="🔙 بازگشت", callback_data=AdminCB(section="plans"))
    kb.adjust(2, 2, 1, 1, 1, 1)
    return kb.as_markup()


def wallets_admin_kb(wallets: list[CryptoWallet]) -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    for w in wallets:
        status = "🟢" if w.is_active else "🔴"
        kb.button(
            text=f"{status} {w.symbol} · {w.network}",
            callback_data=AdminCB(section="wallets", action="view", item_id=w.id),
        )
    kb.button(text="➕ افزودن کیف پول", callback_data=AdminCB(section="wallets", action="add"))
    kb.button(text="🔙 بازگشت", callback_data=AdminCB(section="home"))
    kb.adjust(1)
    return kb.as_markup()


def wallet_detail_kb(wallet: CryptoWallet) -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    toggle = "🔴 غیرفعال‌سازی" if wallet.is_active else "🟢 فعال‌سازی"
    kb.button(text=toggle, callback_data=AdminCB(section="wallets", action="toggle", item_id=wallet.id))
    kb.button(text="🗑 حذف", callback_data=AdminCB(section="wallets", action="del", item_id=wallet.id))
    kb.button(text="🔙 بازگشت", callback_data=AdminCB(section="wallets"))
    kb.adjust(2, 1)
    return kb.as_markup()


def card_admin_kb() -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.button(text="✏️ شماره کارت", callback_data=AdminCB(section="card", action="edit_number"))
    kb.button(text="✏️ نام صاحب کارت", callback_data=AdminCB(section="card", action="edit_holder"))
    kb.button(text="🔙 بازگشت", callback_data=AdminCB(section="home"))
    kb.adjust(2, 1)
    return kb.as_markup()


def texts_admin_kb() -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    for index, key in enumerate(EDITABLE_TEXT_KEYS):
        kb.button(
            text=f"✏️ {EDITABLE_TEXTS[key]}",
            callback_data=AdminCB(section="texts", action="edit", item_id=index),
        )
    kb.button(text="🔙 بازگشت", callback_data=AdminCB(section="home"))
    kb.adjust(1)
    return kb.as_markup()


def users_admin_kb() -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.button(text="🚫 بلاک کاربر", callback_data=AdminCB(section="users", action="ban"))
    kb.button(text="✅ رفع بلاک", callback_data=AdminCB(section="users", action="unban"))
    kb.button(text="🔙 بازگشت", callback_data=AdminCB(section="home"))
    kb.adjust(2, 1)
    return kb.as_markup()


def back_admin_kb(section: str = "home") -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.button(text="🔙 بازگشت", callback_data=AdminCB(section=section))
    return kb.as_markup()


def confirm_broadcast_kb() -> InlineKeyboardMarkup:
    kb = InlineKeyboardBuilder()
    kb.button(text="✅ ارسال به همه", callback_data=AdminCB(section="broadcast", action="send"))
    kb.button(text="❌ لغو", callback_data=AdminCB(section="home"))
    kb.adjust(2)
    return kb.as_markup()
