#!/usr/bin/env bash
# ==========================================================================
#  VPN Sales Bot — one-shot installer for Ubuntu 24.04
#  Installs Docker + Docker Compose, prepares .env and starts the stack.
# ==========================================================================
set -euo pipefail

BLUE="\033[1;34m"; GREEN="\033[1;32m"; YELLOW="\033[1;33m"; RED="\033[1;31m"; NC="\033[0m"
info()  { echo -e "${BLUE}[*]${NC} $*"; }
ok()    { echo -e "${GREEN}[✓]${NC} $*"; }
warn()  { echo -e "${YELLOW}[!]${NC} $*"; }
err()   { echo -e "${RED}[x]${NC} $*" >&2; }

if [[ $EUID -ne 0 ]]; then
  err "Please run as root (sudo bash scripts/install.sh)"
  exit 1
fi

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

info "Updating apt and installing prerequisites..."
apt-get update -y
apt-get install -y ca-certificates curl gnupg lsb-release git

if ! command -v docker >/dev/null 2>&1; then
  info "Installing Docker Engine..."
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
  ok "Docker installed."
else
  ok "Docker already present."
fi

if [[ ! -f .env ]]; then
  info "Creating .env from .env.example — please edit it with your real values."
  cp .env.example .env
  warn "Edit .env now (BOT_TOKEN, ADMIN_IDS, ADMIN_CHANNEL_ID, DB password ...)."
  warn "Run this script again after editing, or start manually with: docker compose up -d --build"
  exit 0
fi

info "Building and starting containers..."
docker compose up -d --build

ok "Done! Check logs with: docker compose logs -f bot"
