"""Admin: browse and search orders."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message
from aiogram.utils.keyboard import InlineKeyboardBuilder

from app.database.models import OrderStatus
from app.filters import IsAdmin
from app.keyboards.admin_kb import back_admin_kb
from app.keyboards.callbacks import AdminCB
from app.services.container import Repos
from app.states.states import AdminSearchFSM
from app.utils.formatting import format_datetime, format_price, to_fa_digits

router = Router(name="admin_orders")
router.message.filter(IsAdmin(), F.chat.type == "private")
router.callback_query.filter(IsAdmin())

_PAGE_SIZE = 8

_STATUS = {
    OrderStatus.AWAITING_PAYMENT: "⏳ در انتظار پرداخت",
    OrderStatus.UNDER_REVIEW: "🔎 در حال بررسی",
    OrderStatus.COMPLETED: "✅ تحویل شده",
    OrderStatus.REJECTED: "❌ رد شده",
    OrderStatus.CANCELLED: "🚫 لغو شده",
}


def _order_line(o) -> str:  # noqa: ANN001
    user = o.user
    username = f"@{user.username}" if user and user.username else (str(user.telegram_id) if user else "—")
    return (
        f"🆔 <code>{to_fa_digits(o.id)}</code> · {o.plan_title} · {format_price(o.price)}\n"
        f"👤 {username} · {_STATUS.get(o.status, '')}\n"
        f"🕐 {format_datetime(o.created_at)}"
    )


def _pager_kb(page: int, has_next: bool):
    kb = InlineKeyboardBuilder()
    if page > 0:
        kb.button(text="⬅️ قبلی", callback_data=AdminCB(section="orders", action="list", page=page - 1))
    if has_next:
        kb.button(text="بعدی ➡️", callback_data=AdminCB(section="orders", action="list", page=page + 1))
    kb.button(text="🔙 بازگشت", callback_data=AdminCB(section="home"))
    kb.adjust(2, 1)
    return kb.as_markup()


@router.callback_query(AdminCB.filter((F.section == "orders") & (F.action.in_({"open", "list"}))))
async def list_orders(call: CallbackQuery, callback_data: AdminCB, repos: Repos, state: FSMContext) -> None:
    await state.clear()
    page = callback_data.page
    orders = await repos.orders.list_recent(limit=_PAGE_SIZE + 1, offset=page * _PAGE_SIZE)
    has_next = len(orders) > _PAGE_SIZE
    orders = orders[:_PAGE_SIZE]

    if not orders and page == 0:
        await call.message.edit_text("هنوز سفارشی ثبت نشده است.", reply_markup=back_admin_kb("home"))
        await call.answer()
        return

    header = f"🧾 <b>سفارش‌ها</b> — صفحهٔ {to_fa_digits(page + 1)}\n\n"
    body = "\n➖➖➖➖➖➖➖➖➖➖\n".join(_order_line(o) for o in orders)
    await call.message.edit_text(header + body, reply_markup=_pager_kb(page, has_next))
    await call.answer()


# --- Search ---------------------------------------------------------------
@router.callback_query(AdminCB.filter((F.section == "orders") & (F.action == "search")))
async def search_start(call: CallbackQuery, state: FSMContext) -> None:
    await state.set_state(AdminSearchFSM.waiting_query)
    await call.message.edit_text(
        "🔎 <b>جستجوی سفارش</b>\n\n"
        "شمارهٔ سفارش، آیدی عددی کاربر یا یوزرنیم (@username) را ارسال کنید:",
        reply_markup=back_admin_kb("home"),
    )
    await call.answer()


@router.message(AdminSearchFSM.waiting_query)
async def search_apply(message: Message, repos: Repos, state: FSMContext) -> None:
    query = message.text.strip()
    orders = await repos.orders.search(query)
    await state.clear()
    if not orders:
        await message.answer("نتیجه‌ای یافت نشد.", reply_markup=back_admin_kb("home"))
        return
    body = "\n➖➖➖➖➖➖➖➖➖➖\n".join(_order_line(o) for o in orders)
    await message.answer(
        f"🔎 <b>نتایج جستجو</b> ({to_fa_digits(len(orders))} مورد):\n\n" + body,
        reply_markup=back_admin_kb("home"),
    )
