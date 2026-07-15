"""Start command, main menu, support and tutorial."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.filters import Command, CommandStart
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message

from app.config import settings
from app.keyboards.callbacks import MenuCB
from app.keyboards.user_kb import back_home_kb, main_menu_kb
from app.services.container import Repos
from app.utils.constants import SettingKey

router = Router(name="start")


async def _welcome_text(repos: Repos) -> str:
    return await repos.settings.get(SettingKey.WELCOME_TEXT) or "خوش آمدید!"


@router.message(CommandStart())
async def cmd_start(message: Message, repos: Repos, state: FSMContext) -> None:
    await state.clear()
    await repos.users.get_or_create(
        telegram_id=message.from_user.id,
        username=message.from_user.username,
        first_name=message.from_user.first_name,
        last_name=message.from_user.last_name,
    )
    text = await _welcome_text(repos)
    hint = ""
    if settings.is_admin(message.from_user.id):
        hint = "\n\n🛠 برای ورود به پنل مدیریت دستور /admin را بزنید."
    await message.answer(text + hint, reply_markup=main_menu_kb())


@router.message(Command("id"))
async def cmd_id(message: Message) -> None:
    """Utility: report the numeric user id (and chat id). Handy for setup."""
    lines = [
        "🆔 <b>شناسه‌های عددی شما</b>\n",
        f"👤 User ID شما: <code>{message.from_user.id}</code>",
    ]
    if message.chat.id != message.from_user.id:
        lines.append(f"💬 Chat ID اینجا: <code>{message.chat.id}</code>")
    await message.answer("\n".join(lines))


@router.callback_query(MenuCB.filter(F.action == "home"))
async def go_home(call: CallbackQuery, repos: Repos, state: FSMContext) -> None:
    await state.clear()
    text = await _welcome_text(repos)
    try:
        await call.message.edit_text(text, reply_markup=main_menu_kb())
    except Exception:
        await call.message.answer(text, reply_markup=main_menu_kb())
    await call.answer()


@router.callback_query(MenuCB.filter(F.action == "support"))
async def show_support(call: CallbackQuery, repos: Repos) -> None:
    support = await repos.settings.get(SettingKey.SUPPORT_USERNAME) or "@support"
    text = (
        "📞 <b>پشتیبانی</b>\n\n"
        "در صورت داشتن هرگونه سؤال یا مشکل، از طریق آیدی زیر با ما در ارتباط باشید:\n\n"
        f"👤 {support}\n\n"
        "پاسخگوی شما هستیم. 🙏"
    )
    await call.message.edit_text(text, reply_markup=back_home_kb())
    await call.answer()


@router.callback_query(MenuCB.filter(F.action == "tutorial"))
async def show_tutorial(call: CallbackQuery, repos: Repos) -> None:
    text = await repos.settings.get(SettingKey.TUTORIAL_TEXT) or "آموزش به‌زودی..."
    await call.message.edit_text(text, reply_markup=back_home_kb())
    await call.answer()
