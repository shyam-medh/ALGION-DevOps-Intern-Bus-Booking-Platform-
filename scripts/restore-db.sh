#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <path_to_backup_file.sql.gz>"
    exit 1
fi

BACKUP_FILE="$1"
CHECKSUM_FILE="${BACKUP_FILE}.sha256"
DB_NAME="${DB_NAME:-bus_booking_db}"
DB_USER="${DB_USER:-postgres}"
CONTAINER_NAME="${DB_CONTAINER:-algion-postgres-db}"

echo "=========================================="
echo "ALGIONS Bus Booking - Database Restore"
echo "=========================================="

# 1. Integrity Verification
if [ -f "$CHECKSUM_FILE" ]; then
    echo "[STEP 1/2] Verifying SHA-256 Checksum..."
    sha256sum -c "$CHECKSUM_FILE"
    echo "[PASS] Checksum verified. Backup integrity intact."
else
    echo "[WARNING] No .sha256 checksum file found. Proceeding with restore."
fi

# 2. Restore Database
echo "[STEP 2/2] Restoring database from $BACKUP_FILE..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME"

echo "[SUCCESS] Database restoration complete!"
echo "=========================================="
