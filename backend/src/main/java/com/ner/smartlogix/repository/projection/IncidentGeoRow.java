package com.ner.smartlogix.repository.projection;

public interface IncidentGeoRow {
    Long getId();
    String getIncidentType();
    String getSeverity();
    String getStatus();
    String getDescription();
    String getOccurredAt();
    String getGeojson();
}
