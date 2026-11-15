#!/usr/bin/env bash
set -euo pipefail

echo "=========================================================="
echo "ALGIONS Bus Booking Platform - SRE Chaos Recovery Drill"
echo "=========================================================="

API_URL="http://localhost:8080"
NGINX_URL="https://localhost"

# ------------------------------------------------------------------
# Test 1: Verify Initial Baseline Health
# ------------------------------------------------------------------
echo ""
echo "[DRILL 1] Baseline Health Check..."
LIVENESS=$(curl -s -k "$API_URL/actuator/health/liveness" | grep -o '"status":"UP"' || true)
READINESS=$(curl -s -k "$API_URL/actuator/health/readiness" | grep -o '"status":"UP"' || true)

if [ -n "$LIVENESS" ] && [ -n "$READINESS" ]; then
    echo "  [PASS] Application liveness and readiness are UP."
else
    echo "  [FAIL] Baseline health check failed. Ensure services are running."
    exit 1
fi

# ------------------------------------------------------------------
# Test 2: Database Failure Simulation & Readiness Probe Degradation
# ------------------------------------------------------------------
echo ""
echo "[DRILL 2] Simulating Database Outage (Stopping PostgreSQL)..."
docker stop algion-postgres-db >/dev/null 2>&1 || true
sleep 3

echo "  Probing readiness during DB outage..."
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -k "$API_URL/actuator/health/readiness" || true)
if [ "$HTTP_STATUS" -eq 503 ]; then
    echo "  [PASS] Readiness probe correctly degraded to HTTP 503 Service Unavailable!"
else
    echo "  [NOTE] Received HTTP $HTTP_STATUS during DB outage."
fi

# ------------------------------------------------------------------
# Test 3: Self-Healing & Automatic Recovery
# ------------------------------------------------------------------
echo ""
echo "[DRILL 3] Simulating Database Recovery (Restarting PostgreSQL)..."
docker start algion-postgres-db >/dev/null 2>&1 || true
sleep 5

echo "  Waiting for HikariCP connection pool self-healing..."
MAX_WAIT=20
WAIT_COUNT=0
RECOVERED=false

while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    WAIT_COUNT=$((WAIT_COUNT + 1))
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -k "$API_URL/actuator/health/readiness" || true)
    if [ "$STATUS" -eq 200 ]; then
        RECOVERED=true
        echo "  [PASS] Database reconnected! Readiness probe restored to HTTP 200 OK."
        break
    fi
    sleep 1
done

if [ "$RECOVERED" != "true" ]; then
    echo "  [FAIL] Service did not recover within $MAX_WAIT seconds."
fi

# ------------------------------------------------------------------
# Test 4: Rate Limiting Enforcement (100 req/min)
# ------------------------------------------------------------------
echo ""
echo "[DRILL 4] Blasting Nginx Reverse Proxy with 120 rapid requests..."
THROTTLED=false

for i in $(seq 1 120); do
    CODE=$(curl -s -o /dev/null -w "%{http_code}" -k "$NGINX_URL/" || true)
    if [ "$CODE" -eq 429 ]; then
        THROTTLED=true
        echo "  [PASS] Request #$i received HTTP 429 Too Many Requests. Rate limiting verified!"
        break
    fi
done

if [ "$THROTTLED" != "true" ]; then
    echo "  [NOTE] Requests stayed within rate limit or burst buffer."
fi

echo ""
echo "=========================================================="
echo "Chaos Recovery Drill Complete!"
echo "=========================================================="
