"""Admin: create / edit / delete plans."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery, Message

from app.filters import IsAdmin
from app.keyboards.admin_kb import (
    back_admin_kb,
    plan_detail_kb,
    plans_admin_kb,
)
from app.keyboards.callbacks import AdminCB
from app.services.container import Repos
from app.states.states import AdminPlanFSM
from app.utils.formatting import (
    format_duration,
    format_price,
    format_volume,
    to_fa_digits,
)
from app.utils.validators import parse_positive_int

router = Router(name="admin_plans")
router.message.filter(IsAdmin(), F.chat.type == "private")
router.callback_query.filter(IsAdmin())


def _plan_detail_text(plan) -> str:  # noqa: ANN001
    status = "🟢 فعال" if plan.is_active else "🔴 غیرفعال"
    desc = plan.description or "—"
    return (
        f"📦 <b>پلن #{to_fa_digits(plan.id)}</b>\n\n"
        f"🏷 عنوان: {plan.title}\n"
        f"💵 قیمت: {format_price(plan.price)}\n"
        f"📦 حجم: {format_volume(plan.volume_gb)}\n"
        f"📅 مدت: {format_duration(plan.duration_days)}\n"
        f"📝 توضیحات: {desc}\n"
        f"وضعیت: {status}"
    )


@router.callback_query(AdminCB.filter((F.section == "plans") & (F.action == "open")))
async def list_plans(call: CallbackQuery, repos: Repos, state: FSMContext) -> None:
    await state.clear()
    plans = await repos.plans.list_all()
    text = "📦 <b>مدیریت پلن‌ها</b>\n\nبرای مشاهده و ویرایش، روی هر پلن بزنید."
    if not plans:
        text += "\n\n(هنوز پلنی ثبت نشده است.)"
    await call.message.edit_text(text, reply_markup=plans_admin_kb(plans))
    await call.answer()


@router.callback_query(AdminCB.filter((F.section == "plans") & (F.action == "view")))
async def view_plan(call: CallbackQuery, callback_data: AdminCB, repos: Repos) -> None:
    plan = await repos.plans.get(callback_data.item_id)
    if plan is None:
        await call.answer("پلن یافت نشد.", show_alert=True)
        return
    await call.message.edit_text(_plan_detail_text(plan), reply_markup=plan_detail_kb(plan))
    await call.answer()


@router.callback_query(AdminCB.filter((F.section == "plans") & (F.action == "toggle")))
async def toggle_plan(call: CallbackQuery, callback_data: AdminCB, repos: Repos) -> None:
    plan = await repos.plans.get(callback_data.item_id)
    if plan is None:
        await call.answer("پلن یافت نشد.", show_alert=True)
        return
    plan.is_active = not plan.is_active
    await repos.session.flush()
    await call.message.edit_text(_plan_detail_text(plan), reply_markup=plan_detail_kb(plan))
    await call.answer("وضعیت پلن تغییر کرد.")


@router.callback_query(AdminCB.filter((F.section == "plans") & (F.action == "del")))
async def delete_plan(call: CallbackQuery, callback_data: AdminCB, repos: Repos) -> None:
    ok = await repos.plans.delete(callback_data.item_id)
    await repos.logs.add("plan_deleted", call.from_user.id, f"plan={callback_data.item_id}")
    plans = await repos.plans.list_all()
    await call.message.edit_text(
        "🗑 پلن حذف شد." if ok else "پلن یافت نشد.",
        reply_markup=plans_admin_kb(plans),
    )
    await call.answer()


# --- Add plan (multi-step FSM) -------------------------------------------
@router.callback_query(AdminCB.filter((F.section == "plans") & (F.action == "add")))
async def add_plan_start(call: CallbackQuery, state: FSMContext) -> None:
    await state.set_state(AdminPlanFSM.title)
    await call.message.edit_text(
        "➕ <b>افزودن پلن جدید</b>\n\n۱/۵ — عنوان پلن را ارسال کنید (مثلاً: ۱۰ گیگ یک‌ماهه):",
        reply_markup=back_admin_kb("plans"),
    )
    await call.answer()


@router.message(AdminPlanFSM.title)
async def add_plan_title(message: Message, state: FSMContext) -> None:
    await state.update_data(title=message.text.strip())
    await state.set_state(AdminPlanFSM.volume)
    await message.answer("۲/۵ — حجم پلن به گیگابایت را وارد کنید (۰ برای نامحدود):")


@router.message(AdminPlanFSM.volume)
async def add_plan_volume(message: Message, state: FSMContext) -> None:
    volume = parse_positive_int(message.text)
    if volume is None:
        await message.answer("لطفاً یک عدد صحیح وارد کنید.")
        return
    await state.update_data(volume=volume)
    await state.set_state(AdminPlanFSM.duration)
    await message.answer("۳/۵ — مدت اعتبار به روز را وارد کنید (مثلاً ۳۰):")


@router.message(AdminPlanFSM.duration)
async def add_plan_duration(message: Message, state: FSMContext) -> None:
    duration = parse_positive_int(message.text)
    if duration is None:
        await message.answer("لطفاً یک عدد صحیح وارد کنید.")
        return
    await state.update_data(duration=duration)
    await state.set_state(AdminPlanFSM.price)
    await message.answer("۴/۵ — قیمت پلن به تومان را وارد کنید (مثلاً ۱۲۰۰۰۰):")


@router.message(AdminPlanFSM.price)
async def add_plan_price(message: Message, state: FSMContext) -> None:
    price = parse_positive_int(message.text)
    if price is None:
        await message.answer("لطفاً یک عدد صحیح وارد کنید.")
        return
    await state.update_data(price=price)
    await state.set_state(AdminPlanFSM.description)
    await message.answer(
        "۵/۵ — توضیحات پلن را وارد کنید (اختیاری). برای رد کردن، «-» بفرستید:"
    )


@router.message(AdminPlanFSM.description)
async def add_plan_description(message: Message, repos: Repos, state: FSMContext) -> None:
    desc = message.text.strip()
    if desc in ("-", "—", "خالی"):
        desc = None
    data = await state.get_data()
    plan = await repos.plans.create(
        title=data["title"],
        volume_gb=data["volume"],
        duration_days=data["duration"],
        price=data["price"],
        description=desc,
    )
    await repos.logs.add("plan_created", message.from_user.id, f"plan={plan.id}")
    await state.clear()
    plans = await repos.plans.list_all()
    await message.answer(
        f"✅ پلن «{plan.title}» با موفقیت افزوده شد.",
        reply_markup=plans_admin_kb(plans),
    )


# --- Edit a single field --------------------------------------------------
_EDIT_PROMPTS = {
    "edit_title": ("title", "عنوان جدید را وارد کنید:"),
    "edit_price": ("price", "قیمت جدید (تومان) را وارد کنید:"),
    "edit_volume": ("volume", "حجم جدید (گیگابایت، ۰ برای نامحدود) را وارد کنید:"),
    "edit_duration": ("duration", "مدت جدید (روز) را وارد کنید:"),
    "edit_desc": ("description", "توضیحات جدید را وارد کنید («-» برای حذف):"),
}


@router.callback_query(AdminCB.filter((F.section == "plans") & F.action.startswith("edit_")))
async def edit_plan_start(call: CallbackQuery, callback_data: AdminCB, state: FSMContext) -> None:
    field, prompt = _EDIT_PROMPTS[callback_data.action]
    await state.set_state(AdminPlanFSM.edit_value)
    await state.update_data(edit_field=field, plan_id=callback_data.item_id)
    await call.message.edit_text(f"✏️ {prompt}", reply_markup=back_admin_kb("plans"))
    await call.answer()


@router.message(AdminPlanFSM.edit_value)
async def edit_plan_apply(message: Message, repos: Repos, state: FSMContext) -> None:
    data = await state.get_data()
    field = data["edit_field"]
    plan = await repos.plans.get(data["plan_id"])
    if plan is None:
        await state.clear()
        await message.answer("پلن یافت نشد.", reply_markup=back_admin_kb("plans"))
        return

    value = message.text.strip()
    if field in ("price", "volume", "duration"):
        num = parse_positive_int(value)
        if num is None:
            await message.answer("لطفاً یک عدد صحیح وارد کنید.")
            return
        setattr(plan, {"price": "price", "volume": "volume_gb", "duration": "duration_days"}[field], num)
    elif field == "title":
        plan.title = value
    elif field == "description":
        plan.description = None if value in ("-", "—") else value

    await repos.session.flush()
    await repos.logs.add("plan_edited", message.from_user.id, f"plan={plan.id};{field}")
    await state.clear()
    await message.answer("✅ تغییرات ذخیره شد.\n\n" + _plan_detail_text(plan), reply_markup=plan_detail_kb(plan))
