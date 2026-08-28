-- =============================================================
-- V5 : fleet  (vehicle, vehicle_location, delivery)
-- =============================================================

CREATE TABLE vehicle (
    id             BIGSERIAL PRIMARY KEY,
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type   VARCHAR(20) NOT NULL,
    capacity_tons  DOUBLE PRECISION,
    driver_id      BIGINT UNIQUE REFERENCES users(id),  -- one driver, one vehicle
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    last_latitude  DOUBLE PRECISION,   -- cached copy of the newest GPS ping so the
    last_longitude DOUBLE PRECISION,   -- live map needs only one cheap query
    last_seen_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ,
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50),
    CONSTRAINT chk_vehicle_type CHECK (vehicle_type IN
        ('TRUCK','MINI_TRUCK','TANKER','AMBULANCE','RELIEF_VAN'))
);

-- The only high-volume table: roughly one row per vehicle every ten seconds.
CREATE TABLE vehicle_location (
    id          BIGSERIAL PRIMARY KEY,
    vehicle_id  BIGINT NOT NULL REFERENCES vehicle(id) ON DELETE CASCADE,
    location    geometry(Point, 4326) NOT NULL,
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    speed_kmph  DOUBLE PRECISION,
    heading     DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_lat CHECK (latitude  BETWEEN -90  AND 90),
    CONSTRAINT chk_lon CHECK (longitude BETWEEN -180 AND 180)
);

CREATE TABLE delivery (
    id                      BIGSERIAL PRIMARY KEY,
    tracking_code           VARCHAR(24) NOT NULL UNIQUE,
    vehicle_id              BIGINT REFERENCES vehicle(id),
    route_id                BIGINT REFERENCES route(id),
    source_district_id      BIGINT NOT NULL REFERENCES district(id),
    destination_district_id BIGINT NOT NULL REFERENCES district(id),
    source_label            VARCHAR(150),
    destination_label       VARCHAR(150),
    goods_type              VARCHAR(30) NOT NULL,
    weight_tons             DOUBLE PRECISION,
    status                  VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    dispatched_at           TIMESTAMPTZ,
    eta                     TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    delay_minutes           INTEGER NOT NULL DEFAULT 0,
    created_by_user_id      BIGINT REFERENCES users(id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(50),
    updated_by              VARCHAR(50),
    CONSTRAINT chk_delivery_status CHECK (status IN ('CREATED','IN_TRANSIT','DELAYED','DELIVERED')),
    CONSTRAINT chk_delivery_route  CHECK (source_district_id <> destination_district_id)
);
