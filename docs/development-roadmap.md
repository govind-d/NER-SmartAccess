# NER-SmartLogix-AI — Development Roadmap (Phase 1 output)

The build order below is chosen so that **every phase ends with something you can run and show**. Nothing is built that a later phase would throw away, and the risky/impressive parts (AI risk engine, live map) land early enough that you still have time to polish them before submission.

---

## Phase 0 — Environment (½ day) ✔ mostly ready on this machine

| Item | Status on your machine | Action |
|---|---|---|
| JDK | **Java 25 LTS installed** | Install **JDK 21 (Temurin)** and point `JAVA_HOME` at it — see the note below |
| Maven | not on PATH | none needed — we generate the **Maven Wrapper** (`mvnw`), so `./mvnw` just works |
| Node.js | **v24 installed** | fine for Vite + React |
| Docker Desktop | **installed** | used for PostGIS |
| Git | **installed**, repo initialised on `main` | fine |

> **Java version decision.** You have Java 25; Spring Boot 3.x is only *certified* up to Java 24, and Hibernate/ByteBuddy can refuse a newer class-file version at runtime. Recommendation: **build on JDK 21 with Spring Boot 3.5.x** (the safe, well-documented combination for a project you must defend in a viva). If you would rather stay on JDK 25, we use **Spring Boot 4.0.x** instead — say so and I will target that; the architecture is unchanged either way.

**Deliverable:** `docker compose up postgis` running, `psql` reachable, `SELECT postgis_version();` works.

---

## Phase 2 — Project Skeleton & Database Foundation (1–2 days)

1. `backend/pom.xml` with the justified dependency set + Maven Wrapper.
2. `NerSmartLogixApplication.java`, `application.yml` + `application-dev.yml`, `.env.example`.
3. All **enums** (they are pure vocabulary — writing them first makes every later class obvious).
4. All **JPA entities** + `BaseAuditEntity`, with PostGIS/JTS types via `hibernate-spatial`.
5. **Flyway migrations V1–V7** (schema) and **V8–V9** (seed).
6. All **repositories** (plain Spring Data first; native `ST_*` queries added when needed).
7. `docker-compose.yml`, `.gitignore`, root `README.md`.

**Deliverable:** app starts, Flyway creates 18 tables + PostGIS indexes, Hibernate `validate` passes, seed data visible in pgAdmin.
**Why first:** entities and enums are the vocabulary every other layer speaks. Getting them right once avoids three refactors later.

---

## Phase 3 — Security: Auth, JWT, Roles (2 days)

`SecurityConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `AuthController`, refresh-token rotation, `GlobalExceptionHandler`, `ApiResponse<T>` envelope, springdoc/Swagger with a JWT security scheme.

**Deliverable:** register/login/refresh/logout working in Swagger; a `DRIVER` token gets **403** on an admin endpoint. Screenshot-able proof of RBAC.
**Why second:** every later controller needs `@PreAuthorize`; retrofitting security to 90 endpoints is misery.

---

## Phase 4 — Core Domain CRUD + GIS Read APIs (3 days)

Districts, Roads, Bridges, Vehicles, Deliveries: DTOs (with validation), MapStruct mappers, service interfaces + impls, controllers, paging/filtering, and the **GeoJSON endpoints** (`/roads/geojson`, `/districts/geojson`) plus the first PostGIS query (`/roads/near`).

**Deliverable:** ~45 working endpoints; a GeoJSON response you can paste into geojson.io and see NER roads drawn.
**Why here:** the AI and routing modules read these tables; they must exist and be populated first.

---

## Phase 5 — AI Risk Engine (3 days) ⭐ the academic core

`RiskFeatureVector`, `FeatureExtractor`, `RiskRule` interface + 6 concrete rules (Rainfall, Landslide, Flood, IncidentHistory, RoadCondition, Terrain), `RuleBasedRiskPredictor`, `ScoreCard`, weights in `application.yml`, `RiskService`, `/risk/**` endpoints, `road_risk_assessment` persistence, scheduled re-scoring job, and **unit tests per rule** (this is where a project earns marks).

**Deliverable:** `POST /risk/predict` returns HIGH_RISK with a human-readable score card. Roads change colour in the DB as rainfall rises.

---

## Phase 6 — Incidents, Alerts, Notifications, WebSocket (3 days)

Incident CRUD + photo upload, `IncidentReportedEvent` + listeners (road impact → risk recompute → alert), `AlertService`, `NotificationService` with a `NotificationChannel` interface (WebSocket now; Email/SMS stubs registered but disabled), `WebSocketConfig` + STOMP JWT interceptor, broadcasters.

**Deliverable:** report an incident in Swagger, watch a JSON alert arrive live in a STOMP test client. This is the demo moment.

---

## Phase 7 — Routing & Optimization (3 days)

`RoutingProvider` interface, `OsrmRoutingClient` (RestClient, timeouts, mock fallback), matching route geometry to our roads via `ST_DWithin`, `RouteRiskScorer` with configurable weights, hard-filtering of BLOCKED roads, alternate-route ranking, ETA + predicted delay, `DijkstraRouteFinder` (JGraphT) as the offline fallback, `/routes/**` endpoints.

**Deliverable:** `POST /routes/recommend` returns a best route plus alternates, with a rejection reason for the blocked one.

---

## Phase 8 — GPS Tracking + Simulator + Delivery Lifecycle (2 days)

Location ingest (REST + STOMP), `vehicle_location` history, geofence check against high-risk zones, `GpsSimulator` (`@Scheduled`, moves N vehicles along real route polylines, dev profile only), delivery state machine, ETA recalculation, delay detection job.

**Deliverable:** 10 vehicles crawling across the map in real time with no manual input.

---

## Phase 9 — Frontend Part 1: Shell, Auth, Map (4 days)

Vite + React + TS + Tailwind, `AuthContext` + protected routes + role-based menus, Axios client with token refresh interceptor, `useWebSocket` hook (STOMP over SockJS), the **Leaflet map page**: OSM base layer, road layer coloured by status, district choropleth by accessibility, incident pins, live vehicle markers, layer toggles, popups.

**Deliverable:** log in as each role and see a live NER map.

---

## Phase 10 — Frontend Part 2: Dashboard, Modules, Offline PWA (4 days)

Analytics dashboard (summary cards + Recharts line/bar/pie), Vehicles, Deliveries + tracking page, Incidents list + report form, Route planner UI (click two points → recommended vs alternates drawn), Alerts drawer with live toasts, Admin user management, and the **offline module**: `vite-plugin-pwa` service worker, IndexedDB queue (Dexie), background sync to `/field-reports/sync`, online/offline banner, pending-count badge.

**Deliverable:** turn off Wi-Fi, file an incident, turn it back on, watch it sync and appear on the dashboard.

---

## Phase 11 — Testing, Docs, Polish, Submission (3 days)

JUnit 5 + Mockito unit tests (risk rules, route scorer, delivery state machine, JWT), `@SpringBootTest` + Testcontainers PostGIS integration tests for the incident→alert flow, `docs/setup-guide.md` finalised, `docs/api-documentation.md` regenerated from Swagger, README with screenshots and architecture diagrams, `docker compose up` full-stack verification, seed reset script, demo walkthrough script for the viva.

---

## Timeline

| Track | Duration |
|---|---|
| Backend (Phases 2–8) | ~17 working days |
| Frontend (Phases 9–10) | ~8 working days |
| Testing & docs (Phase 11) | ~3 working days |
| **Total** | **~28 working days (≈6 weeks part-time)** |

Minimum viable submission if time runs short: Phases 2, 3, 4, 5, 6, 9 — that still demonstrates auth+RBAC, GIS, the AI engine, real-time alerts and a live map. Phases 7, 8, 10 are the differentiators; drop Phase 10's PWA last, since it is the easiest to explain as "designed, partially implemented".

---

## Risk Register

| Risk | Likelihood | Mitigation |
|---|---|---|
| PostGIS + Hibernate spatial setup fights back | Medium | Phase 2 does nothing else; Testcontainers proves it early |
| No real NER road geometry available | High | Seed simplified LineStrings from OSM exports; schema unchanged when real data arrives |
| Public OSRM rate-limits or is down | Medium | `MockRoutingProvider` + local OSRM compose profile + JGraphT fallback |
| Weather API key/quota issues | Medium | `WEATHER_PROVIDER=mock` is the default in dev |
| Scope creep (SMS, ML, multi-tenant) | High | Interfaces are in place; implementations are explicitly deferred and documented as future work |
| Java 25 vs Spring Boot 3.x mismatch | Medium | Decided in Phase 0 (JDK 21 + Boot 3.5.x recommended) |

---

## Future Work (state this in your report — examiners like a credible roadmap)

1. Replace `RuleBasedRiskPredictor` with `TribuoRiskPredictor` (RandomForest) trained on accumulated `road_risk_assessment` + `incident` history; keep both behind the same interface and compare accuracy.
2. Real IMD/GSI data ingestion instead of mock providers.
3. SMS (Twilio/MSG91) and email notification channels — the `NotificationChannel` interface already exists.
4. Multi-vehicle load assignment / fleet optimisation.
5. Mobile app (React Native) reusing the same REST + STOMP contract.
