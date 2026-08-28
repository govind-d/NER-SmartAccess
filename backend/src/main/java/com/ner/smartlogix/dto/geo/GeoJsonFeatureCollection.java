package com.ner.smartlogix.dto.geo;

import java.util.List;

/**
 * A GeoJSON FeatureCollection - exactly what Leaflet's {@code L.geoJSON()} expects, so
 * the frontend can drop the response straight onto the map with no transformation.
 */
public record GeoJsonFeatureCollection(String type, List<GeoJsonFeature> features) {

    public static GeoJsonFeatureCollection of(List<GeoJsonFeature> features) {
        return new GeoJsonFeatureCollection("FeatureCollection", features);
    }
}
