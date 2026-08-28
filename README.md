# NER-SmartLogix-AI

An AI-powered Smart Logistics and Accessibility Intelligence Platform for the North Eastern Region (NER), India — real-time route monitoring, disruption prediction, GIS-based accessibility insights, GPS vehicle tracking, risk-aware routing and offline field reporting.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 16 + PostGIS · React 18 + TypeScript + Tailwind + Leaflet · **No Python anywhere**, including the AI module.

---

## Status — all phases complete

| Phase | Scope | Where it lives |
|---|---|---|
| 1 | System design, database design, API contract, roadmap | [`docs/`](docs) |
| 2 | Skeleton, 19 entities, Flyway V1–V8, 18 repositories | `backend/src/main/java/.../entity`, `repository`, `resources/db/migration` |
| 3 | JWT auth, refresh-token rotation, RBAC, global exception handling | `security/`, `config/SecurityConfig.java`, `exception/` |
| 4 | Districts, roads, bridges, vehicles, deliveries + GeoJSON layers | `service/impl/`, `controller/`, `service/GeoJsonService.java` |
| 5 | AI risk engine — 6 weighted rules in Java | `ml/` |
| 6 | Incidents, alerts, notifications, WebSocket, offline sync | `event/`, `notification/`, `websocket/`, `service/impl/IncidentServiceImpl.java` |
| 7 | Risk-aware routing (OSRM + Dijkstra fallback + scorer) | `routing/`, `service/impl/RouteServiceImpl.java` |
| 8 | GPS tracking, simulator, delivery lifecycle, delay detection | `integration/gps/` |
| 9–10 | React frontend, live map, dashboard, offline PWA | [`frontend/src`](frontend/src) |
| 11 | Tests, Dockerfiles, documentation | `backend/src/test`, `*/Dockerfile`, `docs/` |

**Verified end to end, running on localhost without Docker** (PostgreSQL 17.6 + PostGIS 3.6 native — see [docs/localhost-setup.md](docs/localhost-setup.md)):

* 29 unit tests pass; 216 source files compile on `release 21`
* Flyway applied all 8 migrations; Hibernate `validate` passed against the real schema
* Seeded 6 users, 4 vehicles, 4 deliveries, 6 incidents
* JWT login works; RBAC returns **403** for a driver on an admin endpoint, **401** anonymous, **200** admin
* PostGIS `ST_AsGeoJSON` and `ST_DWithin` return correct results; road lengths are computed by PostGIS
* AI engine: the same road scores **MEDIUM_RISK 41.2** at 20 mm of rain and **HIGH_RISK 75.7** at 180 mm, with a full score card
* OSRM returned a real 98.8 km Guwahati→Shillong route, scored **12.8 LOW_RISK**
* Incident chain: a CRITICAL landslide report auto-matched its road and district, flipped the road to **BLOCKED**, and raised two alerts
* Offline idempotency: the same `clientUuid` sent three times produced **one** incident; batch sync reports `accepted` then `duplicates`
* Frontend: dashboard with 12 cards and 4 charts, map with 29 GeoJSON paths and live vehicle markers, STOMP connected, alerts pushed to the browser with no refresh

---

## Documentation

| Document | Contents |
|---|---|
| [architecture.md](docs/architecture.md) | Layered architecture, package layout, AI design, routing design, security model, dependency justification |
| [database-design.md](docs/database-design.md) | ER diagram, 18 tables, 18 enums, indexes, PostGIS columns, seed plan |
| [api-documentation.md](docs/api-documentation.md) | ~95 REST endpoints, 10 WebSocket destinations |
| [development-roadmap.md](docs/development-roadmap.md) | Phase plan, timeline, risk register, future work |
| [setup-guide.md](docs/setup-guide.md) | Prerequisites, environment variables, troubleshooting |
| [localhost-setup.md](docs/localhost-setup.md) | Running without Docker on native PostgreSQL + PostGIS (verified) |
| [demo-script.md](docs/demo-script.md) | A twelve-minute walkthrough for a viva |

---

## Quick start

**1. Prerequisites** — JDK 21, Maven, Node 20+. Docker is optional; see [docs/localhost-setup.md](docs/localhost-setup.md) to run PostgreSQL natively instead.

```bash
winget install EclipseAdoptium.Temurin.21.JDK
```

```bash
winget install Apache.Maven
```

**2. Configuration** — two git-ignored `.env` files:

```bash
cp .env.example .env && cp backend/.env.example backend/.env
```

Set `DB_PASSWORD` (identical in both), `JWT_SECRET` and `SEED_DEMO_PASSWORD`:

```bash
openssl rand -base64 64
```

**3. Database** — the verified no-Docker path is in [docs/localhost-setup.md](docs/localhost-setup.md). With Docker:

```bash
docker compose up -d postgis
```

**4. Backend**

```bash
cd backend && ./run-dev.ps1
```

**5. Frontend**

```bash
cd frontend && npm install && npm run dev
```

* App: http://localhost:5173 (5174 if 5173 is taken — keep `FRONTEND_ORIGIN` in `backend/.env` in sync)
* Swagger UI: http://localhost:8080/swagger-ui.html
* Health: http://localhost:8080/actuator/health

### Everything in Docker

```bash
docker compose --profile full up --build
```

---

## Verify it yourself

```bash
cd backend && mvn -B test
```

```bash
cd frontend && npm run build
```

Then follow [docs/demo-script.md](docs/demo-script.md) — it exercises RBAC, the live map, the AI engine, the incident event chain, offline sync and routing in that order.

---

## Demo accounts (dev profile)

| Username | Role |
|---|---|
| admin | ADMIN |
| authority1 | AUTHORITY_OFFICIAL |
| manager1 | LOGISTICS_MANAGER |
| officer1 | FIELD_OFFICER |
| driver1, driver2 | DRIVER |

All created with the `SEED_DEMO_PASSWORD` from your `.env`. No password is ever committed.

---

## Repository layout

```
NER-SmartAccess/
├── backend/
│   ├── src/main/java/com/ner/smartlogix/
│   │   ├── analytics/     dashboard aggregation
│   │   ├── config/        security, JWT, OpenAPI, JPA auditing, HTTP client
│   │   ├── controller/    12 REST controllers + 1 STOMP controller
│   │   ├── dto/           request/, response/, geo/  (records, validated)
│   │   ├── entity/        19 JPA entities with PostGIS geometry
│   │   ├── enums/         18 domain enums
│   │   ├── event/         domain events + IncidentImpactListener
│   │   ├── exception/     typed exceptions + GlobalExceptionHandler
│   │   ├── integration/   weather providers, GPS simulator, delay monitor
│   │   ├── mapper/        entity → DTO
│   │   ├── ml/            RiskPredictor, feature extraction, 6 rules
│   │   ├── notification/  NotificationChannel + WebSocket/Email/SMS
│   │   ├── repository/    18 Spring Data repositories, PostGIS queries
│   │   ├── routing/       RoutingProvider (OSRM / internal Dijkstra / mock), scorer
│   │   ├── security/      JWT provider, filter, handlers
│   │   ├── service/       interfaces + impl/
│   │   ├── util/          GeometryUtils
│   │   └── websocket/     STOMP config, auth interceptor, broadcaster
│   ├── src/main/resources/db/migration/   V1–V8
│   ├── src/test/java/     risk engine + JWT tests
│   ├── Dockerfile, pom.xml, run-dev.ps1, .env.example
├── frontend/
│   ├── src/
│   │   ├── components/  Layout, NerMap, ProtectedRoute, OfflineBanner, StatCard
│   │   ├── context/     AuthContext
│   │   ├── hooks/       useWebSocket, useOfflineSync
│   │   ├── pages/       11 screens
│   │   ├── services/    axios client with silent refresh, endpoint wrappers
│   │   ├── types/       mirrors of the backend DTOs
│   │   └── utils/       IndexedDB offline store, formatting
│   ├── Dockerfile, nginx.conf, vite.config.ts (PWA)
├── docs/
├── docker-compose.yml
└── .env.example
```

---

## Security notes

* No password, JWT secret or API key is committed. Secrets are read from environment variables and have **no default**, so a missing secret stops the application at startup rather than falling back to something weak.
* Passwords are BCrypt hashes; refresh tokens are stored as SHA-256 hashes and rotated on every use.
* Uploaded photos are checked by content type **and** magic bytes, size-capped, and stored under generated names outside the web root.
* Role rules live next to the code they protect (`@PreAuthorize`), with ownership checks in the services — a driver can read only their own deliveries and push GPS only for their own vehicle.

## Future work

1. Replace `RuleBasedRiskPredictor` with a Tribuo RandomForest trained on the accumulated `road_risk_assessment` history — same `RiskPredictor` interface, no other change.
2. Real IMD weather and GSI landslide-zonation feeds in place of the mock provider.
3. SMS and email notification channels — the `NotificationChannel` interface and both stubs already exist.
4. Local OSRM instance built from a North-East India extract, replacing the public demo server.
5. React Native driver app reusing the same REST and STOMP contract.
