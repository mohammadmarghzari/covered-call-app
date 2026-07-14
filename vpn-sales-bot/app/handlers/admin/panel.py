"""Admin panel entry point and home navigation."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.filters import Command
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message

from app.filters import IsAdmin
from app.keyboards.admin_kb import admin_home_kb
from app.keyboards.callbacks import AdminCB

router = Router(name="admin_panel")
# Guard the whole admin panel in private chats only.
router.message.filter(IsAdmin(), F.chat.type == "private")
router.callback_query.filter(IsAdmin())

_HOME_TEXT = (
    "🛠 <b>پنل مدیریت</b>\n\n"
    "به پنل مدیریت خوش آمدید. از دکمه‌های زیر برای مدیریت ربات استفاده کنید:"
)


@router.message(Command("admin"))
async def cmd_admin(message: Message, state: FSMContext) -> None:
    await state.clear()
    await message.answer(_HOME_TEXT, reply_markup=admin_home_kb())


@router.callback_query(AdminCB.filter(F.section == "home"))
async def admin_home(call: CallbackQuery, state: FSMContext) -> None:
    await state.clear()
    try:
        await call.message.edit_text(_HOME_TEXT, reply_markup=admin_home_kb())
    except Exception:
        await call.message.answer(_HOME_TEXT, reply_markup=admin_home_kb())
    await call.answer()
