"""'My orders' view for users."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.types import CallbackQuery

from app.database.models import OrderStatus
from app.keyboards.callbacks import MenuCB
from app.keyboards.user_kb import back_home_kb
from app.services.container import Repos
from app.utils.formatting import format_datetime, format_price, to_fa_digits

router = Router(name="my_orders")

_STATUS_LABELS = {
    OrderStatus.AWAITING_PAYMENT: "⏳ در انتظار پرداخت",
    OrderStatus.UNDER_REVIEW: "🔎 در حال بررسی",
    OrderStatus.COMPLETED: "✅ تحویل شده",
    OrderStatus.REJECTED: "❌ رد شده",
    OrderStatus.CANCELLED: "🚫 لغو شده",
}


@router.callback_query(MenuCB.filter(F.action == "orders"))
async def show_my_orders(call: CallbackQuery, repos: Repos) -> None:
    user = await repos.users.get_by_telegram_id(call.from_user.id)
    orders = await repos.orders.list_by_user(user.id) if user else []

    # Only show orders the user actually acted on (skip abandoned drafts).
    orders = [
        o
        for o in orders
        if o.status != OrderStatus.AWAITING_PAYMENT
    ]

    if not orders:
        await call.message.edit_text(
            "📦 شما هنوز خریدی ثبت نکرده‌اید.\n\n"
            "برای تهیهٔ کانفیگ از منوی اصلی، «🛒 خرید کانفیگ» را انتخاب کنید.",
            reply_markup=back_home_kb(),
        )
        await call.answer()
        return

    lines = ["📦 <b>خریدهای شما:</b>\n"]
    for o in orders:
        lines.append(
            f"🆔 سفارش <code>{to_fa_digits(o.id)}</code> — {o.plan_title}\n"
            f"💵 {format_price(o.price)} · {_STATUS_LABELS.get(o.status, '')}\n"
            f"🕐 {format_datetime(o.created_at)}"
        )
        if o.status == OrderStatus.COMPLETED and o.config_link:
            lines.append(f"🔗 <code>{o.config_link}</code>")
        lines.append("➖➖➖➖➖➖➖➖➖➖")

    await call.message.edit_text("\n".join(lines), reply_markup=back_home_kb())
    await call.answer()
