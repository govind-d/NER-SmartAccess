-- =============================================================
-- V4 : routing  (route, route_segment)
-- Created before delivery, because a delivery points at the route it follows.
-- =============================================================

CREATE TABLE route (
    id                     BIGSERIAL PRIMARY KEY,
    name                   VARCHAR(150),
    geom                   geometry(LineString, 4326),
    total_distance_km      DOUBLE PRECISION NOT NULL,
    estimated_duration_min INTEGER          NOT NULL,
    risk_score             DOUBLE PRECISION NOT NULL DEFAULT 0,
    risk_level             VARCHAR(20)      NOT NULL DEFAULT 'LOW_RISK',
    provider               VARCHAR(20)      NOT NULL DEFAULT 'OSRM',
    is_alternate           BOOLEAN          NOT NULL DEFAULT FALSE,
    parent_route_id        BIGINT REFERENCES route(id),
    computed_at            TIMESTAMPTZ      NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ,
    created_by             VARCHAR(50),
    updated_by             VARCHAR(50),
    CONSTRAINT chk_route_risk CHECK (risk_level IN ('LOW_RISK','MEDIUM_RISK','HIGH_RISK'))
);

CREATE TABLE route_segment (
    id                 BIGSERIAL PRIMARY KEY,
    route_id           BIGINT NOT NULL REFERENCES route(id) ON DELETE CASCADE,
    road_id            BIGINT REFERENCES road(id),   -- nullable: part of a route may
    sequence_no        INTEGER NOT NULL,             -- run over roads we do not monitor
    distance_km        DOUBLE PRECISION NOT NULL,
    duration_min       INTEGER NOT NULL,
    segment_risk_score DOUBLE PRECISION DEFAULT 0,
    CONSTRAINT uq_route_segment_seq UNIQUE (route_id, sequence_no)
);
