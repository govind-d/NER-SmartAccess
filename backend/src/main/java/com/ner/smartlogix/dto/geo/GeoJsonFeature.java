package com.ner.smartlogix.dto.geo;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * One GeoJSON feature: a geometry plus arbitrary properties.
 *
 * <p>The geometry is a raw {@link JsonNode} because PostGIS already knows how to emit
 * GeoJSON ({@code ST_AsGeoJSON}). Re-encoding a LineString into Java objects and back
 * again would be slower and would introduce rounding differences.
 */
public record GeoJsonFeature(String type, JsonNode geometry, Map<String, Object> properties) {

    public static GeoJsonFeature of(JsonNode geometry, Map<String, Object> properties) {
        return new GeoJsonFeature("Feature", geometry, properties);
    }
}
