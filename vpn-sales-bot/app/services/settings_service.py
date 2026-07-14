"""Convenience helpers for reading grouped settings."""
from __future__ import annotations

from app.services.container import Repos
from app.utils.constants import SettingKey


async def get_download_links(repos: Repos) -> dict[str, str]:
    keys = [
        SettingKey.DOWNLOAD_ANDROID,
        SettingKey.DOWNLOAD_IPHONE,
        SettingKey.DOWNLOAD_WINDOWS,
        SettingKey.DOWNLOAD_MAC,
    ]
    values = await repos.settings.get_many(keys)
    return {
        "android": values.get(SettingKey.DOWNLOAD_ANDROID) or "",
        "iphone": values.get(SettingKey.DOWNLOAD_IPHONE) or "",
        "windows": values.get(SettingKey.DOWNLOAD_WINDOWS) or "",
        "mac": values.get(SettingKey.DOWNLOAD_MAC) or "",
    }


async def get_card_info(repos: Repos) -> tuple[str, str]:
    values = await repos.settings.get_many(
        [SettingKey.CARD_NUMBER, SettingKey.CARD_HOLDER]
    )
    return (
        values.get(SettingKey.CARD_NUMBER) or "-",
        values.get(SettingKey.CARD_HOLDER) or "-",
    )
