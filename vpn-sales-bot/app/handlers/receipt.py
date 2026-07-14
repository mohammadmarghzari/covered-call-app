"""Receipt handling: user sends a payment photo, bot posts it to the admin channel."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.fsm.context import FSMContext
from aiogram.types import Message

from app.config import settings
from app.keyboards.user_kb import back_home_kb
from app.logger import get_logger
from app.services.container import Repos
from app.states.states import PurchaseFlow
from app.utils.formatting import format_datetime, format_price, to_fa_digits

router = Router(name="receipt")
log = get_logger("receipt")


def _admin_caption(order, user) -> str:  # noqa: ANN001
    username = f"@{user.username}" if user.username else "—"
    full_name = " ".join(filter(None, [user.first_name, user.last_name])) or "—"
    return (
        "🧾 <b>سفارش جدید — در انتظار تأیید</b>\n\n"
        f"🆔 شماره سفارش: <code>{to_fa_digits(order.id)}</code>\n"
        f"👤 نام: {full_name}\n"
        f"🔗 یوزرنیم: {username}\n"
        f"🆔 آیدی عددی: <code>{user.telegram_id}</code>\n"
        f"📦 پلن: {order.plan_title}\n"
        f"💵 مبلغ: {format_price(order.price)}\n"
        f"💳 روش پرداخت: {order.payment_detail or '—'}\n"
        f"🕐 زمان: {format_datetime(order.created_at)}\n\n"
        "↩️ برای تحویل، روی همین پیام <b>ریپلای</b> کرده و فقط <b>لینک کانفیگ</b> را بفرستید."
    )


@router.message(PurchaseFlow.waiting_for_receipt, F.photo)
async def receive_receipt(message: Message, repos: Repos, state: FSMContext) -> None:
    data = await state.get_data()
    order_id = data.get("order_id")
    if not order_id:
        await state.clear()
        await message.answer(
            "مشکلی پیش آمد. لطفاً دوباره از منو خرید را شروع کنید.",
            reply_markup=back_home_kb(),
        )
        return

    file_id = message.photo[-1].file_id
    order = await repos.orders.attach_receipt(order_id, file_id)
    if order is None:
        await state.clear()
        await message.answer("سفارش یافت نشد.", reply_markup=back_home_kb())
        return

    user = order.user

    # Post the receipt to the admin channel and remember its message id so the
    # admin's reply can be routed back to this order.
    try:
        sent = await message.bot.send_photo(
            chat_id=settings.admin_channel_id,
            photo=file_id,
            caption=_admin_caption(order, user),
        )
        await repos.orders.set_admin_message_id(order.id, sent.message_id)
        await repos.logs.add("receipt_submitted", user.telegram_id, f"order={order.id}")
    except Exception as exc:  # pragma: no cover - depends on Telegram availability
        log.error("failed_to_post_receipt", error=str(exc), order_id=order.id)
        await message.answer(
            "⚠️ ارسال رسید با خطا مواجه شد. لطفاً چند لحظه بعد دوباره تلاش کنید یا "
            "با پشتیبانی تماس بگیرید."
        )
        return

    await state.clear()
    await message.answer(
        "✅ رسید شما با موفقیت دریافت و برای بررسی ارسال شد.\n\n"
        "⏳ لطفاً منتظر بمانید؛ پس از تأیید پرداخت، کانفیگ به‌صورت خودکار برای شما "
        "ارسال خواهد شد. 🙏",
        reply_markup=back_home_kb(),
    )


@router.message(PurchaseFlow.waiting_for_receipt)
async def wrong_receipt(message: Message) -> None:
    await message.answer(
        "📸 لطفاً <b>تصویر رسید پرداخت</b> را به‌صورت عکس ارسال کنید."
    )
