package com.ner.smartlogix.repository.projection;

/**
 * A closed projection: Spring Data implements this interface at runtime from the aliases
 * of a native query, so only the listed columns are fetched - no entity, no geometry
 * parsing, no lazy proxies.
 *
 * <p>{@code geojson} arrives already encoded by PostGIS via {@code ST_AsGeoJSON}.
 */
public interface RoadGeoRow {
    Long getId();
    String getCode();
    String getName();
    String getStatus();
    String getRiskLevel();
    Double getLengthKm();
    String getGeojson();
}
