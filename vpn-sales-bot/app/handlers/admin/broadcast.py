"""Admin: broadcast a message to all users."""
from __future__ import annotations

import asyncio

from aiogram import Bot, F, Router
from aiogram.exceptions import TelegramForbiddenError, TelegramRetryAfter
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message

from app.filters import IsAdmin
from app.keyboards.admin_kb import back_admin_kb, confirm_broadcast_kb
from app.keyboards.callbacks import AdminCB
from app.logger import get_logger
from app.services.container import Repos
from app.states.states import AdminBroadcastFSM
from app.utils.formatting import to_fa_digits

router = Router(name="admin_broadcast")
router.message.filter(IsAdmin(), F.chat.type == "private")
router.callback_query.filter(IsAdmin())
log = get_logger("broadcast")


@router.callback_query(AdminCB.filter((F.section == "broadcast") & (F.action == "open")))
async def broadcast_start(call: CallbackQuery, state: FSMContext) -> None:
    await state.set_state(AdminBroadcastFSM.waiting_content)
    await call.message.edit_text(
        "📢 <b>پیام همگانی</b>\n\n"
        "پیامی که می‌خواهید برای همهٔ کاربران ارسال شود را بفرستید "
        "(متن، عکس، ویدیو و ... پشتیبانی می‌شود):",
        reply_markup=back_admin_kb("home"),
    )
    await call.answer()


@router.message(AdminBroadcastFSM.waiting_content)
async def broadcast_preview(message: Message, state: FSMContext) -> None:
    await state.update_data(from_chat_id=message.chat.id, message_id=message.message_id)
    await state.set_state(AdminBroadcastFSM.confirm)
    await message.answer(
        "👆 پیام بالا برای همهٔ کاربران ارسال خواهد شد. تأیید می‌کنید؟",
        reply_markup=confirm_broadcast_kb(),
    )


@router.callback_query(
    AdminBroadcastFSM.confirm,
    AdminCB.filter((F.section == "broadcast") & (F.action == "send")),
)
async def broadcast_send(call: CallbackQuery, repos: Repos, state: FSMContext, bot: Bot) -> None:
    data = await state.get_data()
    await state.clear()
    from_chat_id = data["from_chat_id"]
    message_id = data["message_id"]

    user_ids = await repos.users.all_active_ids()
    await repos.logs.add("broadcast_started", call.from_user.id, f"targets={len(user_ids)}")
    await call.message.edit_text(
        f"⏳ در حال ارسال به {to_fa_digits(len(user_ids))} کاربر...",
    )
    await call.answer()

    sent, failed = 0, 0
    for uid in user_ids:
        try:
            await bot.copy_message(chat_id=uid, from_chat_id=from_chat_id, message_id=message_id)
            sent += 1
        except TelegramRetryAfter as exc:
            await asyncio.sleep(exc.retry_after)
            try:
                await bot.copy_message(chat_id=uid, from_chat_id=from_chat_id, message_id=message_id)
                sent += 1
            except Exception:
                failed += 1
        except TelegramForbiddenError:
            failed += 1  # user blocked the bot
        except Exception as exc:  # pragma: no cover
            failed += 1
            log.warning("broadcast_send_failed", user=uid, error=str(exc))
        # Stay well under Telegram's ~30 msg/sec limit.
        await asyncio.sleep(0.05)

    await bot.send_message(
        call.from_user.id,
        f"✅ ارسال پیام همگانی پایان یافت.\n\n"
        f"• موفق: {to_fa_digits(sent)}\n"
        f"• ناموفق: {to_fa_digits(failed)}",
        reply_markup=back_admin_kb("home"),
    )
