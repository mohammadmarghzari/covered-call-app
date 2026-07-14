"""Data-access for the audit log."""
from __future__ import annotations

from sqlalchemy.ext.asyncio import AsyncSession

from app.database.models import ActionLog


class LogRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def add(self, action: str, actor_id: int | None, detail: str | None = None) -> None:
        self.session.add(ActionLog(action=action, actor_id=actor_id, detail=detail))
