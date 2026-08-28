-- =============================================================
-- V1 : spatial support
-- PostGIS adds the geometry type and the ST_* functions. This is the one thing
-- Hibernate's ddl-auto can never do for us, which is why the project uses Flyway.
-- =============================================================
CREATE EXTENSION IF NOT EXISTS postgis;
