#!/usr/bin/env bash
# ==========================================================================
#  VPN Sales Bot - Ubuntu server one-shot setup
#  Installs Docker, builds an interactive .env, runs the stack and seeds
#  the initial plans. Ideal for setting up from a phone over SSH.
#
#  Usage (as root, from the project root):
#     bash scripts/setup_linux.sh
# ==========================================================================
set -euo pipefail

BLUE='\033[1;34m'; GREEN='\033[1;32m'; YELLOW='\033[1;33m'; RED='\033[1;31m'; NC='\033[0m'
info(){ echo -e "${BLUE}[*]${NC} $*"; }
ok(){   echo -e "${GREEN}[OK]${NC} $*"; }
warn(){ echo -e "${YELLOW}[!]${NC} $*"; }
fail(){ echo -e "${RED}[x]${NC} $*" >&2; exit 1; }

echo -e "${BLUE}==== VPN Sales Bot - Ubuntu setup ====${NC}"

[[ $EUID -eq 0 ]] || fail "این اسکریپت را با کاربر root اجرا کن (یا با sudo)."

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"
info "پوشهٔ پروژه: $PROJECT_DIR"

# --- Docker ---------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  info "در حال نصب Docker (چند دقیقه) ..."
  apt-get update -y
  apt-get install -y ca-certificates curl gnupg
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
    https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  systemctl enable --now docker
  ok "Docker نصب شد."
else
  ok "Docker از قبل نصب است."
fi

# --- .env -----------------------------------------------------------------
if [[ -f .env ]]; then
  warn "فایل .env از قبل وجود دارد؛ از همان استفاده می‌شود (برای ساخت مجدد حذفش کن)."
else
  echo
  info "تنظیم اطلاعات ربات:"
  read -r -p "توکن ربات (BOT_TOKEN): " TOKEN
  [[ -n "${TOKEN:-}" ]] || fail "توکن نمی‌تواند خالی باشد."
  read -r -p "آیدی عددی مدیر (ADMIN_IDS، چند نفر با کاما): " ADMIN
  [[ -n "${ADMIN:-}" ]] || fail "آیدی مدیر نمی‌تواند خالی باشد."
  read -r -p "آدرس کانال [Enter برای https://t.me/unnamed_vpn_7]: " CHANNEL
  CHANNEL="${CHANNEL:-https://t.me/unnamed_vpn_7}"

  PASS="$(openssl rand -hex 16 2>/dev/null || head -c 48 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)"

  cat > .env <<EOF
BOT_TOKEN=$TOKEN
ADMIN_IDS=$ADMIN
ADMIN_CHANNEL_ID=$CHANNEL

POSTGRES_USER=vpnbot
POSTGRES_PASSWORD=$PASS
POSTGRES_DB=vpnbot
POSTGRES_HOST=db
POSTGRES_PORT=5432
DATABASE_URL=postgresql+asyncpg://vpnbot:$PASS@db:5432/vpnbot

REDIS_URL=redis://redis:6379/0

# اگر تلگرام روی سرور فیلتر بود، اینجا پروکسی بگذار (مثلاً socks5://... )
TELEGRAM_PROXY=

RATE_LIMIT_MESSAGES=5
RATE_LIMIT_WINDOW=3
LOG_LEVEL=INFO
CURRENCY=تومان
TIMEZONE=Asia/Tehran
EOF
  ok "فایل .env ساخته شد."
fi

# --- build & run ----------------------------------------------------------
info "در حال ساخت و اجرای ربات (بار اول چند دقیقه طول می‌کشد) ..."
docker compose up -d --build

info "وارد کردن پلن‌های اولیه ..."
docker compose run --rm bot python -m app.seed

echo
ok "همه‌چیز آماده شد! ربات روشن است. 🎉"
echo "----------------------------------------------------"
echo " • دیدن لاگ:    docker compose logs -f bot"
echo " • توقف:        docker compose down"
echo " • اجرای دوباره: docker compose up -d"
echo "----------------------------------------------------"
echo "حالا در تلگرام به @HighQualityVPN_bot پیام /start بده."
