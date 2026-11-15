# Production SRE Runbook: Bus Booking Platform
**Company**: ALGIONS AI LABS PRIVATE LIMITED  
**Service**: Trip & Seat Reservation Microservice  
**Document Classification**: Operational SRE Guide  

---

## 1. Quick Reference & Health Checks

| Check | Target / Endpoint | Expected Healthy Result |
| :--- | :--- | :--- |
| **Edge Proxy** | `curl -k https://localhost/` | Returns API or redirects |
| **Liveness Probe** | `curl -s http://localhost:8080/actuator/health/liveness` | `{"status":"UP"}` (HTTP 200) |
| **Readiness Probe** | `curl -s http://localhost:8080/actuator/health/readiness` | `{"status":"UP","components":{"db":{"status":"UP"}}}` (HTTP 200) |
| **PostgreSQL DB** | `docker exec algion-postgres-db pg_isready -U postgres` | `accepting connections` |
| **Redis Cache** | `docker exec algion-redis-cache redis-cli ping` | `PONG` |
| **Prometheus Scrape** | `curl -s http://localhost:8080/actuator/prometheus` | HTTP 200 with JVM & HTTP metrics |

---

## 2. Standard Service Lifecycle

### 2.1 Starting the Entire Production Stack
```bash
docker compose up -d --build
```

### 2.2 Verifying All Service Containers
```bash
docker compose ps
```
*Expected status for all 4 containers (`app`, `postgres`, `redis`, `nginx`): `running (healthy)`.*

### 2.3 Viewing Real-Time Logs
```bash
# Application logs
docker logs -f algion-bus-booking-api

# Reverse proxy logs
docker logs -f algion-nginx-proxy

# Database logs
docker logs -f algion-postgres-db
```

---

## 3. Zero-Downtime Blue-Green Deployment

The platform uses an automated Blue-Green deployment engine to achieve **zero-downtime releases**.

### 3.1 Triggering Blue-Green Deployment
```bash
chmod +x scripts/deploy-blue-green.sh
./scripts/deploy-blue-green.sh
```

### 3.2 How It Works Under the Hood
1. Detects whether `app-blue` or `app-green` is currently live.
2. Builds and starts the idle target container.
3. Polls `/actuator/health/readiness` on the new container until it returns `UP`.
4. Switches the Nginx upstream configuration:
   ```nginx
   upstream backend_app {
       server app-green:8080; # or app-blue:8080
   }
   ```
5. Executes `nginx -s reload` inside `algion-nginx-proxy` (hot reload with zero dropped packets).
6. Gracefully terminates the old container after a 5-second drain period.

### 3.3 Emergency Rollback
If an anomaly is detected immediately after deployment:
```bash
chmod +x scripts/rollback.sh
./scripts/rollback.sh
```

---

## 4. Disaster Recovery, Backup & Restore

### 4.1 Automated Database Backup
Run the backup script to generate an integrity-verified dump:
```bash
chmod +x scripts/backup-db.sh
./scripts/backup-db.sh
```
- Creates: `backups/bus_booking_db_<timestamp>.sql.gz`
- Computes SHA-256 hash: `backups/bus_booking_db_<timestamp>.sql.gz.sha256`
- Automatically prunes backups older than 7 days.

### 4.2 Restoring from Backup
```bash
chmod +x scripts/restore-db.sh
./scripts/restore-db.sh backups/bus_booking_db_YYYYMMDD_HHMMSS.sql.gz
```
- **Integrity Validation**: Verifies the SHA-256 hash before applying the restore.
- Executes `psql` transactionally inside the PostgreSQL container.

---

## 5. Incident Triage Procedures

### 5.1 High Rate of HTTP 429 Too Many Requests
- **Symptom**: Clients report `HTTP 429 Too Many Requests`.
- **Cause**: Nginx rate-limiting threshold exceeded (100 req/min/IP).
- **Diagnosis**: Check Nginx access log:
  ```bash
  docker logs algion-nginx-proxy | grep "limiting requests"
  ```
- **Remediation**: If legitimate traffic spike, adjust `rate=200r/m` and `burst=50` in `nginx/nginx.conf` and reload:
  ```bash
  docker exec algion-nginx-proxy nginx -s reload
  ```

### 5.2 High Rate of HTTP 409 Conflict
- **Symptom**: Booking requests return `409 Conflict`.
- **Cause**: This is **expected and correct behavior** under high concurrency when multiple passengers contest the exact same seat.
- **Verification**: Check database seat status:
  ```bash
  docker exec -it algion-postgres-db psql -U postgres -d bus_booking_db -c "SELECT seat_number, status FROM trip_seats WHERE status='BOOKED';"
  ```

### 5.3 Application Reports Readiness DOWN (HTTP 503)
- **Symptom**: Actuator `/actuator/health/readiness` returns 503.
- **Diagnosis**:
  1. Inspect PostgreSQL connection pool:
     ```bash
     docker logs algion-bus-booking-api | grep -i "HikariPool"
     ```
  2. Test PostgreSQL direct connectivity:
     ```bash
     docker exec algion-postgres-db pg_isready -U postgres
     ```
- **Remediation**: Restart database container if crashed:
  ```bash
  docker start algion-postgres-db
  ```
  HikariCP will automatically re-establish pooled connections without requiring an application restart.
