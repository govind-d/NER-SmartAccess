-- =============================================================
-- V2 : geography  (district, road, bridge)
-- Created before the users table because a user belongs to a district.
-- SRID 4326 = plain latitude/longitude, the same system GPS and Leaflet use.
-- =============================================================

CREATE TABLE district (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    state       VARCHAR(60)  NOT NULL,
    boundary    geometry(MultiPolygon, 4326),
    centroid    geometry(Point, 4326),
    population  INTEGER,
    area_sq_km  DOUBLE PRECISION,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

CREATE TABLE road (
    id                             BIGSERIAL PRIMARY KEY,
    code                           VARCHAR(30)  NOT NULL UNIQUE,
    name                           VARCHAR(150) NOT NULL,
    road_type                      VARCHAR(30)  NOT NULL,
    district_id                    BIGINT       NOT NULL REFERENCES district(id),
    geom                           geometry(LineString, 4326) NOT NULL,
    length_km                      DOUBLE PRECISION NOT NULL,
    status                         VARCHAR(25)  NOT NULL DEFAULT 'OPEN',
    condition                      VARCHAR(20)  NOT NULL DEFAULT 'GOOD',
    current_risk_level             VARCHAR(20)  NOT NULL DEFAULT 'LOW_RISK',
    slope_degrees                  DOUBLE PRECISION DEFAULT 0,
    landslide_susceptibility       DOUBLE PRECISION DEFAULT 0,
    flood_prone                    BOOLEAN      NOT NULL DEFAULT FALSE,
    historical_block_days_per_year DOUBLE PRECISION DEFAULT 0,
    status_updated_at              TIMESTAMPTZ,
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                     TIMESTAMPTZ,
    created_by                     VARCHAR(50),
    updated_by                     VARCHAR(50),
    CONSTRAINT chk_road_length     CHECK (length_km > 0),
    CONSTRAINT chk_road_status     CHECK (status IN ('OPEN','PARTIALLY_ACCESSIBLE','HIGH_RISK','BLOCKED')),
    CONSTRAINT chk_road_condition  CHECK (condition IN ('GOOD','FAIR','POOR','DAMAGED')),
    CONSTRAINT chk_road_risk       CHECK (current_risk_level IN ('LOW_RISK','MEDIUM_RISK','HIGH_RISK')),
    CONSTRAINT chk_road_suscept    CHECK (landslide_susceptibility BETWEEN 0 AND 1)
);

CREATE TABLE bridge (
    id                   BIGSERIAL PRIMARY KEY,
    code                 VARCHAR(30)  NOT NULL UNIQUE,
    name                 VARCHAR(150) NOT NULL,
    road_id              BIGINT       NOT NULL REFERENCES road(id),
    location             geometry(Point, 4326) NOT NULL,
    load_capacity_tons   DOUBLE PRECISION,
    condition            VARCHAR(20)  NOT NULL DEFAULT 'GOOD',
    status               VARCHAR(25)  NOT NULL DEFAULT 'OPEN',
    last_inspection_date DATE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ,
    created_by           VARCHAR(50),
    updated_by           VARCHAR(50),
    CONSTRAINT chk_bridge_status CHECK (status IN ('OPEN','WEIGHT_RESTRICTED','CLOSED'))
);
