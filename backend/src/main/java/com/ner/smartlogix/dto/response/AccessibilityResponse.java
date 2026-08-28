package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.AccessibilityLevel;
import java.time.OffsetDateTime;

public record AccessibilityResponse(
        String districtCode,
        String districtName,
        AccessibilityLevel accessibilityLevel,
        Integer openRoads,
        Integer blockedRoads,
        Integer highRiskRoads,
        String remarks,
        OffsetDateTime evaluatedAt) {
}
