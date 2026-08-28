package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.*;
import java.time.OffsetDateTime;

public record RoadResponse(
        Long id,
        String code,
        String name,
        RoadType roadType,
        String districtCode,
        String districtName,
        Double lengthKm,
        RoadStatus status,
        RoadCondition condition,
        RiskLevel currentRiskLevel,
        Double slopeDegrees,
        Double landslideSusceptibility,
        boolean floodProne,
        Double historicalBlockDaysPerYear,
        OffsetDateTime statusUpdatedAt) {
}
