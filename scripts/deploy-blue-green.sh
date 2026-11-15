#!/usr/bin/env bash
set -euo pipefail

echo "=========================================="
echo "ALGIONS Bus Booking - Blue-Green Deployer"
echo "=========================================="

COMPOSE_FILE="docker-compose.blue-green.yml"
UPSTREAM_CONF="nginx/upstream.conf"

# 1. Determine Current Active Color
if grep -q "app-blue" "$UPSTREAM_CONF" 2>/dev/null; then
    CURRENT="blue"
    TARGET="green"
else
    CURRENT="green"
    TARGET="blue"
fi

echo "[INFO] Current Live Color: $CURRENT"
echo "[INFO] Target Deployment Color: $TARGET"

# 2. Build and Start Target Container
echo "[STEP 1/4] Building and starting app-$TARGET..."
docker compose -f "$COMPOSE_FILE" up -d --build "app-$TARGET"

# 3. Health Check Verification on Target Container
echo "[STEP 2/4] Verifying health readiness on app-$TARGET..."
MAX_ATTEMPTS=30
ATTEMPT=0
HEALTHY=false

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))
    echo "  Attempt $ATTEMPT/$MAX_ATTEMPTS: Checking /actuator/health/readiness..."
    
    if docker exec "algion-app-$TARGET" wget -q -O - http://localhost:8080/actuator/health/readiness 2>/dev/null | grep -q '"status":"UP"'; then
        HEALTHY=true
        echo "[SUCCESS] app-$TARGET is healthy and ready to accept traffic!"
        break
    fi
    sleep 2
done

if [ "$HEALTHY" != "true" ]; then
    echo "[ERROR] Health check failed on app-$TARGET after $MAX_ATTEMPTS attempts."
    echo "[ACTION] Aborting deployment and stopping app-$TARGET..."
    docker compose -f "$COMPOSE_FILE" stop "app-$TARGET"
    exit 1
fi

# 4. Switch Traffic at Nginx Reverse Proxy
echo "[STEP 3/4] Switching Nginx upstream to app-$TARGET..."
cat <<EOF > "$UPSTREAM_CONF"
upstream backend_app {
    server app-$TARGET:8080;
}
EOF

docker exec algion-nginx-proxy nginx -s reload
echo "[SUCCESS] Nginx traffic switched to app-$TARGET with zero downtime!"

# 5. Gracefully Drain and Stop Old Container
echo "[STEP 4/4] Draining and stopping old app-$CURRENT..."
sleep 5
docker compose -f "$COMPOSE_FILE" stop "app-$CURRENT"

echo "=========================================="
echo "[DEPLOYMENT COMPLETE] Active color is now: $TARGET"
echo "=========================================="
