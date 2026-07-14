"""Config delivery: the admin replies to a receipt in the channel with a link,
and the bot forwards everything to the customer automatically.
"""
from __future__ import annotations

from aiogram import Bot, F, Router
from aiogram.types import Message

from app.config import settings
from app.database.models import OrderStatus
from app.keyboards.user_kb import download_apps_kb
from app.logger import get_logger
from app.services.container import Repos
from app.services.settings_service import get_download_links
from app.utils.constants import SettingKey
from app.utils.validators import is_config_link

router = Router(name="admin_delivery")
log = get_logger("delivery")

_REJECT_PREFIXES = ("رد", "/reject", "reject", "❌")


def _in_admin_channel(message: Message) -> bool:
    return message.chat.id == settings.admin_channel_id


async def _deliver_config(bot: Bot, repos: Repos, order, admin_id: int, link: str) -> None:
    """Send the confirmation, config link, photo, text and download buttons."""
    user = order.user
    chat_id = user.telegram_id

    await bot.send_message(chat_id, "✅ <b>پرداخت شما تأیید شد!</b>")

    # Config link on its own line/message for easy copying.
    await bot.send_message(
        chat_id,
        f"🔐 <b>کانفیگ اختصاصی شما:</b>\n\n<code>{link}</code>",
    )

    delivery_text = await repos.settings.get(SettingKey.DELIVERY_TEXT) or "خرید شما تکمیل شد."
    photo_id = await repos.settings.get(SettingKey.DELIVERY_PHOTO_FILE_ID)
    links = await get_download_links(repos)
    keyboard = download_apps_kb(links)

    if photo_id:
        await bot.send_photo(
            chat_id, photo=photo_id, caption=delivery_text, reply_markup=keyboard
        )
    else:
        await bot.send_message(chat_id, delivery_text, reply_markup=keyboard)

    await repos.orders.mark_completed(order.id, link, admin_id)
    await repos.logs.add("config_delivered", admin_id, f"order={order.id}")


async def _handle_admin_reply(message: Message, repos: Repos, bot: Bot) -> None:
    if not _in_admin_channel(message):
        return
    reply = message.reply_to_message
    if reply is None:
        return

    order = await repos.orders.get_by_admin_message(reply.message_id)
    if order is None:
        return  # not a reply to a tracked receipt

    text = (message.text or message.caption or "").strip()

    # Rejection path.
    if text.lower().startswith(_REJECT_PREFIXES) and not is_config_link(text):
        if order.status == OrderStatus.COMPLETED:
            await message.reply("این سفارش قبلاً تحویل شده است.")
            return
        await repos.orders.set_status(order.id, OrderStatus.REJECTED)
        await repos.logs.add("order_rejected", message.from_user.id if message.from_user else None, f"order={order.id}")
        try:
            await bot.send_message(
                order.user.telegram_id,
                "❌ متأسفانه پرداخت شما تأیید نشد.\n\n"
                "در صورتی که مبلغ را واریز کرده‌اید، لطفاً با پشتیبانی تماس بگیرید.",
            )
        except Exception as exc:  # pragma: no cover
            log.warning("failed_notify_reject", error=str(exc))
        await message.reply("سفارش رد شد و به کاربر اطلاع داده شد. ⛔️")
        return

    # Delivery path — must look like a config link.
    if not is_config_link(text):
        await message.reply(
            "⚠️ لطفاً یک لینک کانفیگ معتبر (مثل vless:// یا vmess://) ارسال کنید،\n"
            "یا برای رد کردن سفارش با «رد» ریپلای کنید."
        )
        return

    if order.status == OrderStatus.COMPLETED:
        await message.reply("این سفارش قبلاً تحویل داده شده است. ✅")
        return

    admin_id = message.from_user.id if message.from_user else 0
    try:
        await _deliver_config(bot, repos, order, admin_id, text)
    except Exception as exc:  # pragma: no cover
        log.error("delivery_failed", error=str(exc), order_id=order.id)
        await message.reply(f"❌ ارسال کانفیگ برای کاربر ناموفق بود:\n{exc}")
        return

    await message.reply("✅ کانفیگ با موفقیت برای مشتری ارسال شد.")


# Channel posts (private/public channel) arrive as channel_post updates.
@router.channel_post(F.reply_to_message)
async def on_channel_reply(message: Message, repos: Repos, bot: Bot) -> None:
    await _handle_admin_reply(message, repos, bot)


# If the admin uses a group instead of a channel, replies arrive as messages.
@router.message(F.chat.id == settings.admin_channel_id, F.reply_to_message)
async def on_group_reply(message: Message, repos: Repos, bot: Bot) -> None:
    await _handle_admin_reply(message, repos, bot)
