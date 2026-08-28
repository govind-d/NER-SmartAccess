-- =============================================================
-- V6 : events, risk and alerting
-- incident, weather_data, road_risk_assessment, accessibility_status,
-- alert, notification, field_report
-- =============================================================

CREATE TABLE incident (
    id            BIGSERIAL PRIMARY KEY,
    client_uuid   UUID UNIQUE,            -- generated on the device; makes offline
    incident_type VARCHAR(30)  NOT NULL,  -- re-sync idempotent
    severity      VARCHAR(15)  NOT NULL,
    description   TEXT,
    location      geometry(Point, 4326) NOT NULL,
    latitude      DOUBLE PRECISION NOT NULL,
    longitude     DOUBLE PRECISION NOT NULL,
    road_id       BIGINT REFERENCES road(id),
    district_id   BIGINT REFERENCES district(id),
    reported_by   BIGINT NOT NULL REFERENCES users(id),
    photo_path    VARCHAR(255),
    status        VARCHAR(20) NOT NULL DEFAULT 'REPORTED',
    occurred_at   TIMESTAMPTZ NOT NULL,   -- when it happened (possibly offline)
    verified_at   TIMESTAMPTZ,
    verified_by   BIGINT REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),  -- when the server received it
    updated_at    TIMESTAMPTZ,
    created_by    VARCHAR(50),
    updated_by    VARCHAR(50),
    CONSTRAINT chk_incident_type CHECK (incident_type IN
        ('LANDSLIDE','FLOOD','ROAD_DAMAGE','BRIDGE_DAMAGE','TRAFFIC_CONGESTION','ACCIDENT','OTHER')),
    CONSTRAINT chk_incident_sev CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_incident_status CHECK (status IN
        ('REPORTED','VERIFIED','IN_PROGRESS','RESOLVED','REJECTED'))
);

CREATE TABLE weather_data (
    id              BIGSERIAL PRIMARY KEY,
    district_id     BIGINT NOT NULL REFERENCES district(id),
    temperature_c   DOUBLE PRECISION,
    rainfall_mm_24h DOUBLE PRECISION NOT NULL DEFAULT 0,  -- flash-flood signal
    rainfall_mm_72h DOUBLE PRECISION NOT NULL DEFAULT 0,  -- slope-saturation signal
    humidity        DOUBLE PRECISION,
    wind_speed_kmph DOUBLE PRECISION,
    condition       VARCHAR(20) NOT NULL,
    source          VARCHAR(30) NOT NULL DEFAULT 'MOCK',  -- MOCK / OPENWEATHER / IMD
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE road_risk_assessment (
    id               BIGSERIAL PRIMARY KEY,
    road_id          BIGINT NOT NULL REFERENCES road(id) ON DELETE CASCADE,
    disruption_score DOUBLE PRECISION NOT NULL,
    risk_level       VARCHAR(20) NOT NULL,
    model_name       VARCHAR(40) NOT NULL,   -- rule-engine-v1 / tribuo-rf-v2
    score_card       JSONB,                  -- WHY the engine decided this
    assessed_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_rra_risk CHECK (risk_level IN ('LOW_RISK','MEDIUM_RISK','HIGH_RISK'))
);

CREATE TABLE accessibility_status (
    id                  BIGSERIAL PRIMARY KEY,
    district_id         BIGINT NOT NULL REFERENCES district(id),
    accessibility_level VARCHAR(25) NOT NULL,
    open_roads          INTEGER NOT NULL DEFAULT 0,
    blocked_roads       INTEGER NOT NULL DEFAULT 0,
    high_risk_roads     INTEGER NOT NULL DEFAULT 0,
    remarks             TEXT,
    evaluated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_access_level CHECK (accessibility_level IN
        ('FULLY_ACCESSIBLE','PARTIALLY_ACCESSIBLE','RESTRICTED','CUT_OFF'))
);

CREATE TABLE alert (
    id          BIGSERIAL PRIMARY KEY,
    alert_type  VARCHAR(30)  NOT NULL,
    severity    VARCHAR(15)  NOT NULL,
    title       VARCHAR(150) NOT NULL,
    message     TEXT         NOT NULL,
    district_id BIGINT REFERENCES district(id),
    road_id     BIGINT REFERENCES road(id),
    incident_id BIGINT REFERENCES incident(id),
    delivery_id BIGINT REFERENCES delivery(id),
    location    geometry(Point, 4326),
    active      BOOLEAN NOT NULL DEFAULT TRUE,   -- deactivated, never deleted
    expires_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

CREATE TABLE notification (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    alert_id  BIGINT REFERENCES alert(id) ON DELETE CASCADE,
    channel   VARCHAR(15) NOT NULL DEFAULT 'WEBSOCKET',
    status    VARCHAR(15) NOT NULL DEFAULT 'SENT',
    read_flag BOOLEAN     NOT NULL DEFAULT FALSE,
    sent_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_notif_channel CHECK (channel IN ('WEBSOCKET','EMAIL','SMS','PUSH')),
    CONSTRAINT chk_notif_status  CHECK (status  IN ('PENDING','SENT','FAILED'))
);

CREATE TABLE field_report (
    id          BIGSERIAL PRIMARY KEY,
    client_uuid UUID NOT NULL UNIQUE,
    reported_by BIGINT NOT NULL REFERENCES users(id),
    report_type VARCHAR(30) NOT NULL,
    description TEXT,
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    photo_path  VARCHAR(255),
    sync_status VARCHAR(15) NOT NULL DEFAULT 'SYNCED',
    captured_at TIMESTAMPTZ NOT NULL,   -- when the officer filled the form (offline)
    synced_at   TIMESTAMPTZ,            -- when the server received it
    incident_id BIGINT REFERENCES incident(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50),
    CONSTRAINT chk_fr_sync CHECK (sync_status IN ('PENDING','SYNCED','FAILED'))
);
