package com.ner.smartlogix.repository.projection;

public interface DistrictGeoRow {
    Long getId();
    String getCode();
    String getName();
    String getState();
    String getAccessibility();
    String getGeojson();
}
