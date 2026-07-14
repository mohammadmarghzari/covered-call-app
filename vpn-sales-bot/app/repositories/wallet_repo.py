"""Data-access for crypto wallets."""
from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.database.models import CryptoWallet


class WalletRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def list_active(self) -> list[CryptoWallet]:
        result = await self.session.execute(
            select(CryptoWallet)
            .where(CryptoWallet.is_active.is_(True))
            .order_by(CryptoWallet.sort_order, CryptoWallet.symbol)
        )
        return list(result.scalars().all())

    async def list_all(self) -> list[CryptoWallet]:
        result = await self.session.execute(
            select(CryptoWallet).order_by(CryptoWallet.sort_order, CryptoWallet.symbol)
        )
        return list(result.scalars().all())

    async def get(self, wallet_id: int) -> CryptoWallet | None:
        return await self.session.get(CryptoWallet, wallet_id)

    async def create(
        self, symbol: str, network: str, address: str, memo: str | None = None
    ) -> CryptoWallet:
        wallet = CryptoWallet(
            symbol=symbol.upper().strip(),
            network=network.strip(),
            address=address.strip(),
            memo=memo,
        )
        self.session.add(wallet)
        await self.session.flush()
        return wallet

    async def delete(self, wallet_id: int) -> bool:
        wallet = await self.get(wallet_id)
        if wallet is None:
            return False
        await self.session.delete(wallet)
        return True
