#!/usr/bin/env bash
# ==========================================================================
#  VPN Sales Bot — deploy / update script
#  Pulls the latest code, rebuilds images, runs migrations and restarts.
# ==========================================================================
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

echo "[*] Pulling latest changes..."
git pull --ff-only || echo "[!] git pull skipped (not a clean fast-forward)."

echo "[*] Rebuilding images..."
docker compose build

echo "[*] Applying database migrations..."
docker compose run --rm migrate

echo "[*] Restarting the bot..."
docker compose up -d

echo "[✓] Deploy complete. Tailing logs (Ctrl-C to exit)..."
docker compose logs -f bot
