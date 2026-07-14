"""Admin: user management (ban / unban)."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message

from app.filters import IsAdmin
from app.keyboards.admin_kb import back_admin_kb, users_admin_kb
from app.keyboards.callbacks import AdminCB
from app.services.container import Repos
from app.states.states import AdminUserFSM
from app.utils.validators import parse_positive_int

router = Router(name="admin_users")
router.message.filter(IsAdmin(), F.chat.type == "private")
router.callback_query.filter(IsAdmin())


@router.callback_query(AdminCB.filter((F.section == "users") & (F.action == "open")))
async def users_home(call: CallbackQuery, repos: Repos, state: FSMContext) -> None:
    await state.clear()
    total = await repos.users.count()
    banned = await repos.users.count_banned()
    from app.utils.formatting import to_fa_digits

    text = (
        "👥 <b>مدیریت کاربران</b>\n\n"
        f"• کل کاربران: {to_fa_digits(total)}\n"
        f"• مسدود‌شده‌ها: {to_fa_digits(banned)}"
    )
    await call.message.edit_text(text, reply_markup=users_admin_kb())
    await call.answer()


@router.callback_query(AdminCB.filter((F.section == "users") & (F.action == "ban")))
async def ban_start(call: CallbackQuery, state: FSMContext) -> None:
    await state.set_state(AdminUserFSM.waiting_ban_id)
    await call.message.edit_text(
        "🚫 آیدی عددی کاربری که می‌خواهید مسدود شود را ارسال کنید:",
        reply_markup=back_admin_kb("users"),
    )
    await call.answer()


@router.callback_query(AdminCB.filter((F.section == "users") & (F.action == "unban")))
async def unban_start(call: CallbackQuery, state: FSMContext) -> None:
    await state.set_state(AdminUserFSM.waiting_unban_id)
    await call.message.edit_text(
        "✅ آیدی عددی کاربری که می‌خواهید رفع مسدودیت شود را ارسال کنید:",
        reply_markup=back_admin_kb("users"),
    )
    await call.answer()


@router.message(AdminUserFSM.waiting_ban_id)
async def ban_apply(message: Message, repos: Repos, state: FSMContext) -> None:
    tid = parse_positive_int(message.text)
    await state.clear()
    if tid is None:
        await message.answer("آیدی نامعتبر است.", reply_markup=users_admin_kb())
        return
    ok = await repos.users.set_banned(tid, True)
    await repos.logs.add("user_banned", message.from_user.id, f"user={tid}")
    msg = "🚫 کاربر مسدود شد." if ok else "کاربری با این آیدی یافت نشد."
    await message.answer(msg, reply_markup=users_admin_kb())


@router.message(AdminUserFSM.waiting_unban_id)
async def unban_apply(message: Message, repos: Repos, state: FSMContext) -> None:
    tid = parse_positive_int(message.text)
    await state.clear()
    if tid is None:
        await message.answer("آیدی نامعتبر است.", reply_markup=users_admin_kb())
        return
    ok = await repos.users.set_banned(tid, False)
    await repos.logs.add("user_unbanned", message.from_user.id, f"user={tid}")
    msg = "✅ مسدودیت کاربر برداشته شد." if ok else "کاربری با این آیدی یافت نشد."
    await message.answer(msg, reply_markup=users_admin_kb())
