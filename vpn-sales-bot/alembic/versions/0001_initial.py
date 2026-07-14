"""initial schema

Revision ID: 0001
Revises:
Create Date: 2026-07-14
"""
from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

from app.database.models import OrderStatus, PaymentType

revision: str = "0001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None

order_status = sa.Enum(OrderStatus, name="orderstatus")
payment_type = sa.Enum(PaymentType, name="paymenttype")


def upgrade() -> None:
    op.create_table(
        "users",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("telegram_id", sa.BigInteger(), nullable=False),
        sa.Column("username", sa.String(length=64), nullable=True),
        sa.Column("first_name", sa.String(length=128), nullable=True),
        sa.Column("last_name", sa.String(length=128), nullable=True),
        sa.Column("is_banned", sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.Column("is_admin", sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("last_seen_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_users_telegram_id", "users", ["telegram_id"], unique=True)
    op.create_index("ix_users_is_banned", "users", ["is_banned"])

    op.create_table(
        "plans",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("title", sa.String(length=128), nullable=False),
        sa.Column("volume_gb", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("duration_days", sa.Integer(), nullable=False, server_default="30"),
        sa.Column("price", sa.BigInteger(), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.true()),
        sa.Column("sort_order", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_plans_is_active", "plans", ["is_active"])

    op.create_table(
        "crypto_wallets",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("symbol", sa.String(length=32), nullable=False),
        sa.Column("network", sa.String(length=64), nullable=False),
        sa.Column("address", sa.String(length=256), nullable=False),
        sa.Column("memo", sa.String(length=128), nullable=True),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.true()),
        sa.Column("sort_order", sa.Integer(), nullable=False, server_default="0"),
        sa.UniqueConstraint("symbol", "network", name="uq_wallet_symbol_network"),
    )
    op.create_index("ix_crypto_wallets_is_active", "crypto_wallets", ["is_active"])

    op.create_table(
        "settings",
        sa.Column("key", sa.String(length=64), primary_key=True),
        sa.Column("value", sa.Text(), nullable=True),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )

    op.create_table(
        "orders",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id", ondelete="CASCADE"), nullable=False),
        sa.Column("plan_id", sa.Integer(), sa.ForeignKey("plans.id", ondelete="SET NULL"), nullable=True),
        sa.Column("plan_title", sa.String(length=128), nullable=False),
        sa.Column("price", sa.BigInteger(), nullable=False),
        sa.Column("payment_type", payment_type, nullable=True),
        sa.Column("payment_detail", sa.String(length=128), nullable=True),
        sa.Column("status", order_status, nullable=False, server_default=OrderStatus.AWAITING_PAYMENT.name),
        sa.Column("receipt_file_id", sa.String(length=256), nullable=True),
        sa.Column("admin_message_id", sa.BigInteger(), nullable=True),
        sa.Column("config_link", sa.Text(), nullable=True),
        sa.Column("delivered_by", sa.BigInteger(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_orders_user_id", "orders", ["user_id"])
    op.create_index("ix_orders_status", "orders", ["status"])
    op.create_index("ix_orders_admin_message_id", "orders", ["admin_message_id"])
    op.create_index("ix_orders_created_at", "orders", ["created_at"])

    op.create_table(
        "action_logs",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("actor_id", sa.BigInteger(), nullable=True),
        sa.Column("action", sa.String(length=64), nullable=False),
        sa.Column("detail", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_action_logs_actor_id", "action_logs", ["actor_id"])
    op.create_index("ix_action_logs_action", "action_logs", ["action"])


def downgrade() -> None:
    op.drop_table("action_logs")
    op.drop_table("orders")
    op.drop_table("settings")
    op.drop_table("crypto_wallets")
    op.drop_table("plans")
    op.drop_table("users")
    order_status.drop(op.get_bind(), checkfirst=True)
    payment_type.drop(op.get_bind(), checkfirst=True)
