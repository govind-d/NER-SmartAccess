package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.AccessibilityLevel;

public record DistrictResponse(
        Long id,
        String code,
        String name,
        String state,
        Double centroidLatitude,
        Double centroidLongitude,
        Integer population,
        Double areaSqKm,
        AccessibilityLevel accessibilityLevel,
        Integer openRoads,
        Integer blockedRoads,
        Integer highRiskRoads) {
}
