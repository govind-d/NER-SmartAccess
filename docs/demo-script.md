# NER-SmartLogix-AI — Demonstration Script

A twelve-minute walkthrough that exercises every module in a sensible order. Follow it
verbatim for a viva; each step is chosen so the next one has something to react to.

---

## 0. Before the audience arrives (5 minutes)

```bash
docker compose up -d postgis
```

```bash
cd backend && ./run-dev.ps1
```

```bash
cd frontend && npm run dev
```

Confirm: Flyway applied V1–V8, the seeder created six users, and the GPS simulator log
line appears. Leave the app running for a few minutes so vehicles have moved.

---

## 1. Security and roles (2 minutes)

1. Open http://localhost:5173 and sign in as **admin**.
2. Point out the sidebar: every page is visible.
3. Sign out, sign in as **driver1**. The sidebar now shows four items — no dashboard, no
   users, no fleet.
4. Type `/users` into the address bar. The page refuses with "Not permitted".
5. In Swagger (http://localhost:8080/swagger-ui.html), call `GET /api/v1/users` with the
   driver's token: **403** with the same JSON envelope the frontend uses.

**Point to make:** the frontend hides the page for convenience; `@PreAuthorize` on the
backend is what actually enforces it.

---

## 2. The live map (2 minutes)

1. Sign back in as **admin** and open **Live map**.
2. Roads are coloured by status — green open, orange high risk, red blocked. Click the
   Haflong–Maibang state highway: it is already BLOCKED in the seed data.
3. Toggle the district layer: the choropleth is driven by the accessibility level
   computed from those road statuses.
4. Watch a vehicle marker move. That is the GPS simulator publishing to
   `/topic/vehicles` over STOMP — no polling anywhere.

---

## 3. The AI risk engine (3 minutes) — the centrepiece

1. Open **Risk explorer** and select **NH-10 Rangpo to Gangtok** (slope 26°,
   landslide susceptibility 0.88).
2. Set 24 h rainfall to **20 mm**. Predict. Result: **LOW_RISK** or **MEDIUM_RISK**.
3. Drag 24 h rainfall to **180 mm** and 72 h to **350 mm**. Predict again:
   **HIGH_RISK**, and the bar chart shows RainfallRule and LandslideRule dominating.
4. Read out the score card entry: *"142 mm in 24 h and 310 mm over 72 h"*, *"susceptibility
   0.82 on a 27 degree slope"*.

**Points to make:**
- Every number came from Java — no Python anywhere in this project.
- The engine explains itself. A government system that says "HIGH_RISK" without saying
  why will not be acted on.
- `RiskPredictor` is an interface. A Tribuo model trained on the accumulating
  `road_risk_assessment` history drops in behind the same interface with no other change.

---

## 4. Incident reporting and the event chain (3 minutes) — the flagship flow

1. Open a second browser window, signed in as **officer1**, on **Report incident**.
   Keep the admin window on **Live map** so both are visible.
2. As the officer: type LANDSLIDE, severity **CRITICAL**, press *Use my location*, then
   edit the coordinates to sit on a monitored road (e.g. `25.24`, `93.08`). Submit.
3. Watch the admin window, without touching it:
   - a red toast appears in the corner;
   - a new incident pin drops on the map;
   - the affected road turns red.
4. Open the **Dashboard**: blocked roads and active incidents have both incremented.

**Point to make:** `IncidentServiceImpl` did one thing — save and publish an event. Four
listeners did the rest: closed the road, recomputed district accessibility, raised the
alert, and flagged the deliveries using that road as DELAYED.

---

## 5. Offline field reporting (2 minutes)

1. In the officer's window, open DevTools → Network → **Offline**.
2. Fill in another report and submit. The banner turns dark: *"You are offline. The report
   is saved on this device."* The pending list shows it.
3. Switch the network back to **Online**. Within a second the banner clears and the
   pending count drops to zero.
4. Press *Sync now* again: the server reports it as a **duplicate**, not a second
   landslide — the `clientUuid` idempotency key at work.

---

## 6. Risk-aware routing (2 minutes)

1. Open **Route planner**. Choose Kamrup Metropolitan → East Khasi Hills, goods
   MEDICINE, *Avoid high-risk roads* ticked. Find the best route.
2. The recommended route is drawn in blue, alternates dashed in grey.
3. Scroll to **How this was decided**: the scoring formula, and the rejected candidates
   with their reasons ("BLOCKED_ROAD — Passes Haflong to Maibang, which is BLOCKED").

**Point to make:** OSRM found the geometry; the platform's contribution is deciding which
of those routes a truck carrying medicine should take today.

---

## 7. Close (1 minute)

Open the **Dashboard** one last time and let the charts speak: deliveries per day,
incidents by type, road status distribution, disruptions by district. Then say what is
next: replacing the rule engine with a trained Tribuo model, real IMD and GSI data feeds,
and SMS notifications — all of which are already interfaces waiting for implementations.
