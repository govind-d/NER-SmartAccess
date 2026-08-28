# NER-SmartLogix-AI — Setup Guide (Phase 1 draft)

This is the *planned* setup. Commands become real from Phase 2 onward, when the backend and frontend folders are generated.

---

## 1. What is already on this machine

| Tool | Detected | Verdict |
|---|---|---|
| Java | 25.0.1 LTS | works, but see §2 |
| Maven | not on PATH | not needed — we ship the Maven Wrapper |
| Node.js | v24.19.0 | good |
| Docker Desktop | installed | used for PostgreSQL + PostGIS |
| Git | installed, repo on `main` | good |

## 2. Java version decision (needs your call)

| Option | Stack | Trade-off |
|---|---|---|
| **A (recommended)** | JDK 21 (Temurin) + Spring Boot 3.5.x | The mandated "Spring Boot 3.x", maximum tutorial/StackOverflow coverage, zero class-file surprises |
| B | JDK 25 + Spring Boot 4.0.x | Uses the JDK you already have, but Boot 4 is newer than most guides you will read while learning |

Install for option A:

```bash
winget install EclipseAdoptium.Temurin.21.JDK
```

Then set `JAVA_HOME` to the JDK 21 folder (the wrapper and IDE both read it).

## 3. Database

```bash
docker compose up -d postgis
```

```
image:    postgis/postgis:16-3.4
db:       nersmartlogix
user:     ${DB_USERNAME}
password: ${DB_PASSWORD}
port:     5432
volume:   pgdata
```

Verify:

```bash
docker exec -it ner-postgis psql -U postgres -d nersmartlogix -c "SELECT postgis_version();"
```

## 4. Environment variables

Copy `backend/.env.example` to `backend/.env` (git-ignored) and fill it in:

```
DB_URL=jdbc:postgresql://localhost:5432/nersmartlogix
DB_USERNAME=nerapp
DB_PASSWORD=<choose-your-own>

JWT_SECRET=<at least 64 random characters>
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

WEATHER_PROVIDER=mock
WEATHER_API_KEY=
WEATHER_API_BASE_URL=https://api.openweathermap.org/data/2.5

ROUTING_PROVIDER=osrm
OSRM_BASE_URL=https://router.project-osrm.org
ORS_API_KEY=

FILE_STORAGE_PATH=./uploads
FRONTEND_ORIGIN=http://localhost:5173
GPS_SIMULATOR_ENABLED=true
SEED_ADMIN_PASSWORD=<choose-your-own>
```

Generate a JWT secret:

```bash
openssl rand -base64 64
```

**Never commit `.env`.** `application.yml` reads these as `${JWT_SECRET}` with no default, so a missing secret fails the app at startup instead of silently using a weak one.

## 5. Backend

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

* API: http://localhost:8080/api/v1
* Swagger UI: http://localhost:8080/swagger-ui.html
* Health: http://localhost:8080/actuator/health

## 6. Frontend

```bash
cd frontend && npm install && npm run dev
```

App: http://localhost:5173

## 7. Full stack in Docker

```bash
docker compose up --build
```

## 8. Demo accounts (dev profile seed)

| Username | Role |
|---|---|
| admin | ADMIN |
| authority1 | AUTHORITY_OFFICIAL |
| manager1 | LOGISTICS_MANAGER |
| officer1 | FIELD_OFFICER |
| driver1 | DRIVER |

All seeded with `SEED_ADMIN_PASSWORD` from your `.env` — no password is ever committed to the repository.

## 9. Troubleshooting (expected issues)

| Symptom | Cause | Fix |
|---|---|---|
| `type "geometry" does not exist` | PostGIS extension not enabled | Flyway `V1__enable_postgis.sql` runs `CREATE EXTENSION postgis;` — check the migration ran |
| `Unsupported class file major version` | JDK newer than the Spring Boot line | See §2 |
| Schema validation error at startup | Entity drifted from the migration | Add a new `V*` migration; never edit an applied one |
| WebSocket connects then drops | JWT missing on the STOMP `CONNECT` frame | Send `Authorization` in the connect headers, not as a query param |
| CORS error in the browser | `FRONTEND_ORIGIN` mismatch | Set it to the exact Vite origin |

---

## 10. Frontend (Phases 9–10)

```bash
cd frontend && npm install
```

```bash
npm run dev
```

App: http://localhost:5173. The Vite dev server proxies `/api` and `/ws` to
http://localhost:8080, so CORS never comes into play during development.

Verify the build the way CI would:

```bash
npm run build
```

That runs `tsc -b` first, so a type error fails the build rather than reaching a user.

### Testing the offline module

1. Sign in as `officer1` and open **Report incident**.
2. DevTools → Network → **Offline**.
3. Submit a report. The banner turns dark and the report appears under *Waiting to sync*.
4. Switch back to **Online**. The queue flushes automatically within a second.
5. Press *Sync now* again — the server reports a **duplicate**, proving the `clientUuid`
   idempotency key works.

The queue lives in IndexedDB (`ner-smartlogix` database, `reports` store) and survives a
browser restart, an app update and a device reboot.

---

## 11. Full stack in Docker

```bash
docker compose --profile full up --build
```

This builds both images (multi-stage: Maven then a JRE for the backend, npm then nginx
for the frontend) and starts them alongside PostGIS. nginx proxies `/api` and `/ws` to the
backend container, so the browser sees a single origin.

---

## 12. Running the tests

```bash
cd backend && mvn -B test
```

29 tests, no database required: the JWT provider and every rule of the risk engine are
plain Java and run in under a second.
