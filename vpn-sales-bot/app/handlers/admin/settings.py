"""Admin: edit card info, texts and the delivery photo."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message

from app.filters import IsAdmin
from app.keyboards.admin_kb import (
    back_admin_kb,
    card_admin_kb,
    texts_admin_kb,
)
from app.keyboards.callbacks import AdminCB
from app.services.container import Repos
from app.services.settings_service import get_card_info
from app.states.states import AdminSettingFSM
from app.utils.constants import EDITABLE_TEXT_KEYS, EDITABLE_TEXTS, SettingKey
from app.utils.validators import normalize_card_number

router = Router(name="admin_settings")
router.message.filter(IsAdmin(), F.chat.type == "private")
router.callback_query.filter(IsAdmin())


# --- Bank card ------------------------------------------------------------
@router.callback_query(AdminCB.filter((F.section == "card") & (F.action == "open")))
async def card_home(call: CallbackQuery, repos: Repos, state: FSMContext) -> None:
    await state.clear()
    number, holder = await get_card_info(repos)
    text = (
        "💳 <b>اطلاعات کارت بانکی</b>\n\n"
        f"🔢 شماره کارت: <code>{number}</code>\n"
        f"👤 صاحب کارت: {holder}"
    )
    await call.message.edit_text(text, reply_markup=card_admin_kb())
    await call.answer()


@router.callback_query(AdminCB.filter((F.section == "card") & (F.action == "edit_number")))
async def card_edit_number(call: CallbackQuery, state: FSMContext) -> None:
    await state.set_state(AdminSettingFSM.waiting_value)
    await state.update_data(key=SettingKey.CARD_NUMBER, is_card=True)
    await call.message.edit_text("🔢 شماره کارت جدید را وارد کنید:", reply_markup=back_admin_kb("card"))
    await call.answer()


@router.callback_query(AdminCB.filter((F.section == "card") & (F.action == "edit_holder")))
async def card_edit_holder(call: CallbackQuery, state: FSMContext) -> None:
    await state.set_state(AdminSettingFSM.waiting_value)
    await state.update_data(key=SettingKey.CARD_HOLDER, is_card=False)
    await call.message.edit_text("👤 نام صاحب کارت جدید را وارد کنید:", reply_markup=back_admin_kb("card"))
    await call.answer()


# --- Texts ----------------------------------------------------------------
@router.callback_query(AdminCB.filter((F.section == "texts") & (F.action == "open")))
async def texts_home(call: CallbackQuery, state: FSMContext) -> None:
    await state.clear()
    await call.message.edit_text(
        "✏️ <b>ویرایش متن‌ها و لینک‌ها</b>\n\nموردی که می‌خواهید ویرایش کنید را انتخاب کنید:",
        reply_markup=texts_admin_kb(),
    )
    await call.answer()


@router.callback_query(AdminCB.filter((F.section == "texts") & (F.action == "edit")))
async def text_edit_start(call: CallbackQuery, callback_data: AdminCB, repos: Repos, state: FSMContext) -> None:
    index = callback_data.item_id
    if index < 0 or index >= len(EDITABLE_TEXT_KEYS):
        await call.answer("مورد نامعتبر.", show_alert=True)
        return
    key = EDITABLE_TEXT_KEYS[index]
    current = await repos.settings.get(key) or "—"
    await state.set_state(AdminSettingFSM.waiting_value)
    await state.update_data(key=key, is_card=False)
    await call.message.edit_text(
        f"✏️ <b>{EDITABLE_TEXTS[key]}</b>\n\n"
        f"مقدار فعلی:\n<code>{current}</code>\n\n"
        "مقدار جدید را ارسال کنید:",
        reply_markup=back_admin_kb("texts"),
    )
    await call.answer()


@router.message(AdminSettingFSM.waiting_value)
async def value_apply(message: Message, repos: Repos, state: FSMContext) -> None:
    data = await state.get_data()
    key = data["key"]
    value = message.text.strip() if message.text else ""
    if not value:
        await message.answer("مقدار نمی‌تواند خالی باشد.")
        return
    if data.get("is_card"):
        value = normalize_card_number(value)
    await repos.settings.set(key, value)
    await repos.logs.add("setting_updated", message.from_user.id, f"key={key}")
    await state.clear()
    await message.answer("✅ با موفقیت به‌روزرسانی شد.", reply_markup=back_admin_kb("home"))


# --- Delivery photo -------------------------------------------------------
@router.callback_query(AdminCB.filter((F.section == "settings") & (F.action == "photo")))
async def photo_start(call: CallbackQuery, repos: Repos, state: FSMContext) -> None:
    await state.set_state(AdminSettingFSM.waiting_photo)
    current = await repos.settings.get(SettingKey.DELIVERY_PHOTO_FILE_ID)
    status = "تنظیم شده ✅" if current else "تنظیم نشده"
    await call.message.edit_text(
        "🖼 <b>عکس تحویل کانفیگ</b>\n\n"
        f"وضعیت فعلی: {status}\n\n"
        "عکسی که پس از تأیید خرید همراه کانفیگ ارسال می‌شود را بفرستید.\n"
        "برای حذف عکس، کلمهٔ «حذف» را بفرستید.",
        reply_markup=back_admin_kb("home"),
    )
    await call.answer()


@router.message(AdminSettingFSM.waiting_photo, F.photo)
async def photo_apply(message: Message, repos: Repos, state: FSMContext) -> None:
    file_id = message.photo[-1].file_id
    await repos.settings.set(SettingKey.DELIVERY_PHOTO_FILE_ID, file_id)
    await repos.logs.add("delivery_photo_set", message.from_user.id)
    await state.clear()
    await message.answer("✅ عکس تحویل ذخیره شد.", reply_markup=back_admin_kb("home"))


@router.message(AdminSettingFSM.waiting_photo, F.text.casefold() == "حذف")
async def photo_clear(message: Message, repos: Repos, state: FSMContext) -> None:
    await repos.settings.set(SettingKey.DELIVERY_PHOTO_FILE_ID, "")
    await state.clear()
    await message.answer("🗑 عکس تحویل حذف شد.", reply_markup=back_admin_kb("home"))


@router.message(AdminSettingFSM.waiting_photo)
async def photo_wrong(message: Message) -> None:
    await message.answer("لطفاً یک عکس ارسال کنید یا «حذف» را بفرستید.")
