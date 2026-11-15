#!/usr/bin/env bash
set -euo pipefail

echo "=========================================="
echo "ALGIONS Bus Booking - Emergency Rollback"
echo "=========================================="

COMPOSE_FILE="docker-compose.blue-green.yml"
UPSTREAM_CONF="nginx/upstream.conf"

if grep -q "app-green" "$UPSTREAM_CONF" 2>/dev/null; then
    CURRENT="green"
    ROLLBACK_TO="blue"
else
    CURRENT="blue"
    ROLLBACK_TO="green"
fi

echo "[WARNING] Rolling back from app-$CURRENT to app-$ROLLBACK_TO..."

# 1. Ensure Rollback Target is Running
docker compose -f "$COMPOSE_FILE" start "app-$ROLLBACK_TO"

# 2. Wait for Health
echo "[INFO] Verifying app-$ROLLBACK_TO is ready..."
sleep 3

# 3. Switch Nginx Back
cat <<EOF > "$UPSTREAM_CONF"
upstream backend_app {
    server app-$ROLLBACK_TO:8080;
}
EOF

docker exec algion-nginx-proxy nginx -s reload
echo "[SUCCESS] Nginx traffic restored to app-$ROLLBACK_TO!"

# 4. Stop Problematic Container
docker compose -f "$COMPOSE_FILE" stop "app-$CURRENT"
echo "[COMPLETE] Rollback successful. Active color: $ROLLBACK_TO"
