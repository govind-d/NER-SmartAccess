package com.ner.smartlogix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ner.smartlogix.dto.geo.GeoJsonFeature;
import com.ner.smartlogix.dto.geo.GeoJsonFeatureCollection;
import com.ner.smartlogix.repository.DistrictRepository;
import com.ner.smartlogix.repository.IncidentRepository;
import com.ner.smartlogix.repository.RoadRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns database rows into map layers.
 *
 * <p>No interface here on purpose: this class has exactly one implementation and no
 * external boundary to abstract. An interface with a single implementor that nobody
 * substitutes is ceremony, not design - the interfaces in this project exist where
 * something really is swappable (risk predictor, weather provider, routing provider).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoJsonService {

    private final RoadRepository roadRepository;
    private final DistrictRepository districtRepository;
    private final IncidentRepository incidentRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public GeoJsonFeatureCollection roads(String status) {
        List<GeoJsonFeature> features = new ArrayList<>();
        roadRepository.findGeoJson(status).forEach(row -> {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("id", row.getId());
            properties.put("code", row.getCode());
            properties.put("name", row.getName());
            properties.put("status", row.getStatus());
            properties.put("riskLevel", row.getRiskLevel());
            properties.put("lengthKm", row.getLengthKm());
            addFeature(features, row.getGeojson(), properties);
        });
        return GeoJsonFeatureCollection.of(features);
    }

    @Transactional(readOnly = true)
    public GeoJsonFeatureCollection districts() {
        List<GeoJsonFeature> features = new ArrayList<>();
        districtRepository.findGeoJson().forEach(row -> {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("id", row.getId());
            properties.put("code", row.getCode());
            properties.put("name", row.getName());
            properties.put("state", row.getState());
            properties.put("accessibility", row.getAccessibility());
            addFeature(features, row.getGeojson(), properties);
        });
        return GeoJsonFeatureCollection.of(features);
    }

    @Transactional(readOnly = true)
    public GeoJsonFeatureCollection incidents(boolean activeOnly) {
        List<GeoJsonFeature> features = new ArrayList<>();
        incidentRepository.findGeoJson(activeOnly).forEach(row -> {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("id", row.getId());
            properties.put("incidentType", row.getIncidentType());
            properties.put("severity", row.getSeverity());
            properties.put("status", row.getStatus());
            properties.put("description", row.getDescription());
            properties.put("occurredAt", row.getOccurredAt());
            addFeature(features, row.getGeojson(), properties);
        });
        return GeoJsonFeatureCollection.of(features);
    }

    /** A row with unreadable geometry is skipped rather than failing the whole layer. */
    private void addFeature(List<GeoJsonFeature> features, String geometryJson,
                            Map<String, Object> properties) {
        if (geometryJson == null) {
            return;
        }
        try {
            JsonNode geometry = objectMapper.readTree(geometryJson);
            features.add(GeoJsonFeature.of(geometry, properties));
        } catch (JsonProcessingException ex) {
            log.warn("Skipping feature {} with unreadable geometry: {}",
                    properties.get("id"), ex.getMessage());
        }
    }
}
