package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.BridgeStatus;
import com.ner.smartlogix.enums.RoadCondition;
import java.time.LocalDate;

public record BridgeResponse(
        Long id,
        String code,
        String name,
        String roadCode,
        String roadName,
        Double latitude,
        Double longitude,
        Double loadCapacityTons,
        RoadCondition condition,
        BridgeStatus status,
        LocalDate lastInspectionDate) {
}
