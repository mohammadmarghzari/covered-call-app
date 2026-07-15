"""Runtime state resolved at startup (not known at import time)."""
from __future__ import annotations

from dataclasses import dataclass

from app.config import settings


@dataclass
class Runtime:
    # Numeric id of the admin channel, resolved at startup (via get_chat when
    # only a username/link was configured).
    admin_channel_id: int | None = None


runtime = Runtime()


def channel_send_target() -> int | str:
    """Best target for sending to the admin channel."""
    if runtime.admin_channel_id is not None:
        return runtime.admin_channel_id
    return settings.admin_channel_ref


def is_admin_channel(chat_id: int, chat_username: str | None) -> bool:
    """True if the given chat is the configured admin channel."""
    if runtime.admin_channel_id is not None and chat_id == runtime.admin_channel_id:
        return True
    uname = settings.admin_channel_username
    if uname and chat_username and chat_username.lower() == uname.lower():
        return True
    return False
