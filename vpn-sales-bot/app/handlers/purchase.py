"""Buy flow: list plans, choose a plan, then choose payment method."""
from __future__ import annotations

from aiogram import F, Router
from aiogram.fsm.context import FSMContext
from aiogram.types import CallbackQuery

from app.keyboards.callbacks import MenuCB, PlanCB
from app.keyboards.user_kb import (
    back_home_kb,
    payment_methods_kb,
    plans_kb,
)
from app.services.container import Repos
from app.utils.formatting import format_duration, format_price, format_volume

router = Router(name="purchase")


@router.callback_query(MenuCB.filter(F.action == "buy"))
async def show_plans(call: CallbackQuery, repos: Repos, state: FSMContext) -> None:
    await state.clear()
    plans = await repos.plans.list_active()
    if not plans:
        await call.message.edit_text(
            "😔 در حال حاضر پلنی برای فروش موجود نیست. لطفاً بعداً مراجعه کنید.",
            reply_markup=back_home_kb(),
        )
        await call.answer()
        return

    text = "🛒 <b>لطفاً یکی از پلن‌های زیر را انتخاب کنید:</b>"
    await call.message.edit_text(text, reply_markup=plans_kb(plans))
    await call.answer()


@router.callback_query(PlanCB.filter())
async def choose_plan(
    call: CallbackQuery, callback_data: PlanCB, repos: Repos, state: FSMContext
) -> None:
    plan = await repos.plans.get(callback_data.plan_id)
    if plan is None or not plan.is_active:
        await call.answer("این پلن دیگر در دسترس نیست.", show_alert=True)
        return

    user = await repos.users.get_or_create(
        telegram_id=call.from_user.id,
        username=call.from_user.username,
        first_name=call.from_user.first_name,
        last_name=call.from_user.last_name,
    )
    order = await repos.orders.create(
        user_id=user.id, plan_id=plan.id, plan_title=plan.title, price=plan.price
    )

    desc = f"\n\n📝 {plan.description}" if plan.description else ""
    text = (
        "✅ <b>پلن انتخابی شما:</b>\n\n"
        f"🏷 عنوان: {plan.title}\n"
        f"📦 حجم: {format_volume(plan.volume_gb)}\n"
        f"📅 مدت: {format_duration(plan.duration_days)}\n"
        f"💵 قیمت: {format_price(plan.price)}"
        f"{desc}\n\n"
        "💳 لطفاً روش پرداخت را انتخاب کنید:"
    )
    await call.message.edit_text(text, reply_markup=payment_methods_kb(order.id))
    await call.answer()
