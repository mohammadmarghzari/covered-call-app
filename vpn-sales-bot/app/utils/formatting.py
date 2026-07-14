"""Helpers for formatting numbers, dates and plan labels in Persian."""
from __future__ import annotations

from datetime import datetime, timezone
from zoneinfo import ZoneInfo

from app.config import settings

_EN_TO_FA = str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹")

try:
    _TZ = ZoneInfo(settings.timezone)
except Exception:  # pragma: no cover - fallback if tz data missing
    _TZ = timezone.utc


def to_fa_digits(text: str | int) -> str:
    return str(text).translate(_EN_TO_FA)


def format_price(amount: int) -> str:
    """Format an integer amount with thousands separators and Persian digits."""
    return to_fa_digits(f"{amount:,}") + f" {settings.currency}"


def format_volume(volume_gb: int) -> str:
    if volume_gb <= 0:
        return "نامحدود"
    return f"{to_fa_digits(volume_gb)} گیگابایت"


def format_duration(days: int) -> str:
    if days <= 0:
        return "بدون محدودیت زمانی"
    if days % 30 == 0:
        return f"{to_fa_digits(days // 30)} ماهه"
    return f"{to_fa_digits(days)} روزه"


def format_datetime(dt: datetime | None) -> str:
    if dt is None:
        return "-"
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    local = dt.astimezone(_TZ)
    return to_fa_digits(local.strftime("%Y/%m/%d %H:%M"))


def plan_button_label(title: str, price: int) -> str:
    return f"{title} — {format_price(price)}"
