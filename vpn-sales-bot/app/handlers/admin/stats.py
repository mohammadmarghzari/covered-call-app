"""Admin: sales & user statistics."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.types import CallbackQuery

from app.filters import IsAdmin
from app.keyboards.admin_kb import back_admin_kb
from app.keyboards.callbacks import AdminCB
from app.services.container import Repos
from app.services.stats_service import build_stats
from app.utils.formatting import format_price, to_fa_digits

router = Router(name="admin_stats")
router.callback_query.filter(IsAdmin())


@router.callback_query(AdminCB.filter(F.section == "stats"))
async def show_stats(call: CallbackQuery, repos: Repos) -> None:
    s = await build_stats(repos)
    text = (
        "📊 <b>آمار فروش و کاربران</b>\n\n"
        "👥 <b>کاربران</b>\n"
        f"• کل کاربران: {to_fa_digits(s.total_users)}\n"
        f"• کاربران جدید امروز: {to_fa_digits(s.new_users_today)}\n"
        f"• کاربران مسدود: {to_fa_digits(s.banned_users)}\n\n"
        "🧾 <b>سفارش‌ها</b>\n"
        f"• کل سفارش‌ها: {to_fa_digits(s.total_orders)}\n"
        f"• تحویل‌شده: {to_fa_digits(s.completed_orders)}\n"
        f"• در انتظار بررسی: {to_fa_digits(s.pending_orders)}\n\n"
        "💰 <b>فروش</b>\n"
        f"• فروش امروز: {to_fa_digits(s.sales_today)} سفارش — {format_price(s.revenue_today)}\n"
        f"• فروش ۳۰ روز اخیر: {to_fa_digits(s.sales_month)} سفارش — {format_price(s.revenue_month)}\n"
        f"• درآمد کل: <b>{format_price(s.total_revenue)}</b>"
    )
    await call.message.edit_text(text, reply_markup=back_admin_kb("home"))
    await call.answer()
