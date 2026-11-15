# Production SRE Runbook: Bus Booking Platform
**Company**: ALGIONS AI LABS PRIVATE LIMITED  
**Service**: Trip & Seat Reservation Microservice  
**Document Classification**: Operational SRE & Reviewer Deployment Manual  

---

## 1. Quick Reference & Health Probes

| Check | Target / Endpoint | Expected Healthy Result |
| :--- | :--- | :--- |
| **Edge Proxy (TLS)** | `curl -k https://localhost/` | Returns API or redirects |
| **Liveness Probe** | `curl -k https://localhost/actuator/health/liveness` | `{"status":"UP"}` (HTTP 200) |
| **Readiness Probe** | `curl -k https://localhost/actuator/health/readiness` | `{"status":"UP","components":{"db":{"status":"UP"}}}` (HTTP 200) |
| **PostgreSQL DB** | `docker exec algion-postgres-db pg_isready -U postgres` | `accepting connections` |
| **Redis Cache** | `docker exec algion-redis-cache redis-cli ping` | `PONG` |
| **Prometheus Scrape** | `curl -k https://localhost/actuator/prometheus` | HTTP 200 with JVM, HikariCP, and HTTP metrics |

---

## 2. Clean Environment Setup (From Clone to Live Endpoints)

This section provides the exact operational steps for an engineer, evaluator, or SRE setting up the platform on a fresh machine.

### 2.1 Prerequisites
* **Docker & Docker Compose** (Docker Desktop on Windows/macOS, or Docker Engine on Linux).
* *Note: No local Java, Maven, PostgreSQL, or Redis installations are required. The entire stack is completely containerized.*

### 2.2 Step 1: Clone the Repository
```bash
git clone https://github.com/shyam-medh/ALGION-DevOps-Intern-Bus-Booking-Platform-.git
cd ALGION-DevOps-Intern-Bus-Booking-Platform-
```

### 2.3 Step 2: Initialize Environment Variables
```bash
# On Linux / macOS / Git Bash:
cp .env.example .env

# On Windows PowerShell:
Copy-Item .env.example .env
```
*(All defaults in `.env.example` are pre-configured to work immediately out of the box).*

### 2.4 Step 3: Launch the Production Stack
```bash
# Option A: Standard 1-command build from source (Recommended)
docker compose up -d --build

# Option B: Fast pull of pre-built release from Docker Hub
docker pull shyammedh/bus-booking-platform:1.0.0
docker compose up -d
```

**Automated Startup Sequence (under 60 seconds):**
1. `algion-postgres-db` (PostgreSQL 16) starts and prepares `bus_booking_db` on the isolated `backend-net`.
2. `algion-redis-cache` (Redis 7) starts on `backend-net`.
3. `algion-bus-booking-api` builds and boots up:
   - Hibernate auto-creates normalized relational schema tables.
   - `DataSeeder` pre-populates initial routes (Mumbai &rarr; Pune, Bangalore &rarr; Chennai), scheduled trips, and 20 seats per trip.
   - Actuator readiness probe turns `UP`.
4. `algion-nginx-proxy` starts, binds to host ports `80` (HTTP redirect) and `443` (TLS 1.3 reverse proxy), routing traffic to the internal container.

### 2.5 Step 4: Verify Container Health
```bash
docker compose ps
```
*Expected status: All 4 containers (`nginx-proxy`, `bus-booking-api`, `postgres-db`, `redis-cache`) report `running (healthy)`.*

### 2.6 Viewing Real-Time Application & Infrastructure Logs
```bash
# Application service logs
docker logs -f algion-bus-booking-api

# Nginx reverse proxy access and error logs
docker logs -f algion-nginx-proxy

# PostgreSQL database logs
docker logs -f algion-postgres-db
```

---

## 3. Exercising & Testing the Live Endpoints

### 3.1 Interactive Browser Testing (Swagger UI)
Open your browser to:
👉 **`https://localhost/swagger-ui/index.html`**

> **TLS Certificate Notice**: Because Nginx terminates TLS using a self-signed SSL certificate, your browser will display a security warning (*"Your connection isn't private"*). Click **Advanced** &rarr; **Proceed to localhost (unsafe)** to open the Swagger console.

From Swagger UI, reviewers can execute requests directly against all REST controllers:
* `GET /api/v1/routes`
* `GET /api/v1/trips?origin=Mumbai&destination=Pune`
* `GET /api/v1/trips/{tripId}/seats`
* `POST /api/v1/bookings`
* `POST /api/v1/bookings/{pnr}/cancel`

---

### 3.2 Terminal Testing (`curl` Command Reference)

#### 1. Verify System Readiness Probe
```bash
curl -k https://localhost/actuator/health/readiness
```
*Expected response*: `{"status":"UP","components":{"db":{"status":"UP"}}}`

#### 2. Discover Seeded Routes
```bash
curl -k https://localhost/api/v1/routes
```

#### 3. Search Scheduled Trips (Mumbai to Pune)
```bash
curl -k "https://localhost/api/v1/trips?origin=Mumbai&destination=Pune"
```
*(Copy the returned `tripId` UUID from the JSON response).*

#### 4. Inspect Real-Time Seat Availability
```bash
# Replace TRIP_ID with UUID from step 3
curl -k https://localhost/api/v1/trips/<TRIP_ID>/seats
```

#### 5. Book a Seat (Atomic Reservation with Concurrency Safety)
```bash
curl -k -X POST https://localhost/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "tripId": "<TRIP_ID>",
    "travelDate": "2026-11-20",
    "boardingStop": "Dadar, Mumbai",
    "dropStop": "Swargate, Pune",
    "customerName": "Rahul Sharma",
    "customerEmail": "rahul@example.com",
    "customerPhone": "+919876543210",
    "passengers": [
      {
        "fullName": "Rahul Sharma",
        "age": 28,
        "gender": "MALE",
        "seatNumber": "1A"
      }
    ]
  }'
```
*Expected response*: `HTTP 201 Created` with unique PNR `ALG-2026-XXXX`.

#### 6. Verify Pessimistic Locking (Double-Booking Rejection)
Immediately re-run the exact same `curl` command above for seat `1A`:
*Expected response*: **`HTTP 409 Conflict`** (`"Seat 1A is already booked for this trip"`).

#### 7. Query Booking by PNR & Cancel
```bash
# Retrieve booking
curl -k https://localhost/api/v1/bookings/ALG-2026-XXXX

# Cancel reservation (releases seat back to AVAILABLE)
curl -k -X POST https://localhost/api/v1/bookings/ALG-2026-XXXX/cancel
```

---

## 4. Zero-Downtime Blue-Green Deployment & Rollback

The platform incorporates an automated Blue-Green release engine using dual container slots (`app-blue` on 8081, `app-green` on 8082).

### 4.1 Triggering a Blue-Green Deployment
```bash
chmod +x scripts/deploy-blue-green.sh
./scripts/deploy-blue-green.sh
```
**Under the Hood:**
1. Detects the active live slot (e.g. `app-blue`).
2. Builds and starts the idle slot (e.g. `app-green`) with the new image.
3. Polls `/actuator/health/readiness` on the new container until it returns `UP`.
4. Dynamically re-writes `nginx/upstream.conf` to point to the new slot.
5. Executes `nginx -s reload` inside `algion-nginx-proxy` (hot reload without dropping active TCP connections).
6. Drains and shuts down the old slot.

### 4.2 Emergency Instant Rollback (< 500ms)
If an anomaly occurs after deployment:
```bash
chmod +x scripts/rollback.sh
./scripts/rollback.sh
```
*Instantly switches the Nginx upstream back to the previous stable slot and executes a hot reload.*

---

## 5. Disaster Recovery, Backup & Restore

### 5.1 Automated Database Backup (Gzip + SHA-256)
```bash
chmod +x scripts/backup-db.sh
./scripts/backup-db.sh
```
* Outputs compressed archive: `backups/bus_booking_db_<timestamp>.sql.gz`
* Generates cryptographic integrity checksum: `backups/bus_booking_db_<timestamp>.sql.gz.sha256`
* Automatically rotates and prunes backups older than 7 days.

### 5.2 Restoring from Verified Backup
```bash
chmod +x scripts/restore-db.sh
./scripts/restore-db.sh backups/bus_booking_db_YYYYMMDD_HHMMSS.sql.gz
```
* **Pre-Flight Hash Check**: Validates the archive against its `.sha256` checksum file before applying changes.
* Restores schema and transactional data into the isolated PostgreSQL container.

---

## 6. SRE Incident Triage Procedures

### 6.1 High Rate of HTTP 429 Too Many Requests
* **Symptom**: Clients report `HTTP 429 Too Many Requests`.
* **Cause**: Nginx leaky-bucket rate limiting threshold (100 req/min/IP) exceeded.
* **Diagnosis**:
  ```bash
  docker logs algion-nginx-proxy | grep "limiting requests"
  ```
* **Remediation**: If caused by legitimate traffic volume, adjust rate limit parameters in `nginx/nginx.conf` (`rate=200r/m` and `burst=50`) and reload:
  ```bash
  docker exec algion-nginx-proxy nginx -s reload
  ```

### 6.2 High Rate of HTTP 409 Conflict
* **Symptom**: Booking requests return `409 Conflict`.
* **Cause**: **Expected correct behavior** under high concurrency when multiple users attempt to reserve the same seat simultaneously.
* **Verification**: Inspect current seat statuses in PostgreSQL:
  ```bash
  docker exec -it algion-postgres-db psql -U postgres -d bus_booking_db -c "SELECT seat_number, status FROM trip_seats WHERE status='BOOKED';"
  ```

### 6.3 Application Reports Readiness DOWN (HTTP 503)
* **Symptom**: Actuator `/actuator/health/readiness` returns 503.
* **Diagnosis**:
  1. Inspect HikariCP pool logs:
     ```bash
     docker logs algion-bus-booking-api | grep -i "HikariPool"
     ```
  2. Test PostgreSQL direct socket response:
     ```bash
     docker exec algion-postgres-db pg_isready -U postgres
     ```
* **Remediation**: If database crashed, restart container:
  ```bash
  docker start algion-postgres-db
  ```
  *HikariCP will automatically restore pooled connections without needing an application restart.*

---

## 7. Service Teardown & Clean Reset

```bash
# Standard graceful shutdown (preserves database data volumes)
docker compose down

# Complete clean reset (wipes all database and cache volumes for fresh testing)
docker compose down -v
```
