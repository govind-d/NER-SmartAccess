package com.ner.smartlogix.mapper;

import com.ner.smartlogix.dto.response.BridgeResponse;
import com.ner.smartlogix.dto.response.RoadResponse;
import com.ner.smartlogix.entity.Bridge;
import com.ner.smartlogix.entity.Road;
import org.springframework.stereotype.Component;

@Component
public class RoadMapper {

    public RoadResponse toResponse(Road road) {
        return new RoadResponse(
                road.getId(),
                road.getCode(),
                road.getName(),
                road.getRoadType(),
                road.getDistrict() == null ? null : road.getDistrict().getCode(),
                road.getDistrict() == null ? null : road.getDistrict().getName(),
                road.getLengthKm(),
                road.getStatus(),
                road.getCondition(),
                road.getCurrentRiskLevel(),
                road.getSlopeDegrees(),
                road.getLandslideSusceptibility(),
                road.isFloodProne(),
                road.getHistoricalBlockDaysPerYear(),
                road.getStatusUpdatedAt());
    }

    public BridgeResponse toResponse(Bridge bridge) {
        return new BridgeResponse(
                bridge.getId(),
                bridge.getCode(),
                bridge.getName(),
                bridge.getRoad() == null ? null : bridge.getRoad().getCode(),
                bridge.getRoad() == null ? null : bridge.getRoad().getName(),
                bridge.getLocation() == null ? null : bridge.getLocation().getY(),
                bridge.getLocation() == null ? null : bridge.getLocation().getX(),
                bridge.getLoadCapacityTons(),
                bridge.getCondition(),
                bridge.getStatus(),
                bridge.getLastInspectionDate());
    }
}
