-- =============================================================
-- V7 : indexes
-- GIST indexes make the ST_* spatial queries fast; without them PostGIS
-- scans every row and compares geometries one by one.
-- =============================================================

-- Spatial
CREATE INDEX idx_district_boundary   ON district         USING GIST (boundary);
CREATE INDEX idx_district_centroid   ON district         USING GIST (centroid);
CREATE INDEX idx_road_geom           ON road             USING GIST (geom);
CREATE INDEX idx_bridge_location     ON bridge           USING GIST (location);
CREATE INDEX idx_route_geom          ON route            USING GIST (geom);
CREATE INDEX idx_incident_location   ON incident         USING GIST (location);
CREATE INDEX idx_vloc_geom           ON vehicle_location USING GIST (location);
CREATE INDEX idx_alert_location      ON alert            USING GIST (location);

-- Filtering used by the dashboard and the map legend
CREATE INDEX idx_road_status         ON road(status);
CREATE INDEX idx_road_risk           ON road(current_risk_level);
CREATE INDEX idx_road_district       ON road(district_id);
CREATE INDEX idx_bridge_road         ON bridge(road_id);
CREATE INDEX idx_delivery_status     ON delivery(status);
CREATE INDEX idx_delivery_vehicle    ON delivery(vehicle_id);
CREATE INDEX idx_incident_status     ON incident(status);

-- "Newest row for this parent" queries. The DESC part matters: it lets PostgreSQL
-- answer with a single index lookup instead of sorting.
CREATE INDEX idx_vloc_vehicle_time     ON vehicle_location(vehicle_id, recorded_at DESC);
CREATE INDEX idx_incident_road_time    ON incident(road_id, occurred_at DESC);
CREATE INDEX idx_weather_district_time ON weather_data(district_id, recorded_at DESC);
CREATE INDEX idx_rra_road_time         ON road_risk_assessment(road_id, assessed_at DESC);
CREATE INDEX idx_access_district_time  ON accessibility_status(district_id, evaluated_at DESC);
CREATE INDEX idx_alert_active_time     ON alert(active, created_at DESC);

-- Partial indexes: index only the rows that are actually queried.
CREATE INDEX idx_notif_user_unread ON notification(user_id) WHERE read_flag = FALSE;
CREATE INDEX idx_refresh_user      ON refresh_token(user_id) WHERE revoked = FALSE;
