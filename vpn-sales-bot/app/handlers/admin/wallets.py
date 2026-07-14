"""Admin: manage crypto wallets."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message

from app.filters import IsAdmin
from app.keyboards.admin_kb import (
    back_admin_kb,
    wallet_detail_kb,
    wallets_admin_kb,
)
from app.keyboards.callbacks import AdminCB
from app.services.container import Repos
from app.states.states import AdminWalletFSM

router = Router(name="admin_wallets")
router.message.filter(IsAdmin(), F.chat.type == "private")
router.callback_query.filter(IsAdmin())


def _wallet_text(w) -> str:  # noqa: ANN001
    status = "🟢 فعال" if w.is_active else "🔴 غیرفعال"
    memo = w.memo or "—"
    return (
        f"🪙 <b>{w.symbol} — {w.network}</b>\n\n"
        f"📥 آدرس:\n<code>{w.address}</code>\n\n"
        f"🏷 ممو/تگ: {memo}\n"
        f"وضعیت: {status}"
    )


@router.callback_query(AdminCB.filter((F.section == "wallets") & (F.action == "open")))
async def list_wallets(call: CallbackQuery, repos: Repos, state: FSMContext) -> None:
    await state.clear()
    wallets = await repos.wallets.list_all()
    text = "🪙 <b>مدیریت کیف پول‌ها</b>\n\nبرای مدیریت هر کیف پول روی آن بزنید."
    if not wallets:
        text += "\n\n(هنوز کیف پولی ثبت نشده است.)"
    await call.message.edit_text(text, reply_markup=wallets_admin_kb(wallets))
    await call.answer()


@router.callback_query(AdminCB.filter((F.section == "wallets") & (F.action == "view")))
async def view_wallet(call: CallbackQuery, callback_data: AdminCB, repos: Repos) -> None:
    w = await repos.wallets.get(callback_data.item_id)
    if w is None:
        await call.answer("کیف پول یافت نشد.", show_alert=True)
        return
    await call.message.edit_text(_wallet_text(w), reply_markup=wallet_detail_kb(w))
    await call.answer()


@router.callback_query(AdminCB.filter((F.section == "wallets") & (F.action == "toggle")))
async def toggle_wallet(call: CallbackQuery, callback_data: AdminCB, repos: Repos) -> None:
    w = await repos.wallets.get(callback_data.item_id)
    if w is None:
        await call.answer("کیف پول یافت نشد.", show_alert=True)
        return
    w.is_active = not w.is_active
    await repos.session.flush()
    await call.message.edit_text(_wallet_text(w), reply_markup=wallet_detail_kb(w))
    await call.answer("وضعیت تغییر کرد.")


@router.callback_query(AdminCB.filter((F.section == "wallets") & (F.action == "del")))
async def delete_wallet(call: CallbackQuery, callback_data: AdminCB, repos: Repos) -> None:
    await repos.wallets.delete(callback_data.item_id)
    await repos.logs.add("wallet_deleted", call.from_user.id, f"wallet={callback_data.item_id}")
    wallets = await repos.wallets.list_all()
    await call.message.edit_text("🗑 کیف پول حذف شد.", reply_markup=wallets_admin_kb(wallets))
    await call.answer()


# --- Add wallet -----------------------------------------------------------
@router.callback_query(AdminCB.filter((F.section == "wallets") & (F.action == "add")))
async def add_wallet_start(call: CallbackQuery, state: FSMContext) -> None:
    await state.set_state(AdminWalletFSM.symbol)
    await call.message.edit_text(
        "➕ <b>افزودن کیف پول</b>\n\n۱/۴ — نماد ارز را وارد کنید (مثلاً USDT یا BTC):",
        reply_markup=back_admin_kb("wallets"),
    )
    await call.answer()


@router.message(AdminWalletFSM.symbol)
async def add_wallet_symbol(message: Message, state: FSMContext) -> None:
    await state.update_data(symbol=message.text.strip())
    await state.set_state(AdminWalletFSM.network)
    await message.answer("۲/۴ — شبکه را وارد کنید (مثلاً TRC20، ERC20، BEP20، TON):")


@router.message(AdminWalletFSM.network)
async def add_wallet_network(message: Message, state: FSMContext) -> None:
    await state.update_data(network=message.text.strip())
    await state.set_state(AdminWalletFSM.address)
    await message.answer("۳/۴ — آدرس کیف پول را وارد کنید:")


@router.message(AdminWalletFSM.address)
async def add_wallet_address(message: Message, state: FSMContext) -> None:
    await state.update_data(address=message.text.strip())
    await state.set_state(AdminWalletFSM.memo)
    await message.answer("۴/۴ — ممو/تگ را وارد کنید (اختیاری). برای رد کردن «-» بفرستید:")


@router.message(AdminWalletFSM.memo)
async def add_wallet_memo(message: Message, repos: Repos, state: FSMContext) -> None:
    memo = message.text.strip()
    if memo in ("-", "—", "خالی"):
        memo = None
    data = await state.get_data()
    wallet = await repos.wallets.create(
        symbol=data["symbol"], network=data["network"], address=data["address"], memo=memo
    )
    await repos.logs.add("wallet_created", message.from_user.id, f"wallet={wallet.id}")
    await state.clear()
    wallets = await repos.wallets.list_all()
    await message.answer(
        f"✅ کیف پول {wallet.symbol} · {wallet.network} افزوده شد.",
        reply_markup=wallets_admin_kb(wallets),
    )
