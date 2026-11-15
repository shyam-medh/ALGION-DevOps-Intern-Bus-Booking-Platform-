#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backups}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DB_NAME="${DB_NAME:-bus_booking_db}"
DB_USER="${DB_USER:-postgres}"
CONTAINER_NAME="${DB_CONTAINER:-algion-postgres-db}"

mkdir -p "$BACKUP_DIR"

BACKUP_FILE="$BACKUP_DIR/${DB_NAME}_${TIMESTAMP}.sql.gz"
CHECKSUM_FILE="${BACKUP_FILE}.sha256"

echo "=========================================="
echo "ALGIONS Bus Booking - Database Backup"
echo "=========================================="
echo "[INFO] Starting backup of database: $DB_NAME"

# 1. Execute pg_dump inside container and compress with gzip
docker exec "$CONTAINER_NAME" pg_dump -U "$DB_USER" -d "$DB_NAME" --clean --if-exists | gzip > "$BACKUP_FILE"

# 2. Compute SHA-256 Checksum for integrity verification
sha256sum "$BACKUP_FILE" > "$CHECKSUM_FILE"

echo "[SUCCESS] Backup created: $BACKUP_FILE"
echo "[SUCCESS] SHA-256 checksum: $(cat "$CHECKSUM_FILE")"

# 3. Retention policy: Keep last 7 backups, prune older
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz*" -type f -mtime +7 -delete || true

echo "=========================================="
