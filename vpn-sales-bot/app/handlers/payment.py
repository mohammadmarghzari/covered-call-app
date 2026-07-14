"""Payment method selection: bank card & crypto wallets."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.fsm.context import FSMContext
from aiogram.types import BufferedInputFile, CallbackQuery

from app.database.models import PaymentType
from app.keyboards.callbacks import OrderNavCB, PayCB, WalletCB
from app.keyboards.user_kb import (
    copy_card_kb,
    copy_wallet_kb,
    payment_methods_kb,
    wallets_kb,
)
from app.services.container import Repos
from app.services.settings_service import get_card_info
from app.states.states import PurchaseFlow
from app.utils.formatting import format_price
from app.utils.qr import make_qr

router = Router(name="payment")

_RECEIPT_HINT = (
    "\n\n📸 پس از واریز، <b>تصویر رسید پرداخت</b> را همین‌جا ارسال کنید تا سفارش شما بررسی شود."
)


async def _load_order_or_alert(call: CallbackQuery, repos: Repos, order_id: int):
    order = await repos.orders.get(order_id)
    if order is None:
        await call.answer("سفارش یافت نشد. لطفاً دوباره از منو اقدام کنید.", show_alert=True)
        return None
    return order


@router.callback_query(PayCB.filter(F.method == "card"))
async def pay_card(
    call: CallbackQuery, callback_data: PayCB, repos: Repos, state: FSMContext
) -> None:
    order = await _load_order_or_alert(call, repos, callback_data.order_id)
    if order is None:
        return

    card_number, card_holder = await get_card_info(repos)
    await repos.orders.set_payment_method(order.id, PaymentType.CARD, "کارت بانکی")

    await state.set_state(PurchaseFlow.waiting_for_receipt)
    await state.update_data(order_id=order.id)

    text = (
        "💳 <b>پرداخت با کارت بانکی</b>\n\n"
        f"🔢 شماره کارت:\n<code>{card_number}</code>\n\n"
        f"👤 به نام: {card_holder}\n"
        f"💵 مبلغ قابل پرداخت: <b>{format_price(order.price)}</b>"
        f"{_RECEIPT_HINT}"
    )
    await call.message.edit_text(
        text, reply_markup=copy_card_kb(order.id, card_number)
    )
    await call.answer()


@router.callback_query(PayCB.filter(F.method == "crypto"))
async def pay_crypto(
    call: CallbackQuery, callback_data: PayCB, repos: Repos
) -> None:
    order = await _load_order_or_alert(call, repos, callback_data.order_id)
    if order is None:
        return

    wallets = await repos.wallets.list_active()
    if not wallets:
        await call.answer("در حال حاضر کیف پول فعالی موجود نیست.", show_alert=True)
        return

    text = (
        "🪙 <b>پرداخت با ارز دیجیتال</b>\n\n"
        f"💵 مبلغ قابل پرداخت (معادل): <b>{format_price(order.price)}</b>\n\n"
        "لطفاً ارز موردنظر خود را انتخاب کنید:"
    )
    await call.message.edit_text(text, reply_markup=wallets_kb(order.id, wallets))
    await call.answer()


@router.callback_query(PayCB.filter(F.method == "back"))
async def back_to_methods(
    call: CallbackQuery, callback_data: PayCB, repos: Repos, state: FSMContext
) -> None:
    await state.clear()
    order = await _load_order_or_alert(call, repos, callback_data.order_id)
    if order is None:
        return
    await call.message.edit_text(
        "💳 لطفاً روش پرداخت را انتخاب کنید:",
        reply_markup=payment_methods_kb(order.id),
    )
    await call.answer()


@router.callback_query(WalletCB.filter())
async def choose_wallet(
    call: CallbackQuery, callback_data: WalletCB, repos: Repos, state: FSMContext
) -> None:
    order = await _load_order_or_alert(call, repos, callback_data.order_id)
    if order is None:
        return
    wallet = await repos.wallets.get(callback_data.wallet_id)
    if wallet is None or not wallet.is_active:
        await call.answer("این کیف پول دیگر در دسترس نیست.", show_alert=True)
        return

    detail = f"{wallet.symbol} {wallet.network}".strip()
    await repos.orders.set_payment_method(order.id, PaymentType.CRYPTO, detail)

    await state.set_state(PurchaseFlow.waiting_for_receipt)
    await state.update_data(order_id=order.id)

    memo_line = f"\n🏷 ممو/تگ:\n<code>{wallet.memo}</code>" if wallet.memo else ""
    caption = (
        f"🪙 <b>{wallet.symbol} — شبکه {wallet.network}</b>\n\n"
        f"💵 مبلغ قابل پرداخت (معادل): <b>{format_price(order.price)}</b>\n\n"
        "📥 آدرس کیف پول:\n"
        f"<code>{wallet.address}</code>"
        f"{memo_line}"
        f"{_RECEIPT_HINT}"
    )

    qr = make_qr(wallet.address)
    photo = BufferedInputFile(qr.read(), filename="wallet_qr.png")

    # Replace the previous message with a photo (QR) message.
    try:
        await call.message.delete()
    except Exception:
        pass
    await call.message.answer_photo(
        photo=photo,
        caption=caption,
        reply_markup=copy_wallet_kb(order.id, wallet.address),
    )
    await call.answer()


@router.callback_query(OrderNavCB.filter(F.action == "cancel"))
async def cancel_order(
    call: CallbackQuery, callback_data: OrderNavCB, repos: Repos, state: FSMContext
) -> None:
    from app.database.models import OrderStatus
    from app.keyboards.user_kb import back_home_kb

    await state.clear()
    await repos.orders.set_status(callback_data.order_id, OrderStatus.CANCELLED)
    text = "❌ سفارش لغو شد. هر زمان خواستید می‌توانید دوباره از منو خرید کنید."
    try:
        await call.message.edit_text(text, reply_markup=back_home_kb())
    except Exception:
        # message may be a photo (crypto flow) — can't edit text, send fresh.
        try:
            await call.message.delete()
        except Exception:
            pass
        await call.message.answer(text, reply_markup=back_home_kb())
    await call.answer()
