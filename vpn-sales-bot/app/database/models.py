"""SQLAlchemy ORM models."""
from __future__ import annotations

import enum
from datetime import datetime

from sqlalchemy import (
    BigInteger,
    Boolean,
    DateTime,
    Enum,
    ForeignKey,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base


class OrderStatus(str, enum.Enum):
    """Lifecycle of a single purchase."""

    AWAITING_PAYMENT = "awaiting_payment"   # plan + method chosen, no receipt yet
    UNDER_REVIEW = "under_review"           # receipt sent to the admin channel
    COMPLETED = "completed"                 # config delivered to the customer
    REJECTED = "rejected"                   # admin rejected the payment
    CANCELLED = "cancelled"                 # user abandoned the order


class PaymentType(str, enum.Enum):
    CARD = "card"
    CRYPTO = "crypto"


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    telegram_id: Mapped[int] = mapped_column(BigInteger, unique=True, index=True)
    username: Mapped[str | None] = mapped_column(String(64))
    first_name: Mapped[str | None] = mapped_column(String(128))
    last_name: Mapped[str | None] = mapped_column(String(128))
    is_banned: Mapped[bool] = mapped_column(Boolean, default=False, index=True)
    is_admin: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    last_seen_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    orders: Mapped[list["Order"]] = relationship(back_populates="user")


class Plan(Base):
    __tablename__ = "plans"

    id: Mapped[int] = mapped_column(primary_key=True)
    title: Mapped[str] = mapped_column(String(128))
    volume_gb: Mapped[int] = mapped_column(Integer, default=0)     # 0 => unlimited
    duration_days: Mapped[int] = mapped_column(Integer, default=30)
    price: Mapped[int] = mapped_column(BigInteger)                 # in Toman
    description: Mapped[str | None] = mapped_column(Text)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, index=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    orders: Mapped[list["Order"]] = relationship(back_populates="plan")


class CryptoWallet(Base):
    __tablename__ = "crypto_wallets"
    __table_args__ = (UniqueConstraint("symbol", "network", name="uq_wallet_symbol_network"),)

    id: Mapped[int] = mapped_column(primary_key=True)
    symbol: Mapped[str] = mapped_column(String(32))           # BTC, ETH, USDT ...
    network: Mapped[str] = mapped_column(String(64))          # TRC20, ERC20, BEP20 ...
    address: Mapped[str] = mapped_column(String(256))
    memo: Mapped[str | None] = mapped_column(String(128))     # optional tag/memo
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, index=True)
    sort_order: Mapped[int] = mapped_column(Integer, default=0)


class Setting(Base):
    """Key/value store for admin-editable configuration (card, texts, links)."""

    __tablename__ = "settings"

    key: Mapped[str] = mapped_column(String(64), primary_key=True)
    value: Mapped[str | None] = mapped_column(Text)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )


class Order(Base):
    __tablename__ = "orders"

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    plan_id: Mapped[int | None] = mapped_column(ForeignKey("plans.id", ondelete="SET NULL"))

    # Snapshot of plan data at purchase time (so later plan edits don't rewrite history).
    plan_title: Mapped[str] = mapped_column(String(128))
    price: Mapped[int] = mapped_column(BigInteger)

    payment_type: Mapped[PaymentType | None] = mapped_column(Enum(PaymentType))
    payment_detail: Mapped[str | None] = mapped_column(String(128))  # e.g. "USDT TRC20"

    status: Mapped[OrderStatus] = mapped_column(
        Enum(OrderStatus), default=OrderStatus.AWAITING_PAYMENT, index=True
    )

    receipt_file_id: Mapped[str | None] = mapped_column(String(256))
    # Message id of the receipt posted in the admin channel — used to map the
    # admin's reply back to this order.
    admin_message_id: Mapped[int | None] = mapped_column(BigInteger, index=True)

    config_link: Mapped[str | None] = mapped_column(Text)
    delivered_by: Mapped[int | None] = mapped_column(BigInteger)  # admin telegram id

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), index=True
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    user: Mapped["User"] = relationship(back_populates="orders")
    plan: Mapped["Plan | None"] = relationship(back_populates="orders")


class ActionLog(Base):
    """Audit log for important events (admin actions, deliveries, bans...)."""

    __tablename__ = "action_logs"

    id: Mapped[int] = mapped_column(primary_key=True)
    actor_id: Mapped[int | None] = mapped_column(BigInteger, index=True)
    action: Mapped[str] = mapped_column(String(64), index=True)
    detail: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
