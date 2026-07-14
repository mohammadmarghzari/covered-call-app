"""Small input validators for admin-entered data."""
from __future__ import annotations

import re

_CONFIG_SCHEMES = (
    "vless://",
    "vmess://",
    "trojan://",
    "ss://",
    "ssr://",
    "hysteria://",
    "hysteria2://",
    "hy2://",
    "tuic://",
    "wireguard://",
    "http://",
    "https://",
)


def is_config_link(text: str) -> bool:
    """Heuristic check that a string looks like a VPN config / subscription link."""
    if not text:
        return False
    stripped = text.strip().lower()
    return stripped.startswith(_CONFIG_SCHEMES)


def parse_positive_int(text: str) -> int | None:
    text = text.strip().replace(",", "").replace("٬", "")
    # allow persian digits
    text = text.translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789"))
    if not text.isdigit():
        return None
    value = int(text)
    return value if value >= 0 else None


def normalize_card_number(text: str) -> str:
    digits = re.sub(r"\D", "", text.translate(str.maketrans("۰۱۲۳۴۵۶۷۸۹", "0123456789")))
    if len(digits) == 16:
        return "-".join(digits[i : i + 4] for i in range(0, 16, 4))
    return text.strip()
