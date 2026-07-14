#!/usr/bin/env bash
# ==========================================================================
#  VPN Sales Bot — database backup script
#  Dumps the PostgreSQL database to ./backups and prunes old backups.
#  Schedule via cron, e.g. daily at 03:00:
#     0 3 * * * /opt/vpn-sales-bot/scripts/backup.sh >> /var/log/vpnbot-backup.log 2>&1
# ==========================================================================
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

# Load env for credentials.
set -a
# shellcheck disable=SC1091
[[ -f .env ]] && source .env
set +a

BACKUP_DIR="${PROJECT_DIR}/backups"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUT="${BACKUP_DIR}/vpnbot_${TIMESTAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "[*] Dumping database ${POSTGRES_DB:-vpnbot} ..."
docker compose exec -T db pg_dump -U "${POSTGRES_USER:-vpnbot}" "${POSTGRES_DB:-vpnbot}" | gzip > "$OUT"

echo "[✓] Backup written to $OUT"

echo "[*] Pruning backups older than ${RETENTION_DAYS} days..."
find "$BACKUP_DIR" -name 'vpnbot_*.sql.gz' -type f -mtime "+${RETENTION_DAYS}" -delete

echo "[✓] Backup routine complete."
